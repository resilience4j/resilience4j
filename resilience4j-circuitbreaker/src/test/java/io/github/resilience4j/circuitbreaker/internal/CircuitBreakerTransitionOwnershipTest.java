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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Generic lifecycle regressions, runnable against upstream without gradual recovery. */
class CircuitBreakerTransitionOwnershipTest {

    @Test
    void competingCallerCanCompleteAnOpenHandoffWhileAnotherConstructorIsPaused() throws Exception {
        Clock clock = mock(Clock.class);
        Instant openedAt = Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(openedAt, openedAt.plusSeconds(1));
        CircuitBreakerConfig config = spy(CircuitBreakerConfig.custom().clock(clock)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .permittedNumberOfCallsInHalfOpenState(2).build());
        CountDownLatch constructingHalfOpen = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicReference<Thread> firstCaller = new AtomicReference<>();
        doAnswer(invocation -> {
            if (Thread.currentThread() == firstCaller.get()) {
                constructingHalfOpen.countDown();
                if (!resume.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("No release");
                }
            }
            return invocation.callRealMethod();
        }).when(config).getSlidingWindowSynchronizationStrategy();
        CircuitBreaker breaker = CircuitBreaker.of("open-handoff", config);
        breaker.transitionToOpenState();
        AtomicInteger transitions = new AtomicInteger();
        breaker.getEventPublisher().onStateTransition(event -> transitions.incrementAndGet());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> firstPermission = executor.submit(() -> {
                firstCaller.set(Thread.currentThread());
                return breaker.tryAcquirePermission();
            });

            assertThat(constructingHalfOpen.await(10, TimeUnit.SECONDS)).isTrue();

            assertThat(breaker.tryAcquirePermission()).isTrue();

            resume.countDown();

            assertThat(firstPermission.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(breaker.getState()).isEqualTo(HALF_OPEN);
            assertThat(transitions.get()).isEqualTo(1);
            assertThat(breaker.tryAcquirePermission()).isFalse();
        } finally {
            resume.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void resetRetiresBothLegacyOpenAndHalfOpenTimers() {
        CapturingScheduler scheduler = new CapturingScheduler();
        CircuitBreaker breaker = new CircuitBreakerStateMachine("legacy-reset",
            CircuitBreakerConfig.custom().automaticTransitionFromOpenToHalfOpenEnabled(true)
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(30)).build(), scheduler.factory);
        breaker.transitionToOpenState();
        Runnable oldOpenTimer = scheduler.tasks.get(0);
        breaker.transitionToHalfOpenState();
        Runnable oldHalfOpenTimer = scheduler.tasks.get(1);

        breaker.reset();
        oldOpenTimer.run();
        oldHalfOpenTimer.run();

        assertThat(breaker.getState()).isEqualTo(CLOSED);
        assertThat(scheduler.tasks).hasSize(2);
        verify(scheduler.futures.get(0)).cancel(false);
        verify(scheduler.futures.get(1)).cancel(true);
    }

    @Test
    void timersAreScheduledOnlyAfterTheirOwnerIsInstalled() {
        CapturingScheduler scheduler = new CapturingScheduler();
        AtomicReference<CircuitBreaker> breakerReference = new AtomicReference<>();
        scheduler.onSchedule = () -> assertThat(breakerReference.get().getState()).isIn(OPEN, HALF_OPEN);
        CircuitBreaker breaker = new CircuitBreakerStateMachine("publication",
            CircuitBreakerConfig.custom().automaticTransitionFromOpenToHalfOpenEnabled(true)
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(30)).build(), scheduler.factory);
        breakerReference.set(breaker);

        breaker.transitionToOpenState();
        scheduler.onSchedule = () -> assertThat(breaker.getState()).isEqualTo(HALF_OPEN);
        breaker.transitionToHalfOpenState();

        assertThat(scheduler.tasks).hasSize(2);
        assertThat(breaker.getState()).isEqualTo(HALF_OPEN);
    }

    @Test
    void aDeadlinePausedBeforePublicationCannotOverrideResetOrScheduleAnOrphan() throws Exception {
        CapturingScheduler scheduler = new CapturingScheduler();
        CountDownLatch constructingOpen = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean pause = new AtomicBoolean();
        CircuitBreaker breaker = new CircuitBreakerStateMachine("stale-deadline",
            CircuitBreakerConfig.custom().automaticTransitionFromOpenToHalfOpenEnabled(true)
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(30))
                .waitIntervalFunctionInOpenState(attempt -> {
                    if (pause.get()) {
                        constructingOpen.countDown();
                        try {
                            if (!resume.await(10, TimeUnit.SECONDS)) {
                                throw new AssertionError("No release");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                    }
                    return 60_000L;
                }).build(), scheduler.factory);
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
        Runnable halfOpenDeadline = scheduler.tasks.get(1);
        pause.set(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> callback = executor.submit(halfOpenDeadline);

            assertThat(constructingOpen.await(10, TimeUnit.SECONDS)).isTrue();
            breaker.reset();
            resume.countDown();
            callback.get(10, TimeUnit.SECONDS);

            assertThat(breaker.getState()).isEqualTo(CLOSED);
            assertThat(scheduler.tasks).hasSize(2);
        } finally {
            resume.countDown();
            executor.shutdownNow();
        }
    }

    private static final class CapturingScheduler {
        private final SchedulerFactory factory = mock(SchedulerFactory.class);
        private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        private final List<Runnable> tasks = new ArrayList<>();
        private final List<ScheduledFuture<?>> futures = new ArrayList<>();
        private Runnable onSchedule = () -> {
        };

        private CapturingScheduler() {
            when(factory.getScheduler()).thenReturn(executor);
            when(executor.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenAnswer(invocation -> {
                    onSchedule.run();
                    tasks.add(invocation.getArgument(0));
                    ScheduledFuture<?> future = mock(ScheduledFuture.class);
                    futures.add(future);
                    return future;
                });
        }
    }
}
