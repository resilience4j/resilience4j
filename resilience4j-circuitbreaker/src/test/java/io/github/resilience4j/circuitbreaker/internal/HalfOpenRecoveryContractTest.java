/*
 *
 *  Copyright 2026 authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.circuitbreaker.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Contract regressions for compatible completion attribution and health-driven recovery. */
class HalfOpenRecoveryContractTest {

    private static final Duration DWELL = Duration.ofSeconds(10);
    private static final int SAMPLE_SIZE = 10;

    @Test
    void lateSeventyFivePercentCompletionsCanCloseHundredPercentWithItsCallsStillPending() {
        Fixture fixture = new Fixture(config -> { });
        fixture.completeHealthyPhase();
        fixture.completeHealthyPhase();
        fixture.assertTarget(75);

        List<PendingCall> lateSeventyFivePercentCalls = fixture.admit(SAMPLE_SIZE);
        List<PendingCall> healthySeventyFivePercentCalls = fixture.admit(SAMPLE_SIZE);
        fixture.clock.advance(DWELL);
        healthySeventyFivePercentCalls.forEach(PendingCall::succeed);
        fixture.assertTarget(100);

        List<PendingCall> hundredPercentCalls = fixture.admit(SAMPLE_SIZE);
        fixture.clock.advance(DWELL);
        lateSeventyFivePercentCalls.forEach(PendingCall::succeed);

        assertThat(hundredPercentCalls).allSatisfy(call ->
            assertThat(call.decorated.isDone()).isFalse());
        assertThat(fixture.breaker.getState())
            .as("Compatible completion-time attribution accepts successes admitted in an older phase")
            .isEqualTo(CLOSED);
        assertThat(fixture.breaker.getHalfOpenPhase().isActive()).isFalse();

        hundredPercentCalls.forEach(call -> call.backend.completeExceptionally(
            new IllegalStateException("Backend rejects load admitted at 100%")));
        assertThat(hundredPercentCalls).allSatisfy(call ->
            assertThat(call.decorated.isCompletedExceptionally()).isTrue());
        assertThat(fixture.breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(SAMPLE_SIZE);
        assertThat(fixture.breaker.getState()).isEqualTo(CLOSED);
    }

    @Test
    void idleTimeAndAnAdmissionCannotPromoteUsingOnlyOldHealthSamples() {
        Fixture fixture = new Fixture(config -> { });
        fixture.admit(SAMPLE_SIZE).forEach(PendingCall::succeed);
        fixture.assertTarget(25);
        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(SAMPLE_SIZE);

        fixture.clock.advance(Duration.ofHours(1));
        PendingCall newCall = fixture.admitOne();

        assertThat(newCall.decorated.isDone()).isFalse();
        assertThat(fixture.breaker.getHalfOpenPhase().getPermittedTrafficPercentage())
            .as("An offered call has no fresh outcome with which to validate the idle phase")
            .isEqualTo(25);

        newCall.succeed();

        fixture.assertTarget(50);
    }

    @Test
    void gradualRecoveryRejectsPositiveClosedEpisodeTimeouts() {
        for (Duration timeout : new Duration[]{Duration.ofNanos(1), Duration.ofSeconds(5)}) {
            assertThatThrownBy(() -> new Fixture(config -> config
                .maxWaitDurationInHalfOpenState(timeout)
                .transitionToStateAfterWaitDuration(CLOSED)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transitionToStateAfterWaitDuration=OPEN")
                .hasMessageContaining("maxWaitDurationInHalfOpenState");
        }
    }

    @Test
    void standardHalfOpenStillAllowsAPositiveClosedTimeout() {
        Fixture fixture = new Fixture(config -> config.halfOpenRecoveryConfig(null)
            .maxWaitDurationInHalfOpenState(Duration.ofSeconds(5))
            .transitionToStateAfterWaitDuration(CLOSED));
        assertThat(fixture.breaker.getState()).isEqualTo(HALF_OPEN);
        assertThat(fixture.breaker.getHalfOpenPhase().isActive()).isFalse();
        assertThat(fixture.scheduler.tasks).hasSize(1);
        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();

        fixture.clock.advance(Duration.ofSeconds(5));
        fixture.scheduler.tasks.get(0).run();

        assertThat(fixture.breaker.getState()).isEqualTo(CLOSED);
    }

    @Test
    void gradualRecoveryAllowsAClosedTargetWhenTheEpisodeTimeoutIsDisabled() {
        Fixture fixture = new Fixture(config -> config
            .maxWaitDurationInHalfOpenState(Duration.ZERO)
            .transitionToStateAfterWaitDuration(CLOSED));
        fixture.assertTarget(25);
        assertThat(fixture.scheduler.tasks).isEmpty();

        fixture.clock.advance(Duration.ofHours(1));
        fixture.assertTarget(25);
        fixture.admit(SAMPLE_SIZE).forEach(PendingCall::succeed);

        fixture.assertTarget(50);
    }

    private record PendingCall(CompletableFuture<String> backend,
        CompletableFuture<String> decorated) {

        void succeed() {
            assertThat(backend.complete("success")).isTrue();
            assertThat(decorated.join()).isEqualTo("success");
        }
    }

    private static final class Fixture {

        private final MutableClock clock = new MutableClock();
        private final CapturingScheduler scheduler = new CapturingScheduler();
        private final CircuitBreaker breaker;

        private Fixture(Consumer<CircuitBreakerConfig.Builder> customize) {
            CircuitBreakerConfig.Builder config = CircuitBreakerConfig.custom()
                .clock(clock)
                .currentTimestampFunction(current -> current.instant().toEpochMilli(),
                    TimeUnit.MILLISECONDS)
                .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom()
                    .trafficPercentageStep(25)
                    .minimumPhaseDuration(DWELL)
                    .minimumNumberOfCalls(SAMPLE_SIZE)
                    .build());
            customize.accept(config);
            breaker = new CircuitBreakerStateMachine("recovery-contract", config.build(),
                scheduler.factory);
            breaker.transitionToOpenState();
            breaker.transitionToHalfOpenState();
        }

        private PendingCall admitOne() {
            for (int attempt = 0; attempt < 100; attempt++) {
                CompletableFuture<String> backend = new CompletableFuture<>();
                CompletableFuture<String> decorated = breaker
                    .executeCompletionStage(() -> backend).toCompletableFuture();
                if (!decorated.isDone()) {
                    return new PendingCall(backend, decorated);
                }
            }
            throw new AssertionError("A positive recovery target must admit within 100 attempts");
        }

        private List<PendingCall> admit(int numberOfCalls) {
            List<PendingCall> calls = new ArrayList<>();
            for (int index = 0; index < numberOfCalls; index++) {
                calls.add(admitOne());
            }
            return calls;
        }

        private void completeHealthyPhase() {
            List<PendingCall> calls = admit(SAMPLE_SIZE);
            clock.advance(DWELL);
            calls.forEach(PendingCall::succeed);
        }

        private void assertTarget(int percentage) {
            assertThat(breaker.getState()).isEqualTo(HALF_OPEN);
            assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage())
                .isEqualTo(percentage);
        }
    }

    private static final class CapturingScheduler {

        private final SchedulerFactory factory = mock(SchedulerFactory.class);
        private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        private final List<Runnable> tasks = new ArrayList<>();

        private CapturingScheduler() {
            when(factory.getScheduler()).thenReturn(executor);
            when(executor.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenAnswer(invocation -> {
                    tasks.add(invocation.getArgument(0));
                    return mock(ScheduledFuture.class);
                });
        }
    }

    private static final class MutableClock extends Clock {

        private final AtomicReference<Instant> now;
        private final ZoneId zone;

        private MutableClock() {
            this(new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z")), ZoneOffset.UTC);
        }

        private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
            this.now = now;
            this.zone = zone;
        }

        private void advance(Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(now, newZone);
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
