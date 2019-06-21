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
import io.github.resilience4j.common.RecordFailurePredicate;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of circuit breaker registry
 */
public class CircuitBreakerConfigurationPropertiesTest {
	@Test
	public void testCreateCircuitBreakerRegistry() {
		//Given


		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties1 = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties1.setRingBufferSizeInClosedState(1000);
		instanceProperties1.setWaitDurationInOpenState(Duration.ofMillis(100));
		instanceProperties1.setEventConsumerBufferSize(100);
		instanceProperties1.setRegisterHealthIndicator(true);
		instanceProperties1.setRingBufferSizeInClosedState(200);
		instanceProperties1.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);
		instanceProperties1.setFailureRateThreshold(10);
		//noinspection unchecked
		instanceProperties1.setIgnoreExceptions(new Class[]{IllegalStateException.class});
		//noinspection unchecked
		instanceProperties1.setRecordExceptions(new Class[]{IllegalStateException.class});
		//noinspection unchecked
		instanceProperties1.setRecordFailurePredicate((Class) RecordFailurePredicate.class);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties2 = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties2.setRingBufferSizeInClosedState(1337);

		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		circuitBreakerConfigurationProperties.getInstances().put("backend1", instanceProperties1);
		circuitBreakerConfigurationProperties.getInstances().put("backend2", instanceProperties2);


		//Then
		assertThat(circuitBreakerConfigurationProperties.getBackends().size()).isEqualTo(2);
		assertThat(circuitBreakerConfigurationProperties.getInstances().size()).isEqualTo(2);
		CircuitBreakerConfig circuitBreaker1 = circuitBreakerConfigurationProperties.createCircuitBreakerConfig(instanceProperties1);
		assertThat(circuitBreaker1).isNotNull();
		assertThat(circuitBreaker1.getRingBufferSizeInClosedState()).isEqualTo(200);
		assertThat(circuitBreaker1.getWaitDurationInOpenState()).isEqualTo(Duration.ofMillis(100));
		final CircuitBreakerConfigurationProperties.InstanceProperties backend1 = circuitBreakerConfigurationProperties.getBackendProperties("backend1");
		assertThat(circuitBreakerConfigurationProperties.findCircuitBreakerProperties("backend1")).isNotEmpty();
		CircuitBreakerConfig circuitBreaker2 = circuitBreakerConfigurationProperties.createCircuitBreakerConfig(instanceProperties2);
		assertThat(circuitBreaker2).isNotNull();
		assertThat(circuitBreaker2.getRingBufferSizeInClosedState()).isEqualTo(1337);

	}

	@Test
	public void testCreateCircuitBreakerRegistryWithSharedConfigs() {
		//Given
		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties defaultProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		defaultProperties.setRingBufferSizeInClosedState(1000);
		defaultProperties.setRingBufferSizeInHalfOpenState(100);
		defaultProperties.setWaitDurationInOpenState(Duration.ofMillis(100));

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		sharedProperties.setRingBufferSizeInClosedState(1337);
		sharedProperties.setRingBufferSizeInHalfOpenState(1000);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setRingBufferSizeInHalfOpenState(99);

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties backendWithSharedConfig = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setRingBufferSizeInHalfOpenState(999);

		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();
		circuitBreakerConfigurationProperties.getConfigs().put("default", defaultProperties);
		circuitBreakerConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		circuitBreakerConfigurationProperties.getInstances().put("backendWithDefaultConfig", backendWithDefaultConfig);
		circuitBreakerConfigurationProperties.getInstances().put("backendWithSharedConfig", backendWithSharedConfig);


		//Then
		assertThat(circuitBreakerConfigurationProperties.getInstances().size()).isEqualTo(2);

		// Should get default config and overwrite setRingBufferSizeInHalfOpenState
		CircuitBreakerConfig circuitBreaker1 = circuitBreakerConfigurationProperties.createCircuitBreakerConfig(backendWithDefaultConfig);
		assertThat(circuitBreaker1).isNotNull();
		assertThat(circuitBreaker1.getRingBufferSizeInClosedState()).isEqualTo(1000);
		assertThat(circuitBreaker1.getRingBufferSizeInHalfOpenState()).isEqualTo(99);

		// Should get shared config and overwrite setRingBufferSizeInHalfOpenState
		CircuitBreakerConfig circuitBreaker2 = circuitBreakerConfigurationProperties.createCircuitBreakerConfig(backendWithSharedConfig);
		assertThat(circuitBreaker2).isNotNull();
		assertThat(circuitBreaker2.getRingBufferSizeInClosedState()).isEqualTo(1337);
		assertThat(circuitBreaker2.getRingBufferSizeInHalfOpenState()).isEqualTo(999);

		// Unknown backend should get default config of Registry
		CircuitBreakerConfig circuitBreaker3 = circuitBreakerConfigurationProperties.createCircuitBreakerConfig(new CircuitBreakerConfigurationProperties.InstanceProperties());
		assertThat(circuitBreaker3).isNotNull();
		assertThat(circuitBreaker3.getRingBufferSizeInClosedState()).isEqualTo(CircuitBreakerConfig.DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE);

	}

	@Test
	public void testCreateCircuitBreakerRegistryWithUnknownConfig() {
		CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationProperties();

		io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties();
		instanceProperties.setBaseConfig("unknownConfig");
		circuitBreakerConfigurationProperties.getInstances().put("backend", instanceProperties);


		//When
		assertThatThrownBy(() -> circuitBreakerConfigurationProperties.createCircuitBreakerConfig(instanceProperties))
				.isInstanceOf(ConfigurationNotFoundException.class)
				.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}
}