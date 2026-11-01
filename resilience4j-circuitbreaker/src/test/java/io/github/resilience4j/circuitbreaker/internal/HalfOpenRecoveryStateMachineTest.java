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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnHalfOpenPhaseTransitionEvent;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HalfOpenRecoveryStateMachineTest {
    private static final Duration DWELL = Duration.ofSeconds(10);

    @Test
    void automaticInitialProducesTheExpectedRampsAndValidatesTheLastPhase() {
        int[][] ramps = {{30, 60, 90, 100}, {45, 90, 100}, {100}};
        for (int[] percentages : ramps) {
            Fixture fixture = new Fixture(HalfOpenRecoveryConfig.custom()
                .trafficPercentageStep(percentages[0]).minimumNumberOfCalls(2)
                .minimumPhaseDuration(DWELL).build());
            for (int i = 0; i < percentages.length; i++) {
                assertPhase(fixture, i + 1, percentages[i]);

                assertThat(fixture.breaker.getHalfOpenPhase().getNumberOfPhases())
                    .isEqualTo(percentages.length);
                fixture.success();
                fixture.success();
                assertPhase(fixture, i + 1, percentages[i]);
                fixture.clock.advance(DWELL);
                fixture.acquireAdmitted();
                assertPhase(fixture, i + 1, percentages[i]);
                fixture.breaker.onSuccess(0, TimeUnit.NANOSECONDS);
            }

            assertThat(fixture.breaker.getState()).isEqualTo(CLOSED);
        }
    }

    @Test
    void automaticStepBackReturnsToThePreviousRampTargetAndReopensAtTheFirst() {
        Fixture fixture = new Fixture(HalfOpenRecoveryConfig.custom().trafficPercentageStep(30)
            .minimumNumberOfCalls(2).minimumPhaseDuration(DWELL).failureStrategy(STEP_BACK).build());
        fixture.completeHealthyPhase();
        fixture.completeHealthyPhase();
        fixture.completeHealthyPhase();
        assertPhase(fixture, 4, 100);
        for (int target : new int[]{90, 60, 30}) {
            fixture.failure();
            fixture.failure();

            assertThat(fixture.breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(target);
        }
        fixture.failure();
        fixture.failure();

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
    }

    @Test
    void admitsTheConfiguredPercentageOfEveryHundredAttemptsWithoutFiniteQuota() {
        for (int percentage : new int[]{1, 5, 7, 10, 35, 100}) {
            Fixture fixture = new Fixture(recovery().initialTrafficPercentage(percentage).build());
            int admitted = 0;
            for (int i = 0; i < 10_000; i++) {
                if (fixture.breaker.tryAcquirePermission()) {
                    admitted++;
                }
            }

            assertThat(admitted).isEqualTo(percentage * 100);
            assertThat(fixture.breaker.getState()).isEqualTo(HALF_OPEN);
            assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        }
    }

    @Test
    void spreadsFivePercentAcrossTheTrafficInsteadOfAdmittingABurstOfFive() {
        Fixture fixture = new Fixture(recovery().initialTrafficPercentage(5).build());
        for (int segment = 0; segment < 5; segment++) {
            int admitted = 0;
            for (int i = 0; i < 20; i++) {
                admitted += fixture.breaker.tryAcquirePermission() ? 1 : 0;
            }

            assertThat(admitted).isEqualTo(1);
        }
    }

    @Test
    void rejectedTrafficUsesTheExistingFallbackSignalWithoutHealthSamples() {
        Fixture fixture = new Fixture(recovery().initialTrafficPercentage(5).build());
        AtomicInteger rejectedEvents = new AtomicInteger();
        fixture.breaker.getEventPublisher().onCallNotPermitted(event -> rejectedEvents.incrementAndGet());

        assertThat(fixture.breaker.tryAcquirePermission()).isTrue();

        assertThatThrownBy(fixture.breaker::acquirePermission)
            .isInstanceOf(CallNotPermittedException.class);

        assertThat(rejectedEvents.get()).isEqualTo(1);
        assertThat(fixture.breaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
    }

    @Test
    void requiresBothHealthySamplesAndMinimumDurationIncludingAtOneHundredPercent() {
        Fixture fixture = new Fixture(recovery().build());
        fixture.success();
        fixture.success();
        assertPhase(fixture, 1, 50);
        fixture.clock.advance(DWELL.minusNanos(1));
        fixture.breaker.tryAcquirePermission();
        assertPhase(fixture, 1, 50);
        fixture.clock.advance(Duration.ofNanos(1));

        fixture.acquireAdmitted();
        assertPhase(fixture, 1, 50);
        fixture.breaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertPhase(fixture, 2, 100);
        fixture.success();
        fixture.success();

        assertThat(fixture.breaker.getState()).isEqualTo(HALF_OPEN);
        fixture.clock.advance(DWELL);
        fixture.acquireAdmitted();
        assertPhase(fixture, 2, 100);
        fixture.breaker.onSuccess(0, TimeUnit.NANOSECONDS);

        assertThat(fixture.breaker.getState()).isEqualTo(CLOSED);
        assertThat(fixture.breaker.getHalfOpenPhase().isActive()).isFalse();
    }

    @Test
    void insufficientTrafficHoldsThePhaseAndTheFirstOfferedCallCanBeSampled() {
        Fixture fixture = new Fixture(recovery().initialTrafficPercentage(5).build());
        fixture.clock.advance(Duration.ofHours(1));
        fixture.success();
        assertPhase(fixture, 1, 5);
        fixture.success();
        assertPhase(fixture, 2, 55);
    }

    @Test
    void clockRollbackCannotAdvanceAHealthyPhase() {
        Fixture fixture = new Fixture(recovery().build());
        fixture.success();
        fixture.success();
        fixture.clock.advance(Duration.ofSeconds(-30));
        for (int i = 0; i < 100; i++) {
            fixture.breaker.tryAcquirePermission();
        }
        fixture.success();
        assertPhase(fixture, 1, 50);
    }

    @Test
    void recoveryMinimumIsIndependentOfTheLowerParentMinimum() {
        Fixture fixture = new Fixture(recovery().build());
        fixture.failure();

        assertThat(fixture.breaker.getState()).isEqualTo(HALF_OPEN);
        fixture.failure();

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
    }

    @Test
    void stepsBackOnePhaseAndReopensWhenTheFirstPhaseFails() {
        Fixture fixture = new Fixture(recovery().failureStrategy(STEP_BACK).build());
        fixture.completeHealthyPhase();
        assertPhase(fixture, 2, 100);
        fixture.failure();
        fixture.failure();
        assertPhase(fixture, 1, 50);
        fixture.failure();
        fixture.failure();

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
    }

    @Test
    void slowSuccessfulCallsCanReopenRecovery() {
        Fixture fixture = new Fixture(recovery().build());
        fixture.acquireAdmitted();
        fixture.breaker.onSuccess(61, TimeUnit.SECONDS);
        fixture.acquireAdmitted();
        fixture.breaker.onSuccess(61, TimeUnit.SECONDS);

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
    }

    @Test
    void healthWindowIsBoundedAndIgnoredOutcomesDoNotContributeSamples() {
        Fixture fixture = new Fixture(recovery().build(), config ->
            config.ignoreExceptions(IllegalArgumentException.class));
        for (int i = 0; i < 100; i++) {
            fixture.acquireAdmitted();
            fixture.breaker.onError(0, TimeUnit.NANOSECONDS, new IllegalArgumentException("ignored"));
        }

        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        for (int i = 0; i < 100; i++) {
            fixture.success();
        }

        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertPhase(fixture, 1, 50);
    }

    @Test
    void deadlineIsSharedAcrossAdvancesAndStepBacksAndReopensRecovery() {
        Fixture fixture = new Fixture(recovery().failureStrategy(STEP_BACK).build(), config ->
            config.maxWaitDurationInHalfOpenState(Duration.ofSeconds(30)));

        assertThat(fixture.scheduler.tasks).hasSize(1);
        fixture.completeHealthyPhase();
        assertPhase(fixture, 2, 100);

        assertThat(fixture.scheduler.tasks).hasSize(1);
        verify(fixture.scheduler.futures.get(0), never()).cancel(anyBoolean());
        fixture.failure();
        fixture.failure();
        assertPhase(fixture, 1, 50);
        assertThat(fixture.scheduler.tasks).hasSize(1);
        verify(fixture.scheduler.futures.get(0), never()).cancel(anyBoolean());
        fixture.scheduler.tasks.get(0).run();

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
        verify(fixture.scheduler.futures.get(0)).cancel(false);
    }

    @Test
    void resetCancelsTheDeadlineAndAnAlreadyStartedCallbackCannotReopenIt() {
        Fixture fixture = new Fixture(recovery().build(), config ->
            config.maxWaitDurationInHalfOpenState(Duration.ofSeconds(30)));
        fixture.breaker.reset();
        verify(fixture.scheduler.futures.get(0)).cancel(false);
        fixture.scheduler.tasks.get(0).run();

        assertThat(fixture.breaker.getState()).isEqualTo(CLOSED);
    }

    @Test
    void staleDeadlineCannotAffectANewRecoveryCycle() {
        Fixture fixture = new Fixture(recovery().build(), config ->
            config.maxWaitDurationInHalfOpenState(Duration.ofSeconds(30)));
        Runnable oldDeadline = fixture.scheduler.tasks.get(0);
        fixture.breaker.transitionToOpenState();
        fixture.breaker.transitionToHalfOpenState();
        oldDeadline.run();
        assertPhase(fixture, 1, 50);

        assertThat(fixture.scheduler.tasks).hasSize(2);
        fixture.scheduler.tasks.get(1).run();

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
    }

    @Test
    void phaseChangesDoNotInflateTheBackoffAttempt() {
        List<Integer> attempts = new ArrayList<>();
        Fixture fixture = new Fixture(recovery().build(), config ->
            config.waitIntervalFunctionInOpenState(attempt -> {
                attempts.add(attempt);
                return 1_000L;
            }));
        fixture.completeHealthyPhase();
        fixture.failure();
        fixture.failure();

        assertThat(attempts).containsExactly(1, 2);
    }

    @Test
    void initialHalfOpenStateUsesRecoveryAndNullRestoresLegacyFiniteProbes() {
        MutableClock clock = new MutableClock();
        CircuitBreakerConfig enabled = CircuitBreakerConfig.custom().clock(clock)
            .initialState(HALF_OPEN).halfOpenRecoveryConfig(recovery().build()).build();
        CircuitBreaker phased = new CircuitBreakerStateMachine("initial-phased", enabled);

        assertThat(phased.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(50);
        CircuitBreakerConfig disabled = CircuitBreakerConfig.from(enabled)
            .halfOpenRecoveryConfig(null).permittedNumberOfCallsInHalfOpenState(3).build();
        CircuitBreaker legacy = new CircuitBreakerStateMachine("initial-legacy", disabled);

        assertThat(legacy.getHalfOpenPhase().isActive()).isFalse();
        for (int i = 0; i < 3; i++) {
            assertThat(legacy.tryAcquirePermission()).isTrue();
        }

        assertThat(legacy.tryAcquirePermission()).isFalse();
    }

    @Test
    void clampTheFinalTrafficStepToOneHundred() {
        Fixture fixture = new Fixture(recovery().initialTrafficPercentage(35)
            .trafficPercentageStep(40).build());
        assertThat(fixture.breaker.getHalfOpenPhase().getNumberOfPhases()).isEqualTo(3);
        fixture.completeHealthyPhase();
        assertPhase(fixture, 2, 75);
        fixture.completeHealthyPhase();
        assertPhase(fixture, 3, 100);
        fixture.completeHealthyPhase();

        assertThat(fixture.breaker.getState()).isEqualTo(CLOSED);
    }

    @Test
    void concurrentAdmissionHasTheExactAggregatePercentage() throws Exception {
        Fixture fixture = new Fixture(recovery().initialTrafficPercentage(7).build());
        List<Integer> counts = concurrent(8, () -> {
            int accepted = 0;
            for (int i = 0; i < 1_000; i++) {
                accepted += fixture.breaker.tryAcquirePermission() ? 1 : 0;
            }
            return accepted;
        });

        assertThat(counts.stream().mapToInt(Integer::intValue).sum()).isEqualTo(560);
    }

    @Test
    void concurrentCompletionsAfterDwellPublishOneSuccessorPhase() throws Exception {
        Fixture fixture = new Fixture(recovery().build());
        AtomicInteger advanced = new AtomicInteger();
        fixture.breaker.getEventPublisher().onHalfOpenPhaseTransition(event -> {
            if (event.getReason() == CircuitBreakerOnHalfOpenPhaseTransitionEvent.Reason.ADVANCED) {
                advanced.incrementAndGet();
            }
        });
        fixture.success();
        for (int call = 0; call < 8; call++) {
            fixture.acquireAdmitted();
        }
        fixture.clock.advance(DWELL);
        concurrent(8, () -> {
            fixture.breaker.onSuccess(0, TimeUnit.NANOSECONDS);
            return 1;
        });
        assertPhase(fixture, 2, 100);

        assertThat(advanced.get()).isEqualTo(1);
    }

    @Test
    void reentrantStateListenerResetDoesNotPublishAnObsoleteEnteredPhase() {
        MutableClock clock = new MutableClock();
        CircuitBreaker breaker = new CircuitBreakerStateMachine("reentrant-entry",
            CircuitBreakerConfig.custom().clock(clock)
                .halfOpenRecoveryConfig(recovery().build()).build());
        AtomicInteger phaseEvents = new AtomicInteger();
        breaker.getEventPublisher().onHalfOpenPhaseTransition(event -> phaseEvents.incrementAndGet());
        breaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == HALF_OPEN) {
                breaker.reset();
            }
        });
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();

        assertThat(breaker.getState()).isEqualTo(CLOSED);
        assertThat(phaseEvents.get()).isZero();
    }

    @Test
    void holdsIntermediateHealthAndRequiresAFullWindowBeforeSteppingBack() {
        Fixture fixture = new Fixture(HalfOpenRecoveryConfig.custom().initialTrafficPercentage(50)
            .trafficPercentageStep(25).minimumNumberOfCalls(100).minimumPhaseDuration(DWELL)
            .failureStrategy(STEP_BACK).build());
        fixture.clock.advance(DWELL);
        for (int call = 0; call < 100; call++) {
            if (call < 6) {
                fixture.failure();
            } else {
                fixture.success();
            }
        }
        assertPhase(fixture, 1, 50);
        assertThat(fixture.breaker.getMetrics().getFailureRate()).isEqualTo(6);
        fixture.success(); // Evict one failure: exactly 5% is healthy enough to advance.
        assertPhase(fixture, 2, 75);
        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();

        for (int call = 0; call < 99; call++) {
            if (call < 20) {
                fixture.failure();
            } else {
                fixture.success();
            }
        }
        assertPhase(fixture, 2, 75); // A single failure did not cause an immediate regression.
        fixture.success(); // Complete the 100-outcome window, exactly 20% failures.
        assertPhase(fixture, 1, 50); // Regression does not wait for the minimum dwell.
        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        fixture.failure();
        assertPhase(fixture, 1, 50);
        for (int call = 0; call < 99; call++) {
            fixture.success();
        }
        assertPhase(fixture, 1, 50); // Healthy again, but the restarted dwell still applies.
        fixture.clock.advance(DWELL);
        fixture.success();
        assertPhase(fixture, 2, 75);
    }

    @Test
    void optingIntoRecoveryDoesNotTightenTheClosedState() {
        CircuitBreaker breaker = new CircuitBreakerStateMachine("ordinary-closed-health",
            CircuitBreakerConfig.custom().minimumNumberOfCalls(100).slidingWindowSize(100)
                .failureRateThreshold(50).halfOpenRecoveryConfig(recovery().build()).build());
        for (int call = 0; call < 100; call++) {
            if (call < 20) {
                try {
                    breaker.executeRunnable(() -> { throw new IllegalStateException("failure"); });
                } catch (IllegalStateException expected) {
                    // Only technical failures are recorded; no administrative state forcing.
                }
            } else {
                breaker.executeRunnable(() -> { });
            }
        }
        assertThat(breaker.getState()).isEqualTo(CLOSED);
        assertThat(breaker.getMetrics().getFailureRate()).isEqualTo(20);
    }

    @Test
    void finalIntermediateHealthStepsBackOnlyAfterDurationAndAFreshCompletion() {
        Fixture fixture = finalPhaseFixture(STEP_BACK, config -> { });
        AtomicInteger exceeded = new AtomicInteger();
        AtomicInteger steppedBack = new AtomicInteger();
        fixture.breaker.getEventPublisher().onFailureRateExceeded(event -> exceeded.incrementAndGet());
        fixture.breaker.getEventPublisher().onSlowCallRateExceeded(event -> exceeded.incrementAndGet());
        fixture.breaker.getEventPublisher().onHalfOpenPhaseTransition(event -> steppedBack.incrementAndGet());
        fixture.clock.advance(DWELL.minusNanos(1));
        recordWindow(fixture, 8);
        assertPhase(fixture, 2, 100);
        fixture.clock.advance(Duration.ofNanos(1));
        assertPhase(fixture, 2, 100); // Time alone cannot evaluate the phase.
        fixture.success();

        assertPhase(fixture, 1, 75);
        assertThat(steppedBack.get()).isEqualTo(1);
        assertThat(exceeded.get()).isZero(); // No 20% failure or 50% slow threshold was exceeded.
        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
    }

    @Test
    void finalIntermediateHealthUsesTheOpenFailureStrategyWithoutAFalseThresholdEvent() {
        Fixture fixture = finalPhaseFixture(HalfOpenRecoveryConfig.FailureStrategy.OPEN, config -> { });
        AtomicInteger exceeded = new AtomicInteger();
        fixture.breaker.getEventPublisher().onFailureRateExceeded(event -> exceeded.incrementAndGet());
        fixture.clock.advance(DWELL);
        recordWindow(fixture, 8);

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
        assertThat(exceeded.get()).isZero();
    }

    @Test
    void finalOrdinaryHealthGuardIsDeferredAndDoesNotAffectEarlierRecoveryPhases() {
        Fixture fixture = finalPhaseFixture(STEP_BACK, config -> config.failureRateThreshold(1));
        AtomicInteger exceeded = new AtomicInteger();
        fixture.breaker.getEventPublisher().onFailureRateExceeded(event -> exceeded.incrementAndGet());
        recordWindow(fixture, 1); // Healthy for smart advancement; exactly the ordinary OPEN bound.
        assertPhase(fixture, 2, 100);
        fixture.clock.advance(DWELL);
        assertPhase(fixture, 2, 100);
        fixture.success();

        assertPhase(fixture, 1, 75);
        assertThat(exceeded.get()).isZero();
        fixture.clock.advance(DWELL);
        recordWindow(fixture, 1);
        assertPhase(fixture, 2, 100); // The ordinary guard applies only at the final target.
    }

    @Test
    void finalSevereHealthStillStepsBackBeforeItsMinimumDuration() {
        Fixture fixture = finalPhaseFixture(STEP_BACK, config -> { });
        AtomicInteger exceeded = new AtomicInteger();
        fixture.breaker.getEventPublisher().onFailureRateExceeded(event -> exceeded.incrementAndGet());
        recordWindow(fixture, 20);

        assertPhase(fixture, 1, 75);
        assertThat(exceeded.get()).isEqualTo(1);
    }

    @Test
    void finalValidationDoesNotDecideBeforeTheWholeSampleWindowExists() {
        Fixture fixture = finalPhaseFixture(STEP_BACK, config -> { });
        fixture.clock.advance(DWELL.multipliedBy(10));
        for (int call = 0; call < 99; call++) {
            fixture.failure();
        }
        assertPhase(fixture, 2, 100);
        assertThat(fixture.breaker.getMetrics().getFailureRate()).isEqualTo(-1);
        fixture.failure();
        assertPhase(fixture, 1, 75);
    }

    @Test
    void exactFailureAndSlowThresholdsPublishOnceForTheWinningPhase() {
        Fixture fixture = new Fixture(recovery().failureRateThreshold(50).slowCallRateThreshold(50)
            .build(), config -> config.failureRateThreshold(50).slowCallRateThreshold(50));
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger slow = new AtomicInteger();
        fixture.breaker.getEventPublisher().onFailureRateExceeded(event -> failures.incrementAndGet());
        fixture.breaker.getEventPublisher().onSlowCallRateExceeded(event -> slow.incrementAndGet());
        fixture.success();
        fixture.acquireAdmitted();
        fixture.breaker.onError(61, TimeUnit.SECONDS, new IllegalStateException("slow PSP failure"));

        assertThat(fixture.breaker.getState()).isEqualTo(OPEN);
        assertThat(failures.get()).isEqualTo(1);
        assertThat(slow.get()).isEqualTo(1);
        assertThat(fixture.breaker.getMetrics().getFailureRate()).isEqualTo(50);
        assertThat(fixture.breaker.getMetrics().getSlowCallRate()).isEqualTo(50);
        fixture.breaker.onError(61, TimeUnit.SECONDS, new IllegalStateException("late outcome"));

        assertThat(failures.get()).isEqualTo(1);
        assertThat(slow.get()).isEqualTo(1);
    }

    @Test
    void thresholdListenerResetIsNotOverriddenAfterItsCallback() {
        Fixture fixture = new Fixture(recovery().build());
        List<CircuitBreaker.StateTransition> transitions = new ArrayList<>();
        fixture.breaker.getEventPublisher().onStateTransition(event ->
            transitions.add(event.getStateTransition()));
        fixture.breaker.getEventPublisher().onFailureRateExceeded(event -> fixture.breaker.reset());
        fixture.failure();
        fixture.failure();

        assertThat(fixture.breaker.getState()).isEqualTo(CLOSED);
        assertThat(transitions).containsExactly(
            CircuitBreaker.StateTransition.HALF_OPEN_TO_OPEN,
            CircuitBreaker.StateTransition.OPEN_TO_CLOSED);
    }

    @Test
    void callbackDispatchedToAnOldPhaseCannotOverrideItsSuccessor() throws Exception {
        Fixture fixture = new Fixture(recovery().build());
        // Model a callback paused after reading stateReference, before invoking
        // that state's method. Public callbacks otherwise target the current state.
        java.lang.reflect.Field referenceField = CircuitBreakerStateMachine.class
            .getDeclaredField("stateReference");
        referenceField.setAccessible(true);
        Object previous = ((AtomicReference<?>) referenceField.get(fixture.breaker)).get();
        fixture.completeHealthyPhase();
        assertPhase(fixture, 2, 100);
        java.lang.reflect.Method onError = previous.getClass().getDeclaredMethod(
            "onError", long.class, TimeUnit.class, Throwable.class);
        onError.setAccessible(true);
        onError.invoke(previous, 0L, TimeUnit.NANOSECONDS, new IllegalStateException("late"));
        onError.invoke(previous, 0L, TimeUnit.NANOSECONDS, new IllegalStateException("late"));
        assertPhase(fixture, 2, 100);

        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
    }

    @Test
    void publicHalfOpenTransitionPreservesActiveRecoveryProgressAndDeadline() {
        Fixture fixture = new Fixture(recovery().initialTrafficPercentage(25)
            .trafficPercentageStep(25).build(), config ->
            config.maxWaitDurationInHalfOpenState(Duration.ofSeconds(30)));
        fixture.completeHealthyPhase();
        assertPhase(fixture, 2, 50);
        fixture.success();
        fixture.success();
        fixture.clock.advance(DWELL.minusSeconds(1));

        fixture.breaker.transitionToHalfOpenState();

        assertPhase(fixture, 2, 50);

        assertThat(fixture.breaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(fixture.scheduler.tasks).hasSize(1);
        verify(fixture.scheduler.futures.get(0), never()).cancel(anyBoolean());
        fixture.clock.advance(Duration.ofSeconds(1));
        fixture.acquireAdmitted();
        assertPhase(fixture, 2, 50);
        fixture.breaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertPhase(fixture, 3, 75);

        assertThat(fixture.scheduler.tasks).hasSize(1);
    }

    private static List<Integer> concurrent(int threads,
        java.util.concurrent.Callable<Integer> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return task.call();
                }));
            }
            List<Integer> values = new ArrayList<>();
            for (Future<Integer> future : futures) {
                values.add(future.get(10, TimeUnit.SECONDS));
            }
            return values;
        } finally {
            executor.shutdownNow();
        }
    }

    private static Fixture finalPhaseFixture(HalfOpenRecoveryConfig.FailureStrategy strategy,
        java.util.function.Consumer<CircuitBreakerConfig.Builder> customize) {
        Fixture fixture = new Fixture(HalfOpenRecoveryConfig.custom().initialTrafficPercentage(75)
            .trafficPercentageStep(25).minimumNumberOfCalls(100).minimumPhaseDuration(DWELL)
            .failureStrategy(strategy).build(), customize);
        fixture.clock.advance(DWELL);
        recordWindow(fixture, 0);
        assertPhase(fixture, 2, 100);
        return fixture;
    }

    private static void recordWindow(Fixture fixture, int failures) {
        for (int call = 0; call < 100; call++) {
            if (call >= 100 - failures) {
                fixture.failure();
            } else {
                fixture.success();
            }
        }
    }

    private static HalfOpenRecoveryConfig.Builder recovery() {
        return HalfOpenRecoveryConfig.custom().initialTrafficPercentage(50)
            .trafficPercentageStep(50).minimumPhaseDuration(DWELL).minimumNumberOfCalls(2);
    }

    private static void assertPhase(Fixture fixture, int index, int percentage) {
        assertThat(fixture.breaker.getState()).isEqualTo(HALF_OPEN);
        assertThat(fixture.breaker.getHalfOpenPhase().getPhase()).isEqualTo(index);
        assertThat(fixture.breaker.getHalfOpenPhase().getPermittedTrafficPercentage())
            .isEqualTo(percentage);
    }

    private static final class Fixture {
        final MutableClock clock = new MutableClock();
        final CapturingScheduler scheduler = new CapturingScheduler();
        final CircuitBreaker breaker;

        Fixture(HalfOpenRecoveryConfig recovery) {
            this(recovery, config -> { });
        }

        Fixture(HalfOpenRecoveryConfig recovery,
            java.util.function.Consumer<CircuitBreakerConfig.Builder> customize) {
            CircuitBreakerConfig.Builder config = CircuitBreakerConfig.custom().clock(clock)
                .minimumNumberOfCalls(1).halfOpenRecoveryConfig(recovery);
            customize.accept(config);
            breaker = new CircuitBreakerStateMachine("recovery", config.build(), scheduler.factory);
            breaker.transitionToOpenState();
            breaker.transitionToHalfOpenState();
        }

        void acquireAdmitted() {
            for (int i = 0; i < 100; i++) {
                if (breaker.tryAcquirePermission()) {
                    return;
                }
            }
            throw new AssertionError("A positive recovery percentage must admit within 100 attempts");
        }

        void success() {
            acquireAdmitted();
            breaker.onSuccess(0, TimeUnit.NANOSECONDS);
        }

        void failure() {
            acquireAdmitted();
            breaker.onError(0, TimeUnit.NANOSECONDS, new IllegalStateException("PSP failure"));
        }

        void completeHealthyPhase() {
            success();
            acquireAdmitted();
            clock.advance(DWELL);
            breaker.onSuccess(0, TimeUnit.NANOSECONDS);
        }
    }

    private static final class CapturingScheduler {
        final SchedulerFactory factory = mock(SchedulerFactory.class);
        final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        final List<Runnable> tasks = new ArrayList<>();
        final List<ScheduledFuture<?>> futures = new ArrayList<>();

        CapturingScheduler() {
            when(factory.getScheduler()).thenReturn(executor);
            when(executor.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenAnswer(invocation -> {
                    tasks.add(invocation.getArgument(0));
                    ScheduledFuture<?> future = mock(ScheduledFuture.class);
                    futures.add(future);
                    return future;
                });
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;
        private final ZoneId zone;

        MutableClock() {
            this(new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z")), ZoneOffset.UTC);
        }

        private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
            this.now = now;
            this.zone = zone;
        }

        void advance(Duration duration) {
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
