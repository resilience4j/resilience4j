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
package io.github.resilience4j.common.circuitbreaker.configuration;

import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.github.resilience4j.core.lang.Nullable;

import java.time.Duration;

/**
 * Optional half-open traffic recovery settings. Unset values inherit the shared
 * configuration; {@code enabled=false} explicitly disables inherited recovery.
 * An initial traffic percentage of zero selects automatic initialization at the step.
 */
public class HalfOpenRecoveryProperties {

    @Nullable
    private Boolean enabled;

    @Nullable
    private Integer initialTrafficPercentage;

    @Nullable
    private Integer trafficPercentageStep;

    @Nullable
    private Duration minimumPhaseDuration;

    @Nullable
    private Integer minimumNumberOfCalls;

    @Nullable
    private HalfOpenRecoveryConfig.FailureStrategy failureStrategy;

    @Nullable
    private Float maxFailureRateForAdvance;

    @Nullable
    private Float failureRateThreshold;

    @Nullable
    private Float maxSlowCallRateForAdvance;

    @Nullable
    private Float slowCallRateThreshold;

    /**
     * Returns this configuration layer's enablement setting.
     *
     * @return true to enable, false to disable, or null to inherit the setting
     */
    @Nullable
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether this layer enables or explicitly disables phased recovery.
     *
     * @param enabled true to enable, false to disable, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Returns this layer's initial traffic setting, before inheritance and validation.
     *
     * @return zero for automatic initialization, an explicit percentage, or null to inherit
     */
    @Nullable
    public Integer getInitialTrafficPercentage() {
        return initialTrafficPercentage;
    }

    /**
     * Sets the initial percentage. Validation occurs when the policy is built.
     *
     * @param percentage zero to use the step, a value from 1 to 100, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setInitialTrafficPercentage(Integer percentage) {
        this.initialTrafficPercentage = percentage;
        return this;
    }

    /**
     * Returns this layer's increment in percentage points, before inheritance and validation.
     *
     * @return the configured step, or null to inherit
     */
    @Nullable
    public Integer getTrafficPercentageStep() {
        return trafficPercentageStep;
    }

    /**
     * Sets the increment in percentage points. Validation occurs when the policy is built.
     *
     * @param step a value from 1 to 100, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setTrafficPercentageStep(Integer step) {
        this.trafficPercentageStep = step;
        return this;
    }

    /**
     * Returns this layer's minimum healthy phase duration, before inheritance and validation.
     *
     * @return the configured duration, or null to inherit
     */
    @Nullable
    public Duration getMinimumPhaseDuration() {
        return minimumPhaseDuration;
    }

    /**
     * Sets the minimum time before a healthy phase may advance. Validation occurs
     * when the policy is built.
     *
     * @param duration a positive duration, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setMinimumPhaseDuration(Duration duration) {
        this.minimumPhaseDuration = duration;
        return this;
    }

    /**
     * Returns this layer's phase health-window size, before inheritance and validation.
     *
     * @return the configured sample count, or null to inherit
     */
    @Nullable
    public Integer getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    /**
     * Sets the number of recorded completions required for phase evaluation.
     * Validation occurs when the policy is built.
     *
     * @param minimumNumberOfCalls a positive sample count, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setMinimumNumberOfCalls(Integer minimumNumberOfCalls) {
        this.minimumNumberOfCalls = minimumNumberOfCalls;
        return this;
    }

    /**
     * Returns this layer's response to an unhealthy phase.
     *
     * @return the configured failure strategy, or null to inherit
     */
    @Nullable
    public HalfOpenRecoveryConfig.FailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    /**
     * Sets the response to an unhealthy phase evaluation.
     *
     * @param strategy the failure strategy, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setFailureStrategy(
        HalfOpenRecoveryConfig.FailureStrategy strategy) {
        this.failureStrategy = strategy;
        return this;
    }

    /**
     * Returns this layer's maxFailureRateForAdvance recovery setting.
     *
     * @return the percentage, or null to inherit
     */
    @Nullable
    public Float getMaxFailureRateForAdvance() {
        return maxFailureRateForAdvance;
    }

    /**
     * Sets maxFailureRateForAdvance for gradual recovery. Validation occurs when the policy is built.
     *
     * @param percentage the percentage, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setMaxFailureRateForAdvance(Float percentage) {
        this.maxFailureRateForAdvance = percentage;
        return this;
    }

    /**
     * Returns this layer's failureRateThreshold recovery setting.
     *
     * @return the percentage, or null to inherit
     */
    @Nullable
    public Float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    /**
     * Sets failureRateThreshold for gradual recovery. Validation occurs when the policy is built.
     *
     * @param percentage the percentage, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setFailureRateThreshold(Float percentage) {
        this.failureRateThreshold = percentage;
        return this;
    }

    /**
     * Returns this layer's maxSlowCallRateForAdvance recovery setting.
     *
     * @return the percentage, or null to inherit
     */
    @Nullable
    public Float getMaxSlowCallRateForAdvance() {
        return maxSlowCallRateForAdvance;
    }

    /**
     * Sets maxSlowCallRateForAdvance for gradual recovery. Validation occurs when the policy is built.
     *
     * @param percentage the percentage, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setMaxSlowCallRateForAdvance(Float percentage) {
        this.maxSlowCallRateForAdvance = percentage;
        return this;
    }

    /**
     * Returns this layer's slowCallRateThreshold recovery setting.
     *
     * @return the percentage, or null to inherit
     */
    @Nullable
    public Float getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    /**
     * Sets slowCallRateThreshold for gradual recovery. Validation occurs when the policy is built.
     *
     * @param percentage the percentage, or null to inherit
     * @return these properties
     */
    public HalfOpenRecoveryProperties setSlowCallRateThreshold(Float percentage) {
        this.slowCallRateThreshold = percentage;
        return this;
    }

    @Nullable
    static HalfOpenRecoveryProperties merge(@Nullable HalfOpenRecoveryProperties instance,
        @Nullable HalfOpenRecoveryProperties inherited) {
        if (inherited == null) {
            return instance;
        }
        HalfOpenRecoveryProperties merged = new HalfOpenRecoveryProperties();
        merged.enabled = instance != null && instance.enabled != null
            ? instance.enabled : inherited.enabled;
        merged.initialTrafficPercentage = instance != null && instance.initialTrafficPercentage != null
            ? instance.initialTrafficPercentage : inherited.initialTrafficPercentage;
        merged.trafficPercentageStep = instance != null && instance.trafficPercentageStep != null
            ? instance.trafficPercentageStep : inherited.trafficPercentageStep;
        merged.minimumPhaseDuration = instance != null && instance.minimumPhaseDuration != null
            ? instance.minimumPhaseDuration : inherited.minimumPhaseDuration;
        merged.minimumNumberOfCalls = instance != null && instance.minimumNumberOfCalls != null
            ? instance.minimumNumberOfCalls : inherited.minimumNumberOfCalls;
        merged.failureStrategy = instance != null && instance.failureStrategy != null
            ? instance.failureStrategy : inherited.failureStrategy;
        merged.maxFailureRateForAdvance = instance != null && instance.maxFailureRateForAdvance != null
            ? instance.maxFailureRateForAdvance : inherited.maxFailureRateForAdvance;
        merged.failureRateThreshold = instance != null && instance.failureRateThreshold != null
            ? instance.failureRateThreshold : inherited.failureRateThreshold;
        merged.maxSlowCallRateForAdvance = instance != null && instance.maxSlowCallRateForAdvance != null
            ? instance.maxSlowCallRateForAdvance : inherited.maxSlowCallRateForAdvance;
        merged.slowCallRateThreshold = instance != null && instance.slowCallRateThreshold != null
            ? instance.slowCallRateThreshold : inherited.slowCallRateThreshold;
        return merged;
    }

    static HalfOpenRecoveryProperties fromConfig(HalfOpenRecoveryConfig config) {
        return new HalfOpenRecoveryProperties().setEnabled(true)
            .setInitialTrafficPercentage(config.isInitialTrafficPercentageAutomatic()
                ? 0 : config.getInitialTrafficPercentage())
            .setTrafficPercentageStep(config.getTrafficPercentageStep())
            .setMinimumPhaseDuration(config.getMinimumPhaseDuration())
            .setMinimumNumberOfCalls(config.getMinimumNumberOfCalls())
            .setFailureStrategy(config.getFailureStrategy())
            .setMaxFailureRateForAdvance(config.getMaxFailureRateForAdvance())
            .setFailureRateThreshold(config.getFailureRateThreshold())
            .setMaxSlowCallRateForAdvance(config.getMaxSlowCallRateForAdvance())
            .setSlowCallRateThreshold(config.getSlowCallRateThreshold());
    }

    @Nullable
    HalfOpenRecoveryConfig toConfig(@Nullable HalfOpenRecoveryConfig inherited) {
        if (Boolean.FALSE.equals(enabled)) {
            return null;
        }
        HalfOpenRecoveryConfig.Builder builder = inherited == null
            ? HalfOpenRecoveryConfig.custom() : HalfOpenRecoveryConfig.from(inherited);
        if (initialTrafficPercentage != null) {
            builder.initialTrafficPercentage(initialTrafficPercentage);
        }
        if (trafficPercentageStep != null) {
            builder.trafficPercentageStep(trafficPercentageStep);
        }
        if (minimumPhaseDuration != null) {
            builder.minimumPhaseDuration(minimumPhaseDuration);
        }
        if (minimumNumberOfCalls != null) {
            builder.minimumNumberOfCalls(minimumNumberOfCalls);
        }
        if (failureStrategy != null) {
            builder.failureStrategy(failureStrategy);
        }
        if (maxFailureRateForAdvance != null) {
            builder.maxFailureRateForAdvance(maxFailureRateForAdvance);
        }
        if (failureRateThreshold != null) {
            builder.failureRateThreshold(failureRateThreshold);
        }
        if (maxSlowCallRateForAdvance != null) {
            builder.maxSlowCallRateForAdvance(maxSlowCallRateForAdvance);
        }
        if (slowCallRateThreshold != null) {
            builder.slowCallRateThreshold(slowCallRateThreshold);
        }
        return builder.build();
    }
}
