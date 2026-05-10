/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.common.retry.configuration;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

class CommonRetryConfigurationPropertiesTest {

    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_WAIT_DURATION = instanceProperties -> instanceProperties.setWaitDuration(Duration.ofSeconds(1));
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> ENABLE_EXPONENTIAL_BACKOFF = instanceProperties -> instanceProperties.setEnableExponentialBackoff(true);
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_BACKOFF_MULTIPLIER = instanceProperties -> instanceProperties.setExponentialBackoffMultiplier(2.0);
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_EXPONENTIAL_MAX_WAIT_DURATION = instanceProperties -> instanceProperties.setExponentialMaxWaitDuration(Duration.ofSeconds(5));
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> ENABLE_RANDOMIZED_WAIT = instanceProperties -> instanceProperties.setEnableRandomizedWait(true);
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_RANDOMIZED_FACTOR = instanceProperties -> instanceProperties.setRandomizedWaitFactor(0.5);

    @Test
    void createRetryConfig_withDefault() {
        testCreateRetryConfig();
    }

    @Test
    void createRetryConfig_withWaitDuration() {
        testCreateRetryConfig(WITH_WAIT_DURATION);
    }

    @Test
    void createRetryConfig_withExponentialBackoff() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_EXPONENTIAL_BACKOFF);
    }

    @Test
    void createRetryConfig_withBackoffMultiplier() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER);
    }

    @Test
    void createRetryConfig_withExponentialMaxWaitDuration() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER, WITH_EXPONENTIAL_MAX_WAIT_DURATION);
    }

    @Test
    void createRetryConfig_withRandomizedWait() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT);
    }

    @Test
    void createRetryConfig_withRandomizedFactor() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, WITH_RANDOMIZED_FACTOR);
    }

    @Test
    void createRetryConfig_withRandomizedExponentialBackoff() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, ENABLE_EXPONENTIAL_BACKOFF);
    }

    @Test
    void createRetryConfig_withRandomizedExponentialBackoffMultiplier() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER);
    }

    @Test
    void createRetryConfig_withRandomizedFactorExponentialBackoffMultiplier() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, WITH_RANDOMIZED_FACTOR, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER);
    }

    @Test
    void createRetryConfig_withRandomizedExponentialMaxWaitDuration() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, WITH_RANDOMIZED_FACTOR, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER, WITH_EXPONENTIAL_MAX_WAIT_DURATION);
    }

    @SafeVarargs
    private void testCreateRetryConfig(Consumer<CommonRetryConfigurationProperties.InstanceProperties>... customizers) {
        String defaultConfigurationName = "default";
        String testConfigurationName = "test";

        CommonRetryConfigurationProperties properties = new CommonRetryConfigurationProperties();
        properties.getConfigs().put(defaultConfigurationName, new CommonRetryConfigurationProperties.InstanceProperties().setWaitDuration(Duration.ofSeconds(1)));

        CommonRetryConfigurationProperties.InstanceProperties instanceProperties = new CommonRetryConfigurationProperties.InstanceProperties().setBaseConfig(defaultConfigurationName);
        Arrays.stream(customizers).forEachOrdered(customizer -> customizer.accept(instanceProperties));
        properties.getInstances().put(testConfigurationName, instanceProperties);

        RetryConfig retryConfig = properties.createRetryConfig(testConfigurationName, new CompositeCustomizer<>(List.of()));

        assertThat(retryConfig).isNotNull(); // assert that retryConfig does not throw an exception
        assertThat(retryConfig.getIntervalFunction()).isNull();
        assertThat(retryConfig.getIntervalBiFunction()).isNotNull();
    }

}