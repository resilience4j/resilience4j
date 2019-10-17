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
package io.github.resilience4j.common.utils;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit test for spring confg merge properties
 */
public class SpringConfigUtilsTest {

    @Test
    public void testBulkHeadMergeSpringProperties() {
        io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties.InstanceProperties shared = new io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties.InstanceProperties();
        shared.setMaxConcurrentCalls(3);
        shared.setEventConsumerBufferSize(200);
        assertThat(shared.getEventConsumerBufferSize()).isEqualTo(200);

        io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties.InstanceProperties instanceProperties = new io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties.InstanceProperties();
        instanceProperties.setMaxConcurrentCalls(3);
        assertThat(instanceProperties.getEventConsumerBufferSize()).isNull();

        ConfigUtils.mergePropertiesIfAny(shared, instanceProperties);

        assertThat(instanceProperties.getEventConsumerBufferSize()).isEqualTo(200);


    }

    @Test
    public void testCircuitBreakerMergeSpringProperties() {

        io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
        sharedProperties.setRingBufferSizeInClosedState(1337);
        sharedProperties.setRingBufferSizeInHalfOpenState(1000);
        sharedProperties.setRegisterHealthIndicator(true);
        sharedProperties.setEventConsumerBufferSize(200);

        io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setRingBufferSizeInHalfOpenState(99);
        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isNull();
        assertThat(backendWithDefaultConfig.getRegisterHealthIndicator()).isNull();

        ConfigUtils.mergePropertiesIfAny(backendWithDefaultConfig, sharedProperties);

        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isEqualTo(200);
        assertThat(backendWithDefaultConfig.getRegisterHealthIndicator()).isTrue();


    }

    @Test
    public void testRetrySpringProperties() {
        io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxRetryAttempts(2);
        sharedProperties.setWaitDuration(Duration.ofMillis(100));
        sharedProperties.setEnableRandomizedWait(true);
        sharedProperties.setExponentialBackoffMultiplier(0.1);
        sharedProperties.setEnableExponentialBackoff(false);

        io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setWaitDuration(Duration.ofMillis(200L));
        assertThat(backendWithDefaultConfig.getEnableExponentialBackoff()).isNull();
        assertThat(backendWithDefaultConfig.getExponentialBackoffMultiplier()).isNull();
        assertThat(backendWithDefaultConfig.getEnableRandomizedWait()).isNull();

        ConfigUtils.mergePropertiesIfAny(sharedProperties, backendWithDefaultConfig);
        assertThat(backendWithDefaultConfig.getEnableExponentialBackoff()).isFalse();
        assertThat(backendWithDefaultConfig.getExponentialBackoffMultiplier()).isEqualTo(0.1);
        assertThat(backendWithDefaultConfig.getEnableRandomizedWait()).isTrue();

    }

    @Test
    public void testRateLimiterSpringProperties() {

        io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
        sharedProperties.setLimitForPeriod(2);
        sharedProperties.setLimitRefreshPeriod(Duration.ofMillis(6000000));
        sharedProperties.setSubscribeForEvents(true);
        sharedProperties.setRegisterHealthIndicator(true);
        sharedProperties.setEventConsumerBufferSize(200);

        io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setLimitForPeriod(200);
        assertThat(backendWithDefaultConfig.getRegisterHealthIndicator()).isNull();
        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isNull();
        assertThat(backendWithDefaultConfig.getSubscribeForEvents()).isNull();

        ConfigUtils.mergePropertiesIfAny(sharedProperties, backendWithDefaultConfig);

        assertThat(backendWithDefaultConfig.getRegisterHealthIndicator()).isTrue();
        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isEqualTo(200);
        assertThat(backendWithDefaultConfig.getSubscribeForEvents()).isTrue();
    }

}