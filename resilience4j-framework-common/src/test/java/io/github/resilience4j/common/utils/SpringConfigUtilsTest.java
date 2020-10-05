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

import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit test for spring config merge properties
 */
public class SpringConfigUtilsTest {

    @Test
    public void testBulkHeadMergeSpringProperties() {
        BulkheadConfigurationProperties.InstanceProperties shared = new BulkheadConfigurationProperties.InstanceProperties();
        shared.setMaxConcurrentCalls(3);
        shared.setEventConsumerBufferSize(200);
        assertThat(shared.getEventConsumerBufferSize()).isEqualTo(200);

        BulkheadConfigurationProperties.InstanceProperties instanceProperties = new BulkheadConfigurationProperties.InstanceProperties();
        instanceProperties.setMaxConcurrentCalls(3);
        assertThat(instanceProperties.getEventConsumerBufferSize()).isNull();

        ConfigUtils.mergePropertiesIfAny(shared, instanceProperties);

        assertThat(instanceProperties.getEventConsumerBufferSize()).isEqualTo(200);
    }

    @Test
    public void testCircuitBreakerMergeSpringProperties() {

        CircuitBreakerConfigurationProperties.InstanceProperties sharedProperties = new CircuitBreakerConfigurationProperties.InstanceProperties();
        sharedProperties.setSlidingWindowSize(1337);
        sharedProperties.setPermittedNumberOfCallsInHalfOpenState(1000);
        sharedProperties.setRegisterHealthIndicator(true);
        sharedProperties.setAllowHealthIndicatorToFail(true);
        sharedProperties.setEventConsumerBufferSize(200);

        CircuitBreakerConfigurationProperties.InstanceProperties backendWithDefaultConfig = new CircuitBreakerConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setPermittedNumberOfCallsInHalfOpenState(99);
        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isNull();
        assertThat(backendWithDefaultConfig.getAllowHealthIndicatorToFail()).isNull();

        ConfigUtils.mergePropertiesIfAny(backendWithDefaultConfig, sharedProperties);

        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isEqualTo(200);
        assertThat(backendWithDefaultConfig.getRegisterHealthIndicator()).isTrue();
        assertThat(backendWithDefaultConfig.getAllowHealthIndicatorToFail()).isTrue();
    }

    @Test
    public void testRetrySpringProperties() {
        RetryConfigurationProperties.InstanceProperties sharedProperties = new RetryConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxAttempts(2);
        sharedProperties.setWaitDuration(Duration.ofMillis(100));
        sharedProperties.setEnableRandomizedWait(true);
        sharedProperties.setExponentialBackoffMultiplier(0.1);
        sharedProperties.setExponentialMaxWaitDuration(Duration.ofMinutes(2));
        sharedProperties.setEnableExponentialBackoff(false);

        RetryConfigurationProperties.InstanceProperties backendWithDefaultConfig = new RetryConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setWaitDuration(Duration.ofMillis(200L));
        assertThat(backendWithDefaultConfig.getEnableExponentialBackoff()).isNull();
        assertThat(backendWithDefaultConfig.getExponentialBackoffMultiplier()).isNull();
        assertThat(backendWithDefaultConfig.getExponentialMaxWaitDuration()).isNull();
        assertThat(backendWithDefaultConfig.getEnableRandomizedWait()).isNull();

        ConfigUtils.mergePropertiesIfAny(sharedProperties, backendWithDefaultConfig);
        assertThat(backendWithDefaultConfig.getEnableExponentialBackoff()).isFalse();
        assertThat(backendWithDefaultConfig.getExponentialBackoffMultiplier()).isEqualTo(0.1);
        assertThat(backendWithDefaultConfig.getExponentialMaxWaitDuration()).isEqualTo(Duration.ofMinutes(2));
        assertThat(backendWithDefaultConfig.getEnableRandomizedWait()).isTrue();
    }

    @Test
    public void testRateLimiterSpringProperties() {

        RateLimiterConfigurationProperties.InstanceProperties sharedProperties = new RateLimiterConfigurationProperties.InstanceProperties();
        sharedProperties.setLimitForPeriod(2);
        sharedProperties.setLimitRefreshPeriod(Duration.ofMillis(6000000));
        sharedProperties.setSubscribeForEvents(true);
        sharedProperties.setRegisterHealthIndicator(true);
        sharedProperties.setEventConsumerBufferSize(200);

        RateLimiterConfigurationProperties.InstanceProperties backendWithDefaultConfig = new RateLimiterConfigurationProperties.InstanceProperties();
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

	@Test
	public void testTimeLimiterSpringProperties() {

		TimeLimiterConfigurationProperties.InstanceProperties sharedProperties = new TimeLimiterConfigurationProperties.InstanceProperties();
		sharedProperties.setTimeoutDuration(Duration.ofSeconds(20));
		sharedProperties.setCancelRunningFuture(false);
		sharedProperties.setEventConsumerBufferSize(200);

		TimeLimiterConfigurationProperties.InstanceProperties backendWithDefaultConfig = new TimeLimiterConfigurationProperties.InstanceProperties();
		sharedProperties.setCancelRunningFuture(true);
		assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isNull();

		ConfigUtils.mergePropertiesIfAny(sharedProperties, backendWithDefaultConfig);

		assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isEqualTo(200);
	}
}
