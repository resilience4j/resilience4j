/*
 * Copyright 2026 Caio Vinícius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine;
import io.github.resilience4j.circuitbreaker.internal.SchedulerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers issue #1681: guarded publishers combined with an all-or-nothing operator such as
 * {@link Mono#zip} in the HALF_OPEN state.
 * <p>
 * When one member of the zip fails, the zip cancels its siblings. A cancelled call that has not
 * emitted yet releases its permission instead of being recorded. In this setup the two siblings
 * never emit, and after the first attempt fewer permissions are left than the zip needs before
 * the effective minimum number of calls is recorded. So on every new attempt the failing member
 * is the one rejected with {@link CallNotPermittedException}, the zip cancels the siblings that
 * did get a permission, and no call is recorded again. Without
 * {@code maxWaitDurationInHalfOpenState} the CircuitBreaker would stay HALF_OPEN.
 */
class CircuitBreakerOperatorHalfOpenZipTest {

    private static final int PERMITTED_CALLS_IN_HALF_OPEN = 3;

    private static CircuitBreakerConfig.Builder halfOpenConfig() {
        return CircuitBreakerConfig.custom()
            .slidingWindow(10, 10, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN)
            .failureRateThreshold(50f);
    }

    private static CircuitBreaker halfOpen(CircuitBreakerConfig config) {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("zip", config);
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        return circuitBreaker;
    }

    /**
     * Two siblings that never emit and one member that fails. The siblings are subscribed first,
     * so they hold a permission when the failure cancels them. No timing is involved.
     */
    private static Mono<?> zipWithOneFailure(CircuitBreaker circuitBreaker) {
        return Mono.zip(
            Mono.never().transformDeferred(CircuitBreakerOperator.of(circuitBreaker)),
            Mono.never().transformDeferred(CircuitBreakerOperator.of(circuitBreaker)),
            Mono.error(new IOException("BAM!")).transformDeferred(CircuitBreakerOperator.of(circuitBreaker)));
    }

    @Test
    void cancelledZipMembersReleaseTheirPermissionInHalfOpen() {
        CircuitBreaker circuitBreaker = halfOpen(halfOpenConfig().build());

        StepVerifier.create(zipWithOneFailure(circuitBreaker))
            .expectError(IOException.class)
            .verify(Duration.ofSeconds(1));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        // Only the failed member consumed its permission; both cancelled siblings returned theirs
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();
    }

    @Test
    void zipNeedingMorePermissionsThanLeftRecordsNoFurtherCalls() {
        CircuitBreaker circuitBreaker = halfOpen(halfOpenConfig().build());
        StepVerifier.create(zipWithOneFailure(circuitBreaker))
            .expectError(IOException.class)
            .verify(Duration.ofSeconds(1));

        for (int attempt = 0; attempt < 10; attempt++) {
            StepVerifier.create(zipWithOneFailure(circuitBreaker))
                .expectError(CallNotPermittedException.class)
                .verify(Duration.ofSeconds(1));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(10);
    }

    @Test
    void guardingTheCombinedPublisherRecordsEachAttemptAndLeavesHalfOpen() {
        CircuitBreaker circuitBreaker = halfOpen(halfOpenConfig().build());

        for (int attempt = 0; attempt < PERMITTED_CALLS_IN_HALF_OPEN; attempt++) {
            StepVerifier.create(
                Mono.zip(Mono.never(), Mono.never(), Mono.error(new IOException("BAM!")))
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)))
                .expectError(IOException.class)
                .verify(Duration.ofSeconds(1));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void maxWaitDurationInHalfOpenStateLeavesHalfOpenWhenZipCannotProgress() {
        // The HALF_OPEN timer is captured instead of running on a real scheduler, so it cannot
        // fire before the stall is set up
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SchedulerFactory schedulerFactory = mock(SchedulerFactory.class);
        when(schedulerFactory.getScheduler()).thenReturn(scheduler);
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("zip", halfOpenConfig()
            .maxWaitDurationInHalfOpenState(Duration.ofMillis(200))
            .build(), schedulerFactory);
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();
        ArgumentCaptor<Runnable> maxWaitTask = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(maxWaitTask.capture(), eq(200L), eq(TimeUnit.MILLISECONDS));

        StepVerifier.create(zipWithOneFailure(circuitBreaker))
            .expectError(IOException.class)
            .verify(Duration.ofSeconds(1));
        StepVerifier.create(zipWithOneFailure(circuitBreaker))
            .expectError(CallNotPermittedException.class)
            .verify(Duration.ofSeconds(1));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        maxWaitTask.getValue().run();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        verifyNoMoreInteractions(scheduler);
    }
}
