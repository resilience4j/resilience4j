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
package io.github.resilience4j.utils;

import java.beans.FeatureDescriptor;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;

/**
 * common utils for spring configuration
 */
public class CommonUtils {


	/**
	 * @param source source entity
	 * @param target target entity
	 * @param <T>    type of entity
	 * @return merged entity with its properties
	 */
	public static <T> T mergeProperties(T source, T target) {
		BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
		return target;
	}

	/**
	 * @return default backend properties for the circuit breaker
	 */
	public static CircuitBreakerConfigurationProperties.BackendProperties getDefaultProperties() {
		CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
		CircuitBreakerConfigurationProperties.BackendProperties backendProperties = new CircuitBreakerConfigurationProperties.BackendProperties();
		backendProperties.setFailureRateThreshold(Math.round(circuitBreakerConfig.getFailureRateThreshold()));
		backendProperties.setAutomaticTransitionFromOpenToHalfOpenEnabled(circuitBreakerConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled());
		backendProperties.setEventConsumerBufferSize(100);
		backendProperties.setRegisterHealthIndicator(true);
		backendProperties.setRingBufferSizeInClosedState(circuitBreakerConfig.getRingBufferSizeInClosedState());
		backendProperties.setRingBufferSizeInHalfOpenState(circuitBreakerConfig.getRingBufferSizeInHalfOpenState());
		backendProperties.setWaitDurationInOpenState(circuitBreakerConfig.getWaitDurationInOpenState());
		return backendProperties;
	}

	/**
	 * @param source source entity
	 * @return array of null properties
	 */
	private static String[] getNullPropertyNames(Object source) {
		final BeanWrapper wrappedSource = new BeanWrapperImpl(source);
		return Stream.of(wrappedSource.getPropertyDescriptors())
				.map(FeatureDescriptor::getName)
				.filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null)
				.toArray(String[]::new);
	}
}
