/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.common.circuitbreaker.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.RecordFailurePredicate;
import io.github.resilience4j.common.RecordResultPredicate;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of circuit breaker registry
 */
public class CircuitBreakerConfigurationPropertiesTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateCircuitBreakerRegistry() {
        //Given
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties1 = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties1.setWaitDurationInOpenState(Duration.ofMillis(100));
        instanceProperties1.setEventConsumerBufferSize(100);
        instanceProperties1.setRegisterHealthIndicator(true);
        instanceProperties1.setAllowHealthIndicatorToFail(true);
        instanceProperties1.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        instanceProperties1.setSlidingWindowSize(200);
        instanceProperties1.setMinimumNumberOfCalls(10);
        instanceProperties1.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);
        instanceProperties1.setFailureRateThreshold(50f);
        instanceProperties1.setSlowCallDurationThreshold(Duration.ofSeconds(5));
        instanceProperties1.setMaxWaitDurationInHalfOpenState(Duration.ofSeconds(5));
        instanceProperties1.setSlowCallRateThreshold(50f);
        instanceProperties1.setPermittedNumberOfCallsInHalfOpenState(100);
        instanceProperties1.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        instanceProperties1.setWritableStackTraceEnabled(false);
        //noinspection unchecked
        instanceProperties1.setIgnoreExceptions(new Class[]{IllegalStateException.class});
        //noinspection unchecked
        instanceProperties1.setRecordExceptions(new Class[]{IllegalStateException.class});
        //noinspection unchecked
        instanceProperties1.setRecordFailurePredicate((Class) RecordFailurePredicate.class);
        instanceProperties1.setRecordResultPredicate((Class) RecordResultPredicate.class);
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties2 = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties2.setSlidingWindowSize(1337);

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        circuitBreakerConfigurationProperties.getInstances().put("backend2", instanceProperties2);

        //Then
        assertThat(circuitBreakerConfigurationProperties.getBackends().size()).isEqualTo(2);
        assertThat(circuitBreakerConfigurationProperties.getInstances()).hasSize(2);

        CircuitBreakerConfig circuitBreaker1 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backend1", instanceProperties1,
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker1).isNotNull();
        assertThat(circuitBreaker1.getSlidingWindowSize()).isEqualTo(200);
        assertThat(circuitBreaker1.getSlidingWindowType())
            .isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        assertThat(circuitBreaker1.getMinimumNumberOfCalls()).isEqualTo(10);
        assertThat(circuitBreaker1.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(100);
        assertThat(circuitBreaker1.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(circuitBreaker1.getSlowCallDurationThreshold().getSeconds()).isEqualTo(5);
        assertThat(circuitBreaker1.getMaxWaitDurationInHalfOpenState().getSeconds()).isEqualTo(5);
        assertThat(circuitBreaker1.getSlowCallRateThreshold()).isEqualTo(50f);
        assertThat(circuitBreaker1.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(100);
        assertThat(circuitBreaker1.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(circuitBreaker1.isWritableStackTraceEnabled()).isFalse();

        final CommonCircuitBreakerConfigurationProperties.InstanceProperties backend1 = circuitBreakerConfigurationProperties
            .getBackendProperties("backend1");
        assertThat(circuitBreakerConfigurationProperties.findCircuitBreakerProperties("backend1"))
            .isNotEmpty();
        assertThat(
            circuitBreakerConfigurationProperties.findCircuitBreakerProperties("backend1").get()
                .getRegisterHealthIndicator()).isTrue();
        assertThat(
            circuitBreakerConfigurationProperties.findCircuitBreakerProperties("backend1").get()
                .getAllowHealthIndicatorToFail()).isTrue();
        CircuitBreakerConfig circuitBreaker2 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backend2", instanceProperties2,
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker2).isNotNull();
        assertThat(circuitBreaker2.getSlidingWindowSize()).isEqualTo(1337);

    }


    @Test
    public void testCircuitBreakerIntervalFunctionProperties() {
        //Given
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties1 = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties1.setWaitDurationInOpenState(Duration.ofMillis(1000));
        instanceProperties1.setEnableExponentialBackoff(false);
        instanceProperties1.setEnableRandomizedWait(false);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties2 = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties2.setEnableExponentialBackoff(true);
        instanceProperties2.setExponentialBackoffMultiplier(1.0);
        instanceProperties2.setExponentialMaxWaitDurationInOpenState(Duration.ofMillis(99L));
        instanceProperties2.setWaitDurationInOpenState(Duration.ofMillis(100L));

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        circuitBreakerConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        Map<String, String> globalTagsForCircuitBreakers = new HashMap<>();
        globalTagsForCircuitBreakers.put("testKey1", "testKet2");
        circuitBreakerConfigurationProperties.setTags(globalTagsForCircuitBreakers);
        //Then
        assertThat(circuitBreakerConfigurationProperties.getInstances()).hasSize(2);
        assertThat(circuitBreakerConfigurationProperties.getTags()).isNotEmpty();
        assertThat(circuitBreakerConfigurationProperties.getBackends().size()).isEqualTo(2);
        final CircuitBreakerConfig circuitBreakerConfig1 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backend1", instanceProperties1,
                compositeCircuitBreakerCustomizer());
        final CircuitBreakerConfig circuitBreakerConfig2 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backend2", instanceProperties2,
                compositeCircuitBreakerCustomizer());
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instancePropertiesForRetry1 = circuitBreakerConfigurationProperties
            .getInstances().get("backend1");
        assertThat(instancePropertiesForRetry1.getWaitDurationInOpenState().toMillis())
            .isEqualTo(1000);
        assertThat(circuitBreakerConfig1).isNotNull();
        assertThat(circuitBreakerConfig1.getWaitIntervalFunctionInOpenState()).isNotNull();
        assertThat(circuitBreakerConfig2).isNotNull();
        assertThat(circuitBreakerConfig2.getWaitIntervalFunctionInOpenState()).isNotNull();
        assertThat(circuitBreakerConfig2.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(99L);
    }

    @Test
    public void testCreateCircuitBreakerRegistryWithSharedConfigs() {
        //Given
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlidingWindowSize(1000);
        defaultProperties.setPermittedNumberOfCallsInHalfOpenState(100);
        defaultProperties.setWaitDurationInOpenState(Duration.ofMillis(100));

        CommonCircuitBreakerConfigurationProperties.InstanceProperties sharedProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        sharedProperties.setSlidingWindowSize(1337);
        sharedProperties.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        sharedProperties.setPermittedNumberOfCallsInHalfOpenState(1000);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties backendWithDefaultConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("defaultConfig");
        backendWithDefaultConfig.setPermittedNumberOfCallsInHalfOpenState(99);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties backendWithSharedConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setPermittedNumberOfCallsInHalfOpenState(999);

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("defaultConfig", defaultProperties);
        circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        circuitBreakerConfigurationProperties.getInstances()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        circuitBreakerConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //Then
        assertThat(circuitBreakerConfigurationProperties.getInstances()).hasSize(2);

        // Should get default config and overwrite setPermittedNumberOfCallsInHalfOpenState
        CircuitBreakerConfig circuitBreaker1 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backendWithDefaultConfig", backendWithDefaultConfig,
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker1).isNotNull();
        assertThat(circuitBreaker1.getSlidingWindowSize()).isEqualTo(1000);
        assertThat(circuitBreaker1.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(99);

        // Should get shared config and overwrite setPermittedNumberOfCallsInHalfOpenState
        CircuitBreakerConfig circuitBreaker2 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backendWithSharedConfig", backendWithSharedConfig,
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker2).isNotNull();
        assertThat(circuitBreaker2.getSlidingWindowSize()).isEqualTo(1337);
        assertThat(circuitBreaker2.getSlidingWindowType())
            .isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        assertThat(circuitBreaker2.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(999);

        // Unknown backend should get default config of Registry
        CircuitBreakerConfig circuitBreaker3 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig(
                "UN_KNOWN", new CommonCircuitBreakerConfigurationProperties.InstanceProperties(),
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker3).isNotNull();
        assertThat(circuitBreaker3.getSlidingWindowSize())
            .isEqualTo(CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SIZE);

    }

    @Test
    public void testCreateCircuitBreakerRegistryWithDefaultConfig() {
        //Given
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlidingWindowSize(1000);
        defaultProperties.setPermittedNumberOfCallsInHalfOpenState(100);
        defaultProperties.setWaitDurationInOpenState(Duration.ofMillis(100));

        CommonCircuitBreakerConfigurationProperties.InstanceProperties sharedProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        sharedProperties.setSlidingWindowSize(1337);
        sharedProperties.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        sharedProperties.setPermittedNumberOfCallsInHalfOpenState(1000);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties backendWithoutBaseConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithoutBaseConfig.setPermittedNumberOfCallsInHalfOpenState(99);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties backendWithSharedConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setPermittedNumberOfCallsInHalfOpenState(999);

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);
        circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        circuitBreakerConfigurationProperties.getInstances()
            .put("backendWithoutBaseConfig", backendWithoutBaseConfig);

        circuitBreakerConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //Then
        assertThat(circuitBreakerConfigurationProperties.getInstances()).hasSize(2);

        // Should get default config and overwrite setPermittedNumberOfCallsInHalfOpenState
        CircuitBreakerConfig circuitBreaker1 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backendWithoutBaseConfig", backendWithoutBaseConfig,
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker1).isNotNull();
        assertThat(circuitBreaker1.getSlidingWindowSize()).isEqualTo(1000);
        assertThat(circuitBreaker1.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(99);

        // Should get shared config and overwrite setPermittedNumberOfCallsInHalfOpenState
        CircuitBreakerConfig circuitBreaker2 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backendWithSharedConfig", backendWithSharedConfig,
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker2).isNotNull();
        assertThat(circuitBreaker2.getSlidingWindowSize()).isEqualTo(1337);
        assertThat(circuitBreaker2.getSlidingWindowType())
            .isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        assertThat(circuitBreaker2.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(999);

        // Unknown backend should get default config of Registry
        CircuitBreakerConfig circuitBreaker3 = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig(
                "UN_KNOWN", new CommonCircuitBreakerConfigurationProperties.InstanceProperties(),
                compositeCircuitBreakerCustomizer());
        assertThat(circuitBreaker3).isNotNull();
        assertThat(circuitBreaker3.getSlidingWindowSize()).isEqualTo(1000);
    }

    @Test
    public void testCreateCircuitBreakerRegistryWithUnknownConfig() {
        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("unknownConfig");
        circuitBreakerConfigurationProperties.getInstances().put("backend", instanceProperties);

        //When
        assertThatThrownBy(() -> circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("backend", instanceProperties,
                compositeCircuitBreakerCustomizer()))
            .isInstanceOf(ConfigurationNotFoundException.class)
            .hasMessage("Configuration with name 'unknownConfig' does not exist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnEventConsumerBufferSizeLessThanOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnFailureRateThresholdLessThanOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setFailureRateThreshold(0.999f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnFailureRateThresholdAboveHundred() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setFailureRateThreshold(100.001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnWaitDurationInOpenStateLessThanMillisecond() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setWaitDurationInOpenState(Duration.ofNanos(999999));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnPermittedNumberOfCallsInHalfOpenStateLessThanOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setPermittedNumberOfCallsInHalfOpenState(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnMinimumNumberOfCallsLessThanOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setMinimumNumberOfCalls(0);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnSlidingWindowSizeLessThanOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlidingWindowSize(0);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnSlowCallRateThresholdLessThanOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlowCallRateThreshold(0.999f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnSlowCallRateThresholdAboveHundred() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlowCallRateThreshold(100.001f);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnSlowCallDurationThresholdLessThanOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlowCallDurationThreshold(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnWaitDurationInHalfOpenStateNegative() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxWaitDurationInHalfOpenState(Duration.ofMillis(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnExponentialBackoffMultiplierZeroOrLess() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setExponentialBackoffMultiplier(0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnExponentialMaxWaitDurationInOpenStateLessThanMillisecond() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setExponentialMaxWaitDurationInOpenState(Duration.ofNanos(999999));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnRandomizedWaitFactorNegative() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setRandomizedWaitFactor(-0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnRandomizedWaitFactorBiggerOrEqualOne() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setRandomizedWaitFactor(1.0);
    }

    @Test
    public void testCircuitBreakerConfigWithBaseConfig() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultConfig.setSlidingWindowSize(2000);
        defaultConfig.setWaitDurationInOpenState(Duration.ofMillis(100L));

        CommonCircuitBreakerConfigurationProperties.InstanceProperties sharedConfigWithDefaultConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        sharedConfigWithDefaultConfig.setWaitDurationInOpenState(Duration.ofMillis(1000L));
        sharedConfigWithDefaultConfig.setBaseConfig("defaultConfig");

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceWithSharedConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceWithSharedConfig.setBaseConfig("sharedConfig");


        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("defaultConfig", defaultConfig);
        circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedConfigWithDefaultConfig);
        circuitBreakerConfigurationProperties.getInstances().put("instanceWithSharedConfig", instanceWithSharedConfig);


        CircuitBreakerConfig instance = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("instanceWithSharedConfig", instanceWithSharedConfig, compositeCircuitBreakerCustomizer());
        assertThat(instance).isNotNull();
        assertThat(instance.getSlidingWindowSize()).isEqualTo(2000);
        assertThat(instance.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(1000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRecordExceptionWithBaseConfig() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("defaultConfig");
        instanceProperties.setRecordExceptions(new Class[] {IllegalArgumentException.class});

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("defaultConfig", defaultConfig);


        CircuitBreakerConfig circuitBreakerConfig = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("instanceWithDefaultConfig", instanceProperties, compositeCircuitBreakerCustomizer());

        assertThat(circuitBreakerConfig.getRecordExceptionPredicate().test(new IllegalArgumentException())).isTrue();
        assertThat(circuitBreakerConfig.getRecordExceptionPredicate().test(new NullPointerException())).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIgnoreExceptionWithBaseConfig() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("defaultConfig");
        instanceProperties.setIgnoreExceptions(new Class[] {IllegalArgumentException.class});

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("defaultConfig", defaultConfig);


        CircuitBreakerConfig circuitBreakerConfig = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("instanceWithDefaultConfig", instanceProperties, compositeCircuitBreakerCustomizer());

        assertThat(circuitBreakerConfig.getIgnoreExceptionPredicate().test(new IllegalArgumentException())).isTrue();
        assertThat(circuitBreakerConfig.getIgnoreExceptionPredicate().test(new NullPointerException())).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIgnoreExceptionPredicateWithBaseConfig() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("defaultConfig");
        instanceProperties.setIgnoreExceptionPredicate((Class)RecordFailurePredicate.class);

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("defaultConfig", defaultConfig);


        CircuitBreakerConfig circuitBreakerConfig = circuitBreakerConfigurationProperties
            .createCircuitBreakerConfig("instanceWithDefaultConfig", instanceProperties, compositeCircuitBreakerCustomizer());

        assertThat(circuitBreakerConfig.getIgnoreExceptionPredicate().test(new IOException())).isTrue();
        assertThat(circuitBreakerConfig.getIgnoreExceptionPredicate().test(new NullPointerException())).isFalse();
    }

    @Test(expected = IllegalStateException.class)
    public void testCircularReferenceInBaseConfigThrowsIllegalStateException() {
        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();

        CommonCircuitBreakerConfigurationProperties.InstanceProperties selfReferencingConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        selfReferencingConfig.setBaseConfig("selfReferencingConfig");
        circuitBreakerConfigurationProperties.getConfigs().put("selfReferencingConfig", selfReferencingConfig);

        circuitBreakerConfigurationProperties.createCircuitBreakerConfig("selfReferencingConfig", selfReferencingConfig, compositeCircuitBreakerCustomizer());
    }

    @Test
    public void testFindCircuitBreakerPropertiesWithoutDefaultConfig() {
        //Given
        CommonCircuitBreakerConfigurationProperties.InstanceProperties backendWithoutBaseConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getInstances().put("backendWithoutBaseConfig", backendWithoutBaseConfig);

        //Then
        assertThat(circuitBreakerConfigurationProperties.getInstances()).hasSize(1);

        // Should get default config and overwrite registerHealthIndicator, allowHealthIndicatorToFail and eventConsumerBufferSize
        Optional<CommonCircuitBreakerConfigurationProperties.InstanceProperties> circuitBreakerProperties =
            circuitBreakerConfigurationProperties.findCircuitBreakerProperties("backendWithoutBaseConfig");
        assertThat(circuitBreakerProperties).isPresent();
        assertThat(circuitBreakerProperties.get().getRegisterHealthIndicator()).isNull();
        assertThat(circuitBreakerProperties.get().getAllowHealthIndicatorToFail()).isNull();
        assertThat(circuitBreakerProperties.get().getEventConsumerBufferSize()).isNull();
    }

    @Test
    public void testFindCircuitBreakerPropertiesWithDefaultConfig() {
        //Given
        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setRegisterHealthIndicator(true);
        defaultProperties.setEventConsumerBufferSize(99);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties backendWithoutBaseConfig = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);
        circuitBreakerConfigurationProperties.getInstances().put("backendWithoutBaseConfig", backendWithoutBaseConfig);

        //Then
        assertThat(circuitBreakerConfigurationProperties.getInstances()).hasSize(1);

        // Should get default config and overwrite registerHealthIndicator and eventConsumerBufferSize but not allowHealthIndicatorToFail
        Optional<CommonCircuitBreakerConfigurationProperties.InstanceProperties> circuitBreakerProperties =
            circuitBreakerConfigurationProperties.findCircuitBreakerProperties("backendWithoutBaseConfig");
        assertThat(circuitBreakerProperties).isPresent();
        assertThat(circuitBreakerProperties.get().getRegisterHealthIndicator()).isTrue();
        assertThat(circuitBreakerProperties.get().getAllowHealthIndicatorToFail()).isNull();
        assertThat(circuitBreakerProperties.get().getEventConsumerBufferSize()).isEqualTo(99);
    }

    @Test
    public void testCreateCircuitBreakerRegistryWithBaseAndDefaultConfig() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties baseProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        baseProperties.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        baseProperties.setSlidingWindowSize(2000);
        baseProperties.setMaxWaitDurationInHalfOpenState(Duration.ofMillis(100L));

        CommonCircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        defaultProperties.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        defaultProperties.setSlidingWindowSize(1000);
        defaultProperties.setBaseConfig("baseConfig");

        CommonCircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CommonCircuitBreakerConfigurationProperties();
        circuitBreakerConfigurationProperties.getConfigs().put("baseConfig", baseProperties);
        circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);

        CircuitBreakerConfig config = circuitBreakerConfigurationProperties.createCircuitBreakerConfig(
                "default", defaultProperties, compositeCircuitBreakerCustomizer());

        assertThat(config.getMaxWaitDurationInHalfOpenState()).isEqualTo(baseProperties.getMaxWaitDurationInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(defaultProperties.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualTo(defaultProperties.getSlidingWindowType());
    }


    private CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }
}
