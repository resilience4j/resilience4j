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

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.circuitbreaker.internal.CircuitBreakerMetrics.Result;
import static org.assertj.core.api.Assertions.assertThat;

class HalfOpenRecoveryHealthTest {

    @Test
    void inclusiveBoundsLeaveAStableHoldBandForEachHealthDimension() {
        assertThat(evaluate(5, 10)).isEqualTo(Result.BELOW_THRESHOLDS);
        assertThat(evaluate(6, 10)).isEqualTo(Result.RECOVERY_HOLD);
        assertThat(evaluate(5, 11)).isEqualTo(Result.RECOVERY_HOLD);
        assertThat(evaluate(19, 49)).isEqualTo(Result.RECOVERY_HOLD);
        assertThat(evaluate(20, 0)).isEqualTo(Result.FAILURE_RATE_ABOVE_THRESHOLDS);
        assertThat(evaluate(0, 50)).isEqualTo(Result.SLOW_CALL_RATE_ABOVE_THRESHOLDS);
        assertThat(evaluate(20, 50)).isEqualTo(Result.ABOVE_THRESHOLDS);
    }

    @Test
    void regressionAlsoWaitsForTheCompleteRecoveryWindow() {
        var config = config(HalfOpenRecoveryConfig.custom().minimumNumberOfCalls(100).build());
        var metrics = CircuitBreakerMetrics.forHalfOpenRecovery(100, config);
        for (int call = 0; call < 99; call++) {
            assertThat(metrics.onError(61, TimeUnit.SECONDS))
                .isEqualTo(Result.BELOW_MINIMUM_CALLS_THRESHOLD);
        }
        assertThat(metrics.onError(61, TimeUnit.SECONDS)).isEqualTo(Result.ABOVE_THRESHOLDS);
    }

    @Test
    void zeroAdvanceMaximumRequiresZeroObservedFailuresWithoutTrippingAtZero() {
        var policy = HalfOpenRecoveryConfig.custom().maxFailureRateForAdvance(0)
            .maxSlowCallRateForAdvance(0).minimumNumberOfCalls(100).build();
        var metrics = CircuitBreakerMetrics.forHalfOpenRecovery(100, config(policy));
        for (int call = 0; call < 100; call++) {
            metrics.onSuccess(0, TimeUnit.NANOSECONDS);
        }
        assertThat(metrics.checkIfThresholdsExceeded()).isEqualTo(Result.BELOW_THRESHOLDS);
        assertThat(metrics.onError(0, TimeUnit.NANOSECONDS)).isEqualTo(Result.RECOVERY_HOLD);
    }

    @Test
    void ordinaryClosedAndFiniteHalfOpenUseOnlyTheirExistingThresholds() {
        var config = config(HalfOpenRecoveryConfig.custom().build());
        for (var metrics : new CircuitBreakerMetrics[]{CircuitBreakerMetrics.forClosed(config),
            CircuitBreakerMetrics.forHalfOpen(100, config)}) {
            for (int call = 0; call < 100; call++) {
                if (call < 20) {
                    metrics.onError(0, TimeUnit.NANOSECONDS);
                } else {
                    metrics.onSuccess(0, TimeUnit.NANOSECONDS);
                }
            }
            assertThat(metrics.checkIfThresholdsExceeded()).isEqualTo(Result.BELOW_THRESHOLDS);
        }
    }

    @Test
    void onlyTheFinalPhaseAlsoRequiresOrdinaryFailureAndSlowCallHealth() {
        var policy = HalfOpenRecoveryConfig.custom().minimumNumberOfCalls(100).build();
        var config = CircuitBreakerConfig.from(config(policy)).failureRateThreshold(1)
            .slowCallRateThreshold(5).build();
        assertThat(evaluate(1, 0, config, false)).isEqualTo(Result.BELOW_THRESHOLDS);
        assertThat(evaluate(1, 0, config, true)).isEqualTo(Result.RECOVERY_HOLD);
        assertThat(evaluate(0, 5, config, false)).isEqualTo(Result.BELOW_THRESHOLDS);
        assertThat(evaluate(0, 5, config, true)).isEqualTo(Result.RECOVERY_HOLD);
        assertThat(evaluate(0, 4, config, true)).isEqualTo(Result.BELOW_THRESHOLDS);
        assertThat(evaluate(20, 0, config, true)).isEqualTo(Result.FAILURE_RATE_ABOVE_THRESHOLDS);
        assertThat(evaluate(0, 50, config, true)).isEqualTo(Result.SLOW_CALL_RATE_ABOVE_THRESHOLDS);
    }

    private static Result evaluate(int failures, int slowCalls) {
        var policy = HalfOpenRecoveryConfig.custom().minimumNumberOfCalls(100).build();
        return evaluate(failures, slowCalls, config(policy), false);
    }

    private static Result evaluate(int failures, int slowCalls, CircuitBreakerConfig config,
        boolean finalPhase) {
        var metrics = CircuitBreakerMetrics.forHalfOpenRecovery(100, config, finalPhase);
        Result result = null;
        for (int call = 0; call < 100; call++) {
            long duration = call < slowCalls ? 61 : 0;
            result = call < failures ? metrics.onError(duration, TimeUnit.SECONDS)
                : metrics.onSuccess(duration, TimeUnit.SECONDS);
        }
        return result;
    }

    private static CircuitBreakerConfig config(HalfOpenRecoveryConfig policy) {
        return CircuitBreakerConfig.custom().failureRateThreshold(50).slowCallRateThreshold(80)
            .minimumNumberOfCalls(100).slidingWindowSize(100).halfOpenRecoveryConfig(policy).build();
    }
}
