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
package io.github.resilience4j.circuitbreaker;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HalfOpenRecoveryConfigTest {
    @Test
    void defaultsAndFromPreserveAnImmutableRecoveryPolicy() {
        HalfOpenRecoveryConfig base = HalfOpenRecoveryConfig.custom().build();

        assertThat(base.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(base.getInitialTrafficPercentage()).isEqualTo(5);
        assertThat(base.getTrafficPercentageStep()).isEqualTo(5);
        assertThat(base.getMinimumPhaseDuration()).isEqualTo(Duration.ofSeconds(10));
        assertThat(base.getMinimumNumberOfCalls()).isEqualTo(10);
        assertThat(base.getFailureStrategy()).isEqualTo(HalfOpenRecoveryConfig.FailureStrategy.OPEN);
        HalfOpenRecoveryConfig override = HalfOpenRecoveryConfig.from(base)
            .failureStrategy(STEP_BACK).build();
        assertThat(override.getInitialTrafficPercentage()).isEqualTo(5);
        assertThat(override.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(override.getMinimumPhaseDuration()).isEqualTo(Duration.ofSeconds(10));
        assertThat(override.getFailureStrategy()).isEqualTo(STEP_BACK);
        assertThat(base.getFailureStrategy()).isEqualTo(HalfOpenRecoveryConfig.FailureStrategy.OPEN);
    }

    @Test
    void healthBoundsHaveIndependentDefaultsAndSurviveCopies() {
        HalfOpenRecoveryConfig base = HalfOpenRecoveryConfig.custom().build();
        assertThat(base.getMaxFailureRateForAdvance()).isEqualTo(5);
        assertThat(base.getFailureRateThreshold()).isEqualTo(20);
        assertThat(base.getMaxSlowCallRateForAdvance()).isEqualTo(10);
        assertThat(base.getSlowCallRateThreshold()).isEqualTo(50);

        HalfOpenRecoveryConfig changed = HalfOpenRecoveryConfig.from(base)
            .maxFailureRateForAdvance(0).failureRateThreshold(10)
            .maxSlowCallRateForAdvance(2.5f).slowCallRateThreshold(25).build();
        HalfOpenRecoveryConfig copy = HalfOpenRecoveryConfig.from(changed).build();
        assertThat(copy.getMaxFailureRateForAdvance()).isZero();
        assertThat(copy.getFailureRateThreshold()).isEqualTo(10);
        assertThat(copy.getMaxSlowCallRateForAdvance()).isEqualTo(2.5f);
        assertThat(copy.getSlowCallRateThreshold()).isEqualTo(25);
        assertThat(base.getFailureRateThreshold()).isEqualTo(20);
    }

    @Test
    void healthBoundsRejectNonFiniteOutOfRangeAndOverlappingValues() {
        for (float invalid : new float[]{Float.NaN, Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY, -1, 101}) {
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().maxFailureRateForAdvance(invalid))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().failureRateThreshold(invalid))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().maxSlowCallRateForAdvance(invalid))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().slowCallRateThreshold(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().failureRateThreshold(0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().slowCallRateThreshold(0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().maxFailureRateForAdvance(20).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxFailureRateForAdvance must be less");
        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().maxSlowCallRateForAdvance(51).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxSlowCallRateForAdvance must be less");
        assertThat(HalfOpenRecoveryConfig.custom().maxFailureRateForAdvance(0)
            .maxSlowCallRateForAdvance(0).build().getMaxFailureRateForAdvance()).isZero();
    }

    @Test
    void rejectsOutOfRangePercentagesWhileZeroSelectsAutomaticInitial() {
        for (int invalid : new int[]{Integer.MIN_VALUE, -1, 101, 120, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().initialTrafficPercentage(invalid))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().trafficPercentageStep(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        }

        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().trafficPercentageStep(0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(HalfOpenRecoveryConfig.custom().initialTrafficPercentage(0).build()
            .isInitialTrafficPercentageAutomatic()).isTrue();
    }

    @Test
    void automaticInitialUsesTheStepAndSurvivesConfigCopies() {
        for (int step : new int[]{5, 30, 45, 100}) {
            HalfOpenRecoveryConfig automatic = HalfOpenRecoveryConfig.custom()
                .trafficPercentageStep(step).build();
            assertThat(automatic.getInitialTrafficPercentage()).isEqualTo(step);
            assertThat(automatic.isInitialTrafficPercentageAutomatic()).isTrue();
            HalfOpenRecoveryConfig copied = HalfOpenRecoveryConfig.from(automatic)
                .trafficPercentageStep(17).build();
            assertThat(copied.getInitialTrafficPercentage()).isEqualTo(17);
            assertThat(copied.isInitialTrafficPercentageAutomatic()).isTrue();
            assertThat(automatic.getInitialTrafficPercentage()).isEqualTo(step);
        }
    }

    @Test
    void explicitInitialRemainsIndependentAndCanBeResetToAutomatic() {
        HalfOpenRecoveryConfig explicit = HalfOpenRecoveryConfig.custom()
            .initialTrafficPercentage(5).trafficPercentageStep(30).build();
        HalfOpenRecoveryConfig copied = HalfOpenRecoveryConfig.from(explicit)
            .trafficPercentageStep(45).build();
        assertThat(copied.isInitialTrafficPercentageAutomatic()).isFalse();
        assertThat(copied.getInitialTrafficPercentage()).isEqualTo(5);
        HalfOpenRecoveryConfig reset = HalfOpenRecoveryConfig.from(copied)
            .initialTrafficPercentage(0).build();
        assertThat(reset.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(reset.getInitialTrafficPercentage()).isEqualTo(45);
        assertThat(HalfOpenRecoveryConfig.custom().trafficPercentageStep(45)
            .initialTrafficPercentage(0).build().getInitialTrafficPercentage()).isEqualTo(45);
    }

    @Test
    void invalidStepFailsWithoutSilentlyReplacingTheConfiguredValue() {
        HalfOpenRecoveryConfig.Builder builder = HalfOpenRecoveryConfig.custom().trafficPercentageStep(30);

        assertThatThrownBy(() -> builder.trafficPercentageStep(120))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("trafficPercentageStep must be between 1 and 100");
        HalfOpenRecoveryConfig unchanged = builder.build();

        assertThat(unchanged.getTrafficPercentageStep()).isEqualTo(30);
        assertThat(unchanged.getInitialTrafficPercentage()).isEqualTo(30);
        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().initialTrafficPercentage(100)
            .trafficPercentageStep(120))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void phaseDurationAndMinimumSamplesMustBePositive() {
        for (Duration invalid : new Duration[]{Duration.ZERO, Duration.ofNanos(-1)}) {
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().minimumPhaseDuration(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        }
        for (int invalid : new int[]{-1, 0}) {
            assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().minimumNumberOfCalls(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        }

        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().minimumPhaseDuration(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HalfOpenRecoveryConfig.custom().failureStrategy(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parentCopyPreservesRecoveryAndNullExplicitlyDisablesIt() {
        HalfOpenRecoveryConfig policy = HalfOpenRecoveryConfig.custom().build();
        CircuitBreakerConfig enabled = CircuitBreakerConfig.custom().halfOpenRecoveryConfig(policy).build();

        assertThat(CircuitBreakerConfig.from(enabled).build().getHalfOpenRecoveryConfig()).isSameAs(policy);
        assertThat(CircuitBreakerConfig.from(enabled).halfOpenRecoveryConfig(null).build()
            .getHalfOpenRecoveryConfig()).isNull();
        assertThat(CircuitBreakerConfig.ofDefaults().getHalfOpenRecoveryConfig()).isNull();
    }
}
