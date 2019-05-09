/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.configure;


import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.Builder;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.Nullable;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Configuration
public class CircuitBreakerConfigurationProperties {
	// This property gives you control over CircuitBreaker aspect application order.
	// By default CircuitBreaker will be executed BEFORE RateLimiter.
	// By adjusting RateLimiterProperties.rateLimiterAspectOrder and CircuitBreakerProperties.circuitBreakerAspectOrder
	// you explicitly define aspects CircuitBreaker and RateLimiter execution sequence.
	private int circuitBreakerAspectOrder = Integer.MAX_VALUE - 1;
	private Map<String, BackendProperties> backends = new HashMap<>();
	private Map<String, BackendProperties> configs = new HashMap<>();

	public int getCircuitBreakerAspectOrder() {
		return circuitBreakerAspectOrder;
	}

	public void setCircuitBreakerAspectOrder(int circuitBreakerAspectOrder) {
		this.circuitBreakerAspectOrder = circuitBreakerAspectOrder;
	}

	public Optional<BackendProperties> findCircuitBreakerProperties(String name) {
		return Optional.ofNullable(backends.get(name));
	}

	public CircuitBreakerConfig createCircuitBreakerConfig(BackendProperties backendProperties) {
		if(backendProperties.getBaseConfig() != null){
			BackendProperties baseProperties = configs.get(backendProperties.getBaseConfig());
			if(baseProperties == null){
				throw new ConfigurationNotFoundException(backendProperties.getBaseConfig());
			}
			return buildConfigFromBaseConfig(backendProperties, baseProperties);
		}
		return buildConfig(CircuitBreakerConfig.custom(), backendProperties);
	}

	private CircuitBreakerConfig buildConfigFromBaseConfig(BackendProperties backendProperties, BackendProperties baseProperties) {
		CircuitBreakerConfig baseConfig = buildConfig(CircuitBreakerConfig.custom(), baseProperties);
		return buildConfig(CircuitBreakerConfig.from(baseConfig), backendProperties);
	}

	private CircuitBreakerConfig buildConfig(Builder builder, BackendProperties properties) {

		if (properties.getWaitDurationInOpenState() != null) {
			builder.waitDurationInOpenState(properties.getWaitDurationInOpenState());
		}

		if (properties.getFailureRateThreshold() != null) {
			builder.failureRateThreshold(properties.getFailureRateThreshold());
		}

		if (properties.getRingBufferSizeInClosedState() != null) {
			builder.ringBufferSizeInClosedState(properties.getRingBufferSizeInClosedState());
		}

		if (properties.getRingBufferSizeInHalfOpenState() != null) {
			builder.ringBufferSizeInHalfOpenState(properties.getRingBufferSizeInHalfOpenState());
		}

		if (properties.recordFailurePredicate != null) {
			buildRecordFailurePredicate(properties, builder);
		}

		if (properties.recordExceptions != null) {
			builder.recordExceptions(properties.recordExceptions);
		}

		if (properties.ignoreExceptions != null) {
			builder.ignoreExceptions(properties.ignoreExceptions);
		}

		if (properties.automaticTransitionFromOpenToHalfOpenEnabled != null) {
			builder.automaticTransitionFromOpenToHalfOpenEnabled(properties.automaticTransitionFromOpenToHalfOpenEnabled);
		}

		return builder.build();
	}

	protected void buildRecordFailurePredicate(BackendProperties properties, Builder builder) {
		builder.recordFailure(BeanUtils.instantiateClass(properties.getRecordFailurePredicate()));
	}

	public Map<String, BackendProperties> getBackends() {
		return backends;
	}

	public Map<String, BackendProperties> getConfigs() {
		return configs;
	}

	/**
	 * Class storing property values for configuring {@link io.github.resilience4j.circuitbreaker.CircuitBreaker} instances.
	 */
	public static class BackendProperties {

		@DurationMin(seconds = 1)
		@Nullable
		private Duration waitDurationInOpenState;

		@Min(1)
		@Max(100)
		@Nullable
		private Integer failureRateThreshold;

		@Min(1)
		@Nullable
		private Integer ringBufferSizeInClosedState;

		@Min(1)
		@Nullable
		private Integer ringBufferSizeInHalfOpenState;

		@Nullable
		private Boolean automaticTransitionFromOpenToHalfOpenEnabled;

		@Min(1)
		private Integer eventConsumerBufferSize = 100;

		@NotNull
		private Boolean registerHealthIndicator = true;

		@Nullable
		private Class<Predicate<Throwable>> recordFailurePredicate;

		@Nullable
		private Class<? extends Throwable>[] recordExceptions;

		@Nullable
		private Class<? extends Throwable>[] ignoreExceptions;

		@Nullable
		private String baseConfig;


		/**
		 * @deprecated
		 * @since (0.0.14) use instead {@link #setWaitDurationInOpenState(Duration)} ()}
		 * Sets the wait duration in seconds the CircuitBreaker should stay open, before it switches to half closed.
		 *
		 * @param waitInterval the wait duration
		 */
		@Deprecated
		public void setWaitInterval(Integer waitInterval) {
			this.waitDurationInOpenState = Duration.ofMillis(waitInterval);
		}

		/**
		 * @deprecated
		 * @since (0.0.14) use instead {@link #getWaitDurationInOpenState()}
		 *
		 * get the wait duration in seconds the CircuitBreaker should stay open, before it switches to half closed.
		 *
		 * @return waitInterval the wait duration
		 */
		@Deprecated
		public Duration getWaitInterval() {
			return getWaitDurationInOpenState();
		}

		/**
		 * Returns the failure rate threshold for the circuit breaker as percentage.
		 *
		 * @return the failure rate threshold
		 */
		@Nullable
		public Integer getFailureRateThreshold() {
			return failureRateThreshold;
		}

		/**
		 * Sets the failure rate threshold for the circuit breaker as percentage.
		 *
		 * @param failureRateThreshold the failure rate threshold
		 */
		public void setFailureRateThreshold(Integer failureRateThreshold) {
			this.failureRateThreshold = failureRateThreshold;
		}

		/**
		 * Returns the wait duration the CircuitBreaker will stay open, before it switches to half closed.
		 *
		 * @return the wait duration
		 */
		@Nullable
		public Duration getWaitDurationInOpenState() {
			return waitDurationInOpenState;
		}

		/**
		 * Sets the wait duration the CircuitBreaker should stay open, before it switches to half closed.
		 *
		 * @param waitDurationInOpenState the wait duration
		 */
		public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
			this.waitDurationInOpenState = waitDurationInOpenState;
		}

		/**
		 * Returns the ring buffer size for the circuit breaker while in closed state.
		 *
		 * @return the ring buffer size
		 */
		@Nullable
		public Integer getRingBufferSizeInClosedState() {
			return ringBufferSizeInClosedState;
		}

		/**
		 * Sets the ring buffer size for the circuit breaker while in closed state.
		 *
		 * @param ringBufferSizeInClosedState the ring buffer size
		 */
		public void setRingBufferSizeInClosedState(Integer ringBufferSizeInClosedState) {
			this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
		}

		/**
		 * Returns the ring buffer size for the circuit breaker while in half open state.
		 *
		 * @return the ring buffer size
		 */
		@Nullable
		public Integer getRingBufferSizeInHalfOpenState() {
			return ringBufferSizeInHalfOpenState;
		}

		/**
		 * Sets the ring buffer size for the circuit breaker while in half open state.
		 *
		 * @param ringBufferSizeInHalfOpenState the ring buffer size
		 */
		public void setRingBufferSizeInHalfOpenState(Integer ringBufferSizeInHalfOpenState) {
			this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
		}

		/**
		 * Returns if we should automaticly transition to half open after the timer has run out.
		 *
		 * @return automaticTransitionFromOpenToHalfOpenEnabled if we should automaticly go to half open or not
		 */
		public Boolean getAutomaticTransitionFromOpenToHalfOpenEnabled() {
			return this.automaticTransitionFromOpenToHalfOpenEnabled;
		}

		/**
		 * Sets if we should automatically transition to half open after the timer has run out.
		 *
		 * @param automaticTransitionFromOpenToHalfOpenEnabled The flag for automatic transition to half open after the timer has run out.
		 */
		public void setAutomaticTransitionFromOpenToHalfOpenEnabled(Boolean automaticTransitionFromOpenToHalfOpenEnabled) {
			this.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled;
		}

		public Integer getEventConsumerBufferSize() {
			return eventConsumerBufferSize;
		}

		public void setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
			this.eventConsumerBufferSize = eventConsumerBufferSize;
		}

		public Boolean getRegisterHealthIndicator() {
			return registerHealthIndicator;
		}

		public void setRegisterHealthIndicator(Boolean registerHealthIndicator) {
			this.registerHealthIndicator = registerHealthIndicator;
		}

		@Nullable
		public Class<Predicate<Throwable>> getRecordFailurePredicate() {
			return recordFailurePredicate;
		}

		public void setRecordFailurePredicate(Class<Predicate<Throwable>> recordFailurePredicate) {
			this.recordFailurePredicate = recordFailurePredicate;
		}

		@Nullable
		public Class<? extends Throwable>[] getRecordExceptions() {
			return recordExceptions;
		}

		public void setRecordExceptions(Class<? extends Throwable>[] recordExceptions) {
			this.recordExceptions = recordExceptions;
		}

		@Nullable
		public Class<? extends Throwable>[] getIgnoreExceptions() {
			return ignoreExceptions;
		}

		public void setIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
			this.ignoreExceptions = ignoreExceptions;
		}

		/**
		 * Gets the shared configuration name. If this is set, the configuration builder will use the the shared
		 * configuration backend over this one.
		 *
		 * @return The shared configuration name.
		 */
		@Nullable
		public String getBaseConfig() {
			return baseConfig;
		}

		/**
		 * Sets the shared configuration name. If this is set, the configuration builder will use the the shared
		 * configuration backend over this one.
		 *
		 * @param baseConfig The shared configuration name.
		 */
		public void setBaseConfig(String baseConfig) {
			this.baseConfig = baseConfig;
		}

	}

}
