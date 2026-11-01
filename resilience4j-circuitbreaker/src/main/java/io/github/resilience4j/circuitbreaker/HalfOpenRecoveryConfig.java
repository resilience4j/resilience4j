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

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

/**
 * Opt-in recovery which admits a growing percentage of traffic while HALF_OPEN.
 * Each phase requires both its minimum duration and a healthy window of recorded
 * outcomes. Rejected attempts are not health samples. This does not limit request
 * rate or simultaneous calls; a Bulkhead or RateLimiter can provide those limits.
 */
public final class HalfOpenRecoveryConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Response to an unhealthy evaluation or a rejected final 100-percent validation. */
    public enum FailureStrategy {
        /** Stop admitting calls and restart the normal OPEN wait interval. */
        OPEN,
        /** Retry the preceding phase with a fresh window; reopen if already in the first phase. */
        STEP_BACK
    }

    private final int initialTrafficPercentage;
    private final int trafficPercentageStep;
    private final Duration minimumPhaseDuration;
    private final int minimumNumberOfCalls;
    private final FailureStrategy failureStrategy;
    private final float maxFailureRateForAdvance;
    private final float failureRateThreshold;
    private final float maxSlowCallRateForAdvance;
    private final float slowCallRateThreshold;

    private HalfOpenRecoveryConfig(Builder builder) {
        initialTrafficPercentage = builder.initialTrafficPercentage;
        trafficPercentageStep = builder.trafficPercentageStep;
        minimumPhaseDuration = builder.minimumPhaseDuration;
        minimumNumberOfCalls = builder.minimumNumberOfCalls;
        failureStrategy = builder.failureStrategy;
        maxFailureRateForAdvance = builder.maxFailureRateForAdvance;
        failureRateThreshold = builder.failureRateThreshold;
        maxSlowCallRateForAdvance = builder.maxSlowCallRateForAdvance;
        slowCallRateThreshold = builder.slowCallRateThreshold;
    }

    /**
     * Creates a builder with automatic initial traffic, five-point increments, a ten-second
     * minimum phase duration, ten recorded outcomes and {@link FailureStrategy#OPEN}.
     * Advancement allows at most 5% failures and 10% slow calls; regression starts
     * at 20% failures or 50% slow calls. Intermediate health retains phases below 100%.
     * The final phase must pass validation or use the failure strategy once its duration
     * and sample requirements are met; closing also requires the ordinary health bounds.
     *
     * @return a new builder
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Copies a policy, retaining whether the initial percentage was automatic or explicit.
     *
     * @param config the policy to copy
     * @return an independent builder
     * @throws NullPointerException if the policy is null
     */
    public static Builder from(HalfOpenRecoveryConfig config) {
        return new Builder(Objects.requireNonNull(config, "Recovery config must not be null"));
    }

    /**
     * Returns the effective initial percentage; automatic mode uses the configured step.
     *
     * @return the first recovery target from 1 to 100
     */
    public int getInitialTrafficPercentage() {
        return isInitialTrafficPercentageAutomatic() ? trafficPercentageStep : initialTrafficPercentage;
    }

    /**
     * Returns whether the first recovery percentage is derived from the step.
     *
     * @return true when the configured initial percentage is zero
     */
    public boolean isInitialTrafficPercentageAutomatic() {
        return initialTrafficPercentage == 0;
    }

    /**
     * Returns the increment in percentage points between recovery phases.
     *
     * @return the configured step from 1 to 100
     */
    public int getTrafficPercentageStep() {
        return trafficPercentageStep;
    }

    /**
     * Returns the minimum time a healthy phase must remain active before advancing.
     * A newly recorded outcome must validate the phase after this time has elapsed;
     * elapsed time and an admission attempt alone cannot advance it. An unhealthy
     * evaluation may reopen or step back sooner. The final 100-percent phase is
     * accepted or rejected at a recorded completion once both requirements hold.
     *
     * @return the positive minimum phase duration
     */
    public Duration getMinimumPhaseDuration() {
        return minimumPhaseDuration;
    }

    /**
     * Returns the number of recorded completions in each phase's rolling health window.
     * Rejected attempts and ignored outcomes do not provide samples.
     *
     * @return the positive number of samples required for evaluation
     */
    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    /**
     * Returns the response to an unhealthy phase or rejected final validation.
     *
     * @return the configured failure strategy
     */
    public FailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    /**
     * Returns the inclusive maximum failure percentage permitting advancement.
     * The complete recovery window and minimum phase duration are also required.
     *
     * @return the advancement failure percentage, default 5
     */
    public float getMaxFailureRateForAdvance() {
        return maxFailureRateForAdvance;
    }

    /**
     * Returns the inclusive failure percentage triggering the failure strategy.
     * Values between the advancement maximum and this threshold keep the current phase.
     *
     * @return the regression failure percentage, default 20
     */
    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    /**
     * Returns the inclusive maximum slow-call percentage permitting advancement.
     * Slow calls use the enclosing CircuitBreakerConfig's duration threshold.
     *
     * @return the advancement slow-call percentage, default 10
     */
    public float getMaxSlowCallRateForAdvance() {
        return maxSlowCallRateForAdvance;
    }

    /**
     * Returns the inclusive slow-call percentage triggering the failure strategy.
     * Values between the advancement maximum and this threshold keep the current phase.
     *
     * @return the regression slow-call percentage, default 50
     */
    public float getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    /** Builds an immutable percentage-based HALF_OPEN recovery policy. */
    public static final class Builder {
        private int initialTrafficPercentage = 0;
        private int trafficPercentageStep = 5;
        private Duration minimumPhaseDuration = Duration.ofSeconds(10);
        private int minimumNumberOfCalls = 10;
        private FailureStrategy failureStrategy = FailureStrategy.OPEN;
        private float maxFailureRateForAdvance = 5;
        private float failureRateThreshold = 20;
        private float maxSlowCallRateForAdvance = 10;
        private float slowCallRateThreshold = 50;

        private Builder() {
        }

        private Builder(HalfOpenRecoveryConfig base) {
            initialTrafficPercentage = base.initialTrafficPercentage;
            trafficPercentageStep = base.trafficPercentageStep;
            minimumPhaseDuration = base.minimumPhaseDuration;
            minimumNumberOfCalls = base.minimumNumberOfCalls;
            failureStrategy = base.failureStrategy;
            maxFailureRateForAdvance = base.maxFailureRateForAdvance;
            failureRateThreshold = base.failureRateThreshold;
            maxSlowCallRateForAdvance = base.maxSlowCallRateForAdvance;
            slowCallRateThreshold = base.slowCallRateThreshold;
        }

        /**
         * Configures an optional initial percentage. Zero (the default) selects
         * automatic mode: the first HALF_OPEN phase uses the configured step.
         * Zero is never an actual recovery phase and does not replace OPEN.
         *
         * @param percentage zero for automatic mode, or an explicit value from 1 to 100
         * @return this builder
         * @throws IllegalArgumentException if the percentage is outside 0 to 100
         */
        public Builder initialTrafficPercentage(int percentage) {
            checkPercentage(percentage, "initialTrafficPercentage", 0);
            initialTrafficPercentage = percentage;
            return this;
        }

        /**
         * Sets the increment in percentage points (1..100, default 5). The last target is
         * capped at 100; an out-of-range configured step is rejected rather than capped.
         * In automatic initial mode this also determines the first target.
         *
         * @param step the positive increment in percentage points
         * @return this builder
         * @throws IllegalArgumentException if the step is outside 1..100
         */
        public Builder trafficPercentageStep(int step) {
            checkPercentage(step, "trafficPercentageStep", 1);
            trafficPercentageStep = step;
            return this;
        }

        /**
         * Sets the positive minimum time before a healthy phase may advance (default ten
         * seconds). Time and a sufficiently large healthy sample are both required.
         * An unhealthy evaluation may regress or reopen before this duration elapses.
         *
         * @param duration the minimum duration of each phase
         * @return this builder
         * @throws IllegalArgumentException if the duration is zero or negative
         * @throws NullPointerException if the duration is null
         */
        public Builder minimumPhaseDuration(Duration duration) {
            Objects.requireNonNull(duration, "minimumPhaseDuration must not be null");
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("minimumPhaseDuration must be greater than zero");
            }
            minimumPhaseDuration = duration;
            return this;
        }

        /**
         * Sets the positive size of each phase's rolling outcome window (default ten).
         * Ignored outcomes and refused attempts do not supply samples. This setting
         * is independent of the ordinary CLOSED/HALF_OPEN minimum and finite probe quota.
         *
         * @param calls the number of recorded completions required for evaluation
         * @return this builder
         * @throws IllegalArgumentException if fewer than one sample is requested
         */
        public Builder minimumNumberOfCalls(int calls) {
            if (calls < 1) {
                throw new IllegalArgumentException("minimumNumberOfCalls must be greater than zero");
            }
            minimumNumberOfCalls = calls;
            return this;
        }

        /**
         * Selects the response to an unhealthy phase or rejected final validation
         * (default {@link FailureStrategy#OPEN}).
         * STEP_BACK restarts the preceding phase's duration and outcome window without
         * extending the maximum duration of the complete recovery episode.
         *
         * @param strategy the non-null failure strategy
         * @return this builder
         * @throws NullPointerException if the strategy is null
         */
        public Builder failureStrategy(FailureStrategy strategy) {
            failureStrategy = Objects.requireNonNull(strategy, "failureStrategy must not be null");
            return this;
        }

        /**
         * Sets the inclusive maximum failure percentage permitting advancement (default 5).
         * Zero requires a recorded window without failures.
         * This setting applies only to gradual recovery; ordinary state thresholds are unchanged.
         *
         * @param percentage a finite percentage from 0 to 100
         * @return this builder
         * @throws IllegalArgumentException if outside the range; ordering is checked by build
         */
        public Builder maxFailureRateForAdvance(float percentage) {
            checkRate(percentage, "maxFailureRateForAdvance", true);
            maxFailureRateForAdvance = percentage;
            return this;
        }

        /**
         * Sets the inclusive failure percentage triggering the failure strategy (default 20).
         * This threshold must be greater than its advancement maximum.
         * This setting applies only to gradual recovery; ordinary state thresholds are unchanged.
         *
         * @param percentage a finite percentage greater than zero and at most 100
         * @return this builder
         * @throws IllegalArgumentException if outside the range; ordering is checked by build
         */
        public Builder failureRateThreshold(float percentage) {
            checkRate(percentage, "failureRateThreshold", false);
            failureRateThreshold = percentage;
            return this;
        }

        /**
         * Sets the inclusive maximum slow-call percentage permitting advancement (default 10).
         * Zero requires a recorded window without slow-calls.
         * This setting applies only to gradual recovery; ordinary state thresholds are unchanged.
         *
         * @param percentage a finite percentage from 0 to 100
         * @return this builder
         * @throws IllegalArgumentException if outside the range; ordering is checked by build
         */
        public Builder maxSlowCallRateForAdvance(float percentage) {
            checkRate(percentage, "maxSlowCallRateForAdvance", true);
            maxSlowCallRateForAdvance = percentage;
            return this;
        }

        /**
         * Sets the inclusive slow-call percentage triggering the failure strategy (default 50).
         * This threshold must be greater than its advancement maximum.
         * This setting applies only to gradual recovery; ordinary state thresholds are unchanged.
         *
         * @param percentage a finite percentage greater than zero and at most 100
         * @return this builder
         * @throws IllegalArgumentException if outside the range; ordering is checked by build
         */
        public Builder slowCallRateThreshold(float percentage) {
            checkRate(percentage, "slowCallRateThreshold", false);
            slowCallRateThreshold = percentage;
            return this;
        }

        /**
         * Creates an immutable policy. The 100-percent phase requires validation against
         * both recovery advancement and ordinary health bounds before closing. A complete
         * sample which fails validation after the minimum duration uses the failure strategy.
         * Evaluation remains completion-driven; the global HALF_OPEN timeout is independent.
         *
         * @return the configured recovery policy
         */
        public HalfOpenRecoveryConfig build() {
            if (maxFailureRateForAdvance >= failureRateThreshold) {
                throw new IllegalArgumentException(
                    "maxFailureRateForAdvance must be less than failureRateThreshold");
            }
            if (maxSlowCallRateForAdvance >= slowCallRateThreshold) {
                throw new IllegalArgumentException(
                    "maxSlowCallRateForAdvance must be less than slowCallRateThreshold");
            }
            return new HalfOpenRecoveryConfig(this);
        }

        private static void checkRate(float percentage, String name, boolean allowZero) {
            if (!Float.isFinite(percentage) || percentage < 0 || percentage > 100
                || !allowZero && percentage == 0) {
                throw new IllegalArgumentException(name + " must be "
                    + (allowZero ? "between 0 and 100" : "greater than zero and at most 100"));
            }
        }

        private static void checkPercentage(int percentage, String name, int minimum) {
            if (percentage < minimum || percentage > 100) {
                throw new IllegalArgumentException(name + " must be between " + minimum + " and 100");
            }
        }
    }
}
