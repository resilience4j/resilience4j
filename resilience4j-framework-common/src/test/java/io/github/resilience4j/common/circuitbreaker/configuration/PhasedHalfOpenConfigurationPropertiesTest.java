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

import java.time.Duration;
import java.util.Collections;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.github.resilience4j.common.CompositeCustomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhasedHalfOpenConfigurationPropertiesTest {

    @ParameterizedTest
    @ValueSource(ints = {30, 45, 100})
    void omittedInitialPercentageUsesTheStepAcrossSharedConfiguration(int step) {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery().setInitialTrafficPercentage(null);
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(step));

        var recovery = config(properties, instance).getHalfOpenRecoveryConfig();

        assertThat(recovery.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(step);
        assertThat(properties.getConfigs().get("phased").getHalfOpenRecovery().getInitialTrafficPercentage())
            .isNull();
    }

    @Test
    void childCanRestoreAutomaticInitializationFromAnExplicitBase() {
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties()
                .setInitialTrafficPercentage(0).setTrafficPercentageStep(45));

        var recovery = config(configuredProperties(), instance).getHalfOpenRecoveryConfig();

        assertThat(recovery.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(45);
    }

    @Test
    void automaticModeSurvivesMultipleSharedConfigurationLevels() {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery().setInitialTrafficPercentage(0);
        properties.getConfigs()
            .put("middle", new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(30)));
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("middle")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(45));

        var recovery = config(properties, instance).getHalfOpenRecoveryConfig();

        assertThat(recovery.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(45);
        assertThat(properties.getConfigs().get("middle").getHalfOpenRecovery().getInitialTrafficPercentage())
            .isNull();
    }

    @Test
    void automaticModeFromBaseCustomizerTracksAnInstanceStepOverride() {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery().setInitialTrafficPercentage(null);
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(45));
        var customizer = CircuitBreakerConfigCustomizer.of("phased",
            builder -> builder.halfOpenRecoveryConfig(
                HalfOpenRecoveryConfig.custom().trafficPercentageStep(30).build()));

        var recovery = properties.createCircuitBreakerConfig("backend", instance,
            new CompositeCustomizer<>(Collections.singletonList(customizer))).getHalfOpenRecoveryConfig();

        assertThat(recovery.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(45);
    }

    @Test
    void enablingADisabledAutomaticBaseUsesTheInstanceStep() {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery()
            .setEnabled(false).setInitialTrafficPercentage(0);
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties()
            .setEnabled(true)
            .setTrafficPercentageStep(30));

        var recovery = config(properties, instance).getHalfOpenRecoveryConfig();

        assertThat(recovery.isInitialTrafficPercentageAutomatic()).isTrue();
        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(30);
        assertThat(properties.getConfigs().get("phased").getHalfOpenRecovery().getEnabled()).isFalse();
    }

    @Test
    void stepAboveOneHundredFailsRatherThanBeingClamped() {
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(120));

        assertThatThrownBy(() -> config(configuredProperties(), instance))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("trafficPercentageStep must be between 1 and 100");
    }

    @Test
    void inheritsPhaseSettingsFromSharedConfiguration() {
        CommonCircuitBreakerConfigurationProperties properties = configuredProperties();
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased");

        CircuitBreakerConfig config = config(properties, instance);

        assertThat(config.getHalfOpenRecoveryConfig().getTrafficPercentageStep()).isEqualTo(5);
        assertThat(config.getHalfOpenRecoveryConfig().getFailureStrategy())
            .isEqualTo(HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK);
        assertThat(config.getHalfOpenRecoveryConfig().getMinimumPhaseDuration())
            .isEqualTo(Duration.ofSeconds(2));
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(12);
    }

    @Test
    void instanceCanExplicitlyDisableInheritedPhasesAndFailurePolicy() {
        CommonCircuitBreakerConfigurationProperties properties = configuredProperties();
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setEnabled(false));

        CircuitBreakerConfig config = config(properties, instance);

        assertThat(config.getHalfOpenRecoveryConfig()).isNull();
        assertThat(properties.getConfigs().get("phased").getHalfOpenRecovery().getEnabled()).isTrue();
    }

    @Test
    void partialInstanceOverrideRetainsInheritedSettings() {
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(10));

        var recovery = config(configuredProperties(), instance).getHalfOpenRecoveryConfig();

        assertThat(recovery.getTrafficPercentageStep()).isEqualTo(10);
        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(5);
        assertThat(recovery.isInitialTrafficPercentageAutomatic()).isFalse();
        assertThat(recovery.getMinimumPhaseDuration()).isEqualTo(Duration.ofSeconds(2));
        assertThat(recovery.getFailureStrategy()).isEqualTo(HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK);
    }

    @Test
    void partialOverrideKeepsDisabledBaseDisabled() {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery().setEnabled(false);
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(10));

        assertThat(config(properties, instance).getHalfOpenRecoveryConfig()).isNull();
    }

    @Test
    void explicitlyEnablingDisabledBaseRetainsItsSettings() {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery()
            .setEnabled(false).setInitialTrafficPercentage(25);
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setEnabled(true));

        var recovery = config(properties, instance).getHalfOpenRecoveryConfig();

        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(25);
        assertThat(recovery.getMinimumPhaseDuration()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getConfigs().get("phased").getHalfOpenRecovery().getEnabled()).isFalse();
    }

    @Test
    void resolvesDisabledRecoveryAcrossMultipleSharedConfigurationLevels() {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery()
            .setEnabled(false).setInitialTrafficPercentage(25);
        properties.getConfigs()
            .put("middle", new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(10)));
        var disabled = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("middle");

        assertThat(config(properties, disabled).getHalfOpenRecoveryConfig()).isNull();

        var enabled = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("middle")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setEnabled(true));
        var recovery = config(properties, enabled).getHalfOpenRecoveryConfig();

        assertThat(recovery.getInitialTrafficPercentage()).isEqualTo(25);
        assertThat(recovery.getTrafficPercentageStep()).isEqualTo(10);
        assertThat(recovery.getMinimumPhaseDuration()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void inheritsEffectiveRecoveryValuesFromBaseCustomizer() {
        var properties = configuredProperties();
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased");
        var customizer = CircuitBreakerConfigCustomizer.of("phased",
            builder -> builder.halfOpenRecoveryConfig(
                HalfOpenRecoveryConfig.custom().initialTrafficPercentage(25).build()));

        var config = properties.createCircuitBreakerConfig("backend", instance,
            new CompositeCustomizer<>(Collections.singletonList(customizer)));

        assertThat(config.getHalfOpenRecoveryConfig().getInitialTrafficPercentage()).isEqualTo(25);
    }

    @Test
    void rejectsInvalidPhaseSettingsWhenBuildingConfiguration() {
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setTrafficPercentageStep(0));

        assertThatThrownBy(() -> config(configuredProperties(), instance))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recoveryHealthSettingsInheritAndOverrideIndependentlyOfGeneralThresholds() {
        var properties = configuredProperties();
        properties.getConfigs().get("phased").getHalfOpenRecovery()
            .setMaxFailureRateForAdvance(2f).setFailureRateThreshold(15f)
            .setMaxSlowCallRateForAdvance(8f).setSlowCallRateThreshold(30f);
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased").setFailureRateThreshold(75f)
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setMaxFailureRateForAdvance(0f));

        var config = config(properties, instance);
        var recovery = config.getHalfOpenRecoveryConfig();
        assertThat(recovery.getMaxFailureRateForAdvance()).isZero();
        assertThat(recovery.getFailureRateThreshold()).isEqualTo(15);
        assertThat(recovery.getMaxSlowCallRateForAdvance()).isEqualTo(8);
        assertThat(recovery.getSlowCallRateThreshold()).isEqualTo(30);
        assertThat(config.getFailureRateThreshold()).isEqualTo(75);
        assertThat(properties.getConfigs().get("phased").getHalfOpenRecovery()
            .getMaxFailureRateForAdvance()).isEqualTo(2);
    }

    @Test
    void recoveryHealthBoundsFromCustomizerSurviveInstanceOverrides() {
        var properties = configuredProperties();
        var instance = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setBaseConfig("phased")
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties().setMaxFailureRateForAdvance(1f));
        var customizer = CircuitBreakerConfigCustomizer.of("phased", builder -> builder
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom().failureRateThreshold(30)
                .maxSlowCallRateForAdvance(4).slowCallRateThreshold(40).build()));

        var recovery = properties.createCircuitBreakerConfig("backend", instance,
            new CompositeCustomizer<>(Collections.singletonList(customizer))).getHalfOpenRecoveryConfig();
        assertThat(recovery.getMaxFailureRateForAdvance()).isEqualTo(1);
        assertThat(recovery.getFailureRateThreshold()).isEqualTo(30);
        assertThat(recovery.getMaxSlowCallRateForAdvance()).isEqualTo(4);
        assertThat(recovery.getSlowCallRateThreshold()).isEqualTo(40);
    }

    private static CommonCircuitBreakerConfigurationProperties configuredProperties() {
        var properties = new CommonCircuitBreakerConfigurationProperties();
        var base = new CommonCircuitBreakerConfigurationProperties.InstanceProperties()
            .setPermittedNumberOfCallsInHalfOpenState(12)
            .setHalfOpenRecovery(new HalfOpenRecoveryProperties()
                .setEnabled(true)
                .setInitialTrafficPercentage(5)
                .setTrafficPercentageStep(5)
                .setMinimumPhaseDuration(Duration.ofSeconds(2))
                .setFailureStrategy(HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK));
        properties.getConfigs().put("phased", base);
        return properties;
    }

    private static CircuitBreakerConfig config(CommonCircuitBreakerConfigurationProperties properties,
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instance) {
        return properties.createCircuitBreakerConfig("backend", instance,
            new CompositeCustomizer<>(Collections.emptyList()));
    }
}
