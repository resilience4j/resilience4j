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
import java.util.concurrent.TimeUnit;

import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK;
import static io.github.resilience4j.circuitbreaker.internal.CircuitBreakerMetrics.Result;
import static io.github.resilience4j.circuitbreaker.internal.HalfOpenRecoveryPolicy.Decision;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HalfOpenRecoveryPolicyTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration DWELL = Duration.ofSeconds(10);

    @Test
    void automaticRampsAdvanceToTheirClampedFinalPhaseBeforeClosing() {
        int[][] ramps = {{30, 60, 90, 100}, {45, 90, 100}, {100}};
        for (int[] targets : ramps) {
            HalfOpenRecoveryPolicy policy = new HalfOpenRecoveryPolicy(config()
                .trafficPercentageStep(targets[0]).build());
            for (int index = 0; index < targets.length; index++) {
                var phase = policy.newPhase(index, START);

                assertThat(phase.getSnapshot().getPermittedTrafficPercentage()).isEqualTo(targets[index]);
                assertThat(phase.getSnapshot().getNumberOfPhases()).isEqualTo(targets.length);
                assertThat(phase.evaluate(Result.BELOW_THRESHOLDS, at(START))).isEqualTo(Decision.STAY);
                assertThat(phase.evaluate(Result.BELOW_THRESHOLDS, at(START.plus(DWELL))))
                    .isEqualTo(index == targets.length - 1 ? Decision.CLOSE : Decision.ADVANCE);
            }
        }
    }

    @Test
    void anExplicitStartKeepsItsOffsetWhenTheLastStepIsClamped() {
        HalfOpenRecoveryPolicy policy = new HalfOpenRecoveryPolicy(config()
            .initialTrafficPercentage(5).trafficPercentageStep(30).build());
        int[] targets = {5, 35, 65, 95, 100};
        for (int index = 0; index < targets.length; index++) {
            var phase = policy.newPhase(index, START);

            assertThat(phase.getSnapshot().getPermittedTrafficPercentage()).isEqualTo(targets[index]);
            assertThat(phase.getSnapshot().getPhase()).isEqualTo(index + 1);
        }

        assertThatThrownBy(() -> policy.newPhase(-1, START)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.newPhase(targets.length, START))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elapsedTimeAloneOrHealthySamplesAloneCannotAdvance() {
        var phase = new HalfOpenRecoveryPolicy(config().build()).newPhase(0, START);

        assertThat(phase.evaluate(Result.BELOW_MINIMUM_CALLS_THRESHOLD, at(START.plusSeconds(60))))
            .isEqualTo(Decision.STAY);
        assertThat(phase.evaluate(Result.BELOW_THRESHOLDS, at(START.plus(DWELL).minusNanos(1))))
            .isEqualTo(Decision.STAY);
        assertThat(phase.evaluate(Result.BELOW_THRESHOLDS, at(START.minusSeconds(60))))
            .isEqualTo(Decision.STAY);
        assertThat(phase.evaluate(Result.BELOW_THRESHOLDS, at(START.plus(DWELL))))
            .isEqualTo(Decision.ADVANCE);
    }

    @Test
    void unhealthyEvaluationStepsBackExceptAtTheFirstPhase() {
        HalfOpenRecoveryPolicy policy = new HalfOpenRecoveryPolicy(config()
            .trafficPercentageStep(30).failureStrategy(STEP_BACK).build());
        for (Result result : exceededThresholds()) {
            assertThat(policy.newPhase(0, START).evaluate(result, unreadableClock()))
                .isEqualTo(Decision.OPEN);
            for (int index = 1; index < 4; index++) {
                assertThat(policy.newPhase(index, START).evaluate(result, unreadableClock()))
                    .isEqualTo(Decision.STEP_BACK);
            }
        }
    }

    @Test
    void openFailureStrategyNeverDependsOnDwellOrPhasePosition() {
        HalfOpenRecoveryPolicy policy = new HalfOpenRecoveryPolicy(config()
            .trafficPercentageStep(30).build());
        for (int index = 0; index < 4; index++) {
            for (Result result : exceededThresholds()) {
                assertThat(policy.newPhase(index, START).evaluate(result, unreadableClock()))
                    .isEqualTo(Decision.OPEN);
            }
        }

        assertThat(policy.newPhase(0, START)
            .evaluate(Result.BELOW_MINIMUM_CALLS_THRESHOLD, unreadableClock())).isEqualTo(Decision.STAY);
    }

    @Test
    void intermediateHealthCannotAdvanceOrRegressEvenAfterTheMinimumDuration() {
        var phase = new HalfOpenRecoveryPolicy(config().failureStrategy(STEP_BACK).build())
            .newPhase(1, START);
        assertThat(phase.evaluate(Result.RECOVERY_HOLD, unreadableClock())).isEqualTo(Decision.STAY);
        assertThat(phase.evaluate(Result.RECOVERY_HOLD, at(START.plusSeconds(600))))
            .isEqualTo(Decision.STAY);
    }

    @Test
    void finalPhaseRejectsIntermediateHealthAfterItsMinimumDuration() {
        for (var strategy : HalfOpenRecoveryConfig.FailureStrategy.values()) {
            var phase = new HalfOpenRecoveryPolicy(config().trafficPercentageStep(25)
                .failureStrategy(strategy).build()).newPhase(3, START);
            assertThat(phase.evaluate(Result.RECOVERY_HOLD, at(START.plus(DWELL).minusNanos(1))))
                .isEqualTo(Decision.STAY);
            assertThat(phase.evaluate(Result.RECOVERY_HOLD, at(START.plus(DWELL))))
                .isEqualTo(strategy == STEP_BACK ? Decision.STEP_BACK : Decision.OPEN);
            assertThat(phase.evaluate(Result.BELOW_MINIMUM_CALLS_THRESHOLD,
                at(START.plusSeconds(600)))).isEqualTo(Decision.STAY);
        }
    }

    @Test
    void aSingleHundredPercentPhaseHasNoPrecedingStepToReturnTo() {
        var phase = new HalfOpenRecoveryPolicy(config().trafficPercentageStep(100)
            .failureStrategy(STEP_BACK).build()).newPhase(0, START);
        assertThat(phase.evaluate(Result.RECOVERY_HOLD, at(START.plus(DWELL))))
            .isEqualTo(Decision.OPEN);
        assertThat(phase.evaluate(Result.BELOW_THRESHOLDS, at(START.plus(DWELL))))
            .isEqualTo(Decision.CLOSE);
    }

    @Test
    void admissionIsEvenlyDistributedAndDoesNotExhaustAFiniteQuota() {
        var phase = new HalfOpenRecoveryPolicy(config().build()).newPhase(0, START);
        for (int segment = 0; segment < 100; segment++) {
            int admitted = 0;
            for (int attempt = 0; attempt < 20; attempt++) {
                if (phase.tryAcquirePermission()) {
                    admitted++;
                }
            }

            assertThat(admitted).isEqualTo(1);
        }
        var fullyOpenPhase = new HalfOpenRecoveryPolicy(config().initialTrafficPercentage(100).build())
            .newPhase(0, START);
        for (int attempt = 0; attempt < 1000; attempt++) {
            assertThat(fullyOpenPhase.tryAcquirePermission()).isTrue();
        }
    }

    @Test
    void aFreshPhaseRestartsItsDwellAndSharesTheRecoveryAdmissionBudget() {
        HalfOpenRecoveryPolicy policy = new HalfOpenRecoveryPolicy(config().build());
        var previous = policy.newPhase(0, START);

        assertThat(previous.tryAcquirePermission()).isTrue();
        assertThat(previous.tryAcquirePermission()).isFalse();
        var retried = policy.newPhase(0, START.plus(DWELL));

        assertThat(retried.tryAcquirePermission()).isFalse();
        assertThat(previous.tryAcquirePermission()).isFalse();
        assertThat(previous.evaluate(Result.BELOW_THRESHOLDS, at(START.plus(DWELL))))
            .isEqualTo(Decision.ADVANCE);
        assertThat(retried.evaluate(Result.BELOW_THRESHOLDS, at(START.plus(DWELL))))
            .isEqualTo(Decision.STAY);
    }

    @Test
    void recreatingAFiftyPercentPhaseCannotMintAdditionalAdmissions() {
        HalfOpenRecoveryPolicy policy = new HalfOpenRecoveryPolicy(config()
            .initialTrafficPercentage(50).build());
        int admitted = 0;
        for (int attempt = 0; attempt < 100; attempt++) {
            var phase = policy.newPhase(0, START.plusNanos(attempt));
            if (phase.tryAcquirePermission()) {
                admitted++;
            }
        }

        assertThat(admitted).isEqualTo(50);
    }

    @Test
    void advancingAndSteppingBackPreserveTheFractionalBudgetAtEveryAttempt() {
        HalfOpenRecoveryPolicy policy = new HalfOpenRecoveryPolicy(config()
            .trafficPercentageStep(25).build());
        int[] phaseIndices = {0, 1, 2, 1, 0, 1, 2, 3, 2, 1, 0};
        int admitted = 0;
        int offeredPercentagePoints = 0;
        int attempts = 0;
        for (int cycle = 0; cycle < 20; cycle++) {
            for (int index : phaseIndices) {
                var phase = policy.newPhase(index, START.plusNanos(attempts));
                int target = phase.getSnapshot().getPermittedTrafficPercentage();
                for (int offer = 0; offer < 3; offer++) {
                    offeredPercentagePoints += target;
                    attempts++;
                    if (phase.tryAcquirePermission()) {
                        admitted++;
                    }

                    assertThat(Math.abs(100 * admitted - offeredPercentagePoints))
                        .as("Admission differs from accumulated targets by less than one at attempt %s",
                            attempts)
                        .isLessThan(100);
                }
            }
        }
    }

    @Test
    void concurrentAdmissionPreservesTheConfiguredAggregatePercentage() throws Exception {
        var phase = new HalfOpenRecoveryPolicy(config().initialTrafficPercentage(37).build())
            .newPhase(0, START);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CyclicBarrier start = new CyclicBarrier(8);
        try {
            List<Future<Integer>> tasks = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) {
                tasks.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    int admitted = 0;
                    for (int attempt = 0; attempt < 1000; attempt++) {
                        if (phase.tryAcquirePermission()) {
                            admitted++;
                        }
                    }
                    return admitted;
                }));
            }
            int admitted = 0;
            for (Future<Integer> task : tasks) {
                admitted += task.get(10, TimeUnit.SECONDS);
            }

            assertThat(admitted).isEqualTo(2960);
        } finally {
            executor.shutdownNow();
        }
    }

    private static HalfOpenRecoveryConfig.Builder config() {
        return HalfOpenRecoveryConfig.custom().trafficPercentageStep(5).minimumPhaseDuration(DWELL);
    }

    private static Result[] exceededThresholds() {
        return new Result[]{Result.FAILURE_RATE_ABOVE_THRESHOLDS,
            Result.SLOW_CALL_RATE_ABOVE_THRESHOLDS, Result.ABOVE_THRESHOLDS};
    }

    private static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static Clock unreadableClock() {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                throw new AssertionError("This decision does not need the time");
            }
        };
    }
}
