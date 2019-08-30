/*
 * Copyright 2019 Dan Maas
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
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.Builder;
import io.github.resilience4j.common.utils.ConfigUtils;
import io.github.resilience4j.core.ClassUtils;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;
import org.hibernate.validator.constraints.time.DurationMin;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.*;

public class CircuitBreakerConfigurationProperties {

	private Map<String, InstanceProperties> instances = new HashMap<>();
	private Map<String, InstanceProperties> configs = new HashMap<>();

	public Optional<InstanceProperties> findCircuitBreakerProperties(String name) {
		return Optional.ofNullable(instances.get(name));
	}

	public CircuitBreakerConfig createCircuitBreakerConfig(InstanceProperties instanceProperties) {
		if (StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
			InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
			if (baseProperties == null) {
				throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
			}
			return buildConfigFromBaseConfig(instanceProperties, baseProperties);
		}
		return buildConfig(custom(), instanceProperties);
	}

	private CircuitBreakerConfig buildConfigFromBaseConfig(InstanceProperties instanceProperties, InstanceProperties baseProperties) {
		ConfigUtils.mergePropertiesIfAny(instanceProperties, baseProperties);
		CircuitBreakerConfig baseConfig = buildConfig(custom(), baseProperties);
		return buildConfig(from(baseConfig), instanceProperties);
	}

	private CircuitBreakerConfig buildConfig(Builder builder, InstanceProperties properties) {
		if (properties == null) {
			return builder.build();
		}
		if (properties.getWaitDurationInOpenState() != null) {
			builder.waitDurationInOpenState(properties.getWaitDurationInOpenState());
		}

		if (properties.getFailureRateThreshold() != null) {
			builder.failureRateThreshold(properties.getFailureRateThreshold());
		}

		if (properties.getSlowCallRateThreshold() != null) {
			builder.slowCallRateThreshold(properties.getSlowCallRateThreshold());
		}

		if (properties.getSlowCallDurationThreshold() != null) {
			builder.slowCallDurationThreshold(properties.getSlowCallDurationThreshold());
		}

		if (properties.getRingBufferSizeInClosedState() != null) {
			builder.ringBufferSizeInClosedState(properties.getRingBufferSizeInClosedState());
		}

		if (properties.getSlidingWindowSize() != null) {
			builder.slidingWindowSize(properties.getSlidingWindowSize());
		}

		if (properties.getMinimumNumberOfCalls() != null) {
			builder.minimumNumberOfCalls(properties.getMinimumNumberOfCalls());
		}

		if (properties.getSlidingWindowType() != null) {
			builder.slidingWindowType(properties.getSlidingWindowType());
		}

		if (properties.getRingBufferSizeInHalfOpenState() != null) {
			builder.ringBufferSizeInHalfOpenState(properties.getRingBufferSizeInHalfOpenState());
		}

		if (properties.getPermittedNumberOfCallsInHalfOpenState() != null) {
			builder.permittedNumberOfCallsInHalfOpenState(properties.getPermittedNumberOfCallsInHalfOpenState());
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

	private void buildRecordFailurePredicate(InstanceProperties properties, Builder builder) {
		if (properties.getRecordFailurePredicate() != null) {
			Predicate<Throwable> predicate = ClassUtils.instantiatePredicateClass(properties.getRecordFailurePredicate());
			if (predicate != null) {
				builder.recordFailure(predicate);
			}
		}
	}

	@Nullable
	public InstanceProperties getBackendProperties(String backend) {
		return instances.get(backend);
	}

	public Map<String, InstanceProperties> getInstances() {
		return instances;
	}

	/**
	 * For backwards compatibility when setting backends in configuration properties.
	 */
	public Map<String, InstanceProperties> getBackends() {
		return instances;
	}

	public Map<String, InstanceProperties> getConfigs() {
		return configs;
	}

	/**
	 * Class storing property values for configuring {@link io.github.resilience4j.circuitbreaker.CircuitBreaker} instances.
	 */
	public static class InstanceProperties {

		@DurationMin(millis = 1)
		@Nullable
		private Duration waitDurationInOpenState;

		@DurationMin(nanos = 1)
		@Nullable
		private Duration slowCallDurationThreshold;

		@Min(1)
		@Max(100)
		@Nullable
		private Float failureRateThreshold;

		@Min(1)
		@Max(100)
		@Nullable
		private Float slowCallRateThreshold;

		@Min(1)
		@Nullable
		@Deprecated
		private Integer ringBufferSizeInClosedState;

		@Nullable
		private SlidingWindowType slidingWindowType;

		@Min(1)
		@Nullable
		private Integer slidingWindowSize;

		@Min(1)
		@Nullable
		private Integer minimumNumberOfCalls;

		@Min(1)
		@Nullable
		private Integer permittedNumberOfCallsInHalfOpenState;

		@Min(1)
		@Nullable
		@Deprecated
		private Integer ringBufferSizeInHalfOpenState;

		@Nullable
		private Boolean automaticTransitionFromOpenToHalfOpenEnabled;

		@Min(1)
		@Nullable
		private Integer eventConsumerBufferSize;

		@Nullable
		private Boolean registerHealthIndicator;

		@Nullable
		private Class<Predicate<Throwable>> recordFailurePredicate;

		@Nullable
		private Class<? extends Throwable>[] recordExceptions;

		@Nullable
		private Class<? extends Throwable>[] ignoreExceptions;

		@Nullable
		private String baseConfig;


		/**
		 * Returns the failure rate threshold for the circuit breaker as percentage.
		 *
		 * @return the failure rate threshold
		 */
		@Nullable
		public Float getFailureRateThreshold() {
			return failureRateThreshold;
		}

		/**
		 * Sets the failure rate threshold for the circuit breaker as percentage.
		 *
		 * @param failureRateThreshold the failure rate threshold
		 */
		public InstanceProperties setFailureRateThreshold(Float failureRateThreshold) {
			this.failureRateThreshold = failureRateThreshold;
			return this;
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
		 * @param waitDurationInOpenStateMillis the wait duration
		 */
		public InstanceProperties setWaitDurationInOpenState(Duration waitDurationInOpenStateMillis) {
			this.waitDurationInOpenState = waitDurationInOpenStateMillis;
			return this;
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
		@Deprecated
		public InstanceProperties setRingBufferSizeInClosedState(Integer ringBufferSizeInClosedState) {
			this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
			return this;
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
		@Deprecated
		public InstanceProperties setRingBufferSizeInHalfOpenState(Integer ringBufferSizeInHalfOpenState) {
			this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
			return this;
		}

		/**
		 * Returns if we should automatically transition to half open after the timer has run out.
		 *
		 * @return setAutomaticTransitionFromOpenToHalfOpenEnabled if we should automatically go to half open or not
		 */
		public Boolean getAutomaticTransitionFromOpenToHalfOpenEnabled() {
			return this.automaticTransitionFromOpenToHalfOpenEnabled;
		}

		/**
		 * Sets if we should automatically transition to half open after the timer has run out.
		 *
		 * @param automaticTransitionFromOpenToHalfOpenEnabled The flag for automatic transition to half open after the timer has run out.
		 */
		public InstanceProperties setAutomaticTransitionFromOpenToHalfOpenEnabled(Boolean automaticTransitionFromOpenToHalfOpenEnabled) {
			this.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled;
			return this;
		}

		public Integer getEventConsumerBufferSize() {
			return eventConsumerBufferSize;
		}

		public InstanceProperties setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
			this.eventConsumerBufferSize = eventConsumerBufferSize;
			return this;
		}

		public Boolean getRegisterHealthIndicator() {
			return registerHealthIndicator;
		}

		public InstanceProperties setRegisterHealthIndicator(Boolean registerHealthIndicator) {
			this.registerHealthIndicator = registerHealthIndicator;
			return this;
		}

		@Nullable
		public Class<Predicate<Throwable>> getRecordFailurePredicate() {
			return recordFailurePredicate;
		}

		public InstanceProperties setRecordFailurePredicate(Class<Predicate<Throwable>> recordFailurePredicate) {
			this.recordFailurePredicate = recordFailurePredicate;
			return this;
		}

		@Nullable
		public Class<? extends Throwable>[] getRecordExceptions() {
			return recordExceptions;
		}

		public InstanceProperties setRecordExceptions(Class<? extends Throwable>[] recordExceptions) {
			this.recordExceptions = recordExceptions;
			return this;
		}

		@Nullable
		public Class<? extends Throwable>[] getIgnoreExceptions() {
			return ignoreExceptions;
		}

		public InstanceProperties setIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
			this.ignoreExceptions = ignoreExceptions;
			return this;
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
		public InstanceProperties setBaseConfig(String baseConfig) {
			this.baseConfig = baseConfig;
			return this;
		}

		@Nullable
		public Integer getPermittedNumberOfCallsInHalfOpenState() {
			return permittedNumberOfCallsInHalfOpenState;
		}

		public void setPermittedNumberOfCallsInHalfOpenState(@Nullable Integer permittedNumberOfCallsInHalfOpenState) {
			this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
		}

		@Nullable
		public Integer getMinimumNumberOfCalls() {
			return minimumNumberOfCalls;
		}

		public void setMinimumNumberOfCalls(@Nullable Integer minimumNumberOfCalls) {
			this.minimumNumberOfCalls = minimumNumberOfCalls;
		}

		@Nullable
		public Integer getSlidingWindowSize() {
			return slidingWindowSize;
		}

		public void setSlidingWindowSize(@Nullable Integer slidingWindowSize) {
			this.slidingWindowSize = slidingWindowSize;
		}

		@Nullable
		public Float getSlowCallRateThreshold() {
			return slowCallRateThreshold;
		}

		public void setSlowCallRateThreshold(@Nullable Float slowCallRateThreshold) {
			this.slowCallRateThreshold = slowCallRateThreshold;
		}

		@Nullable
		public Duration getSlowCallDurationThreshold() {
			return slowCallDurationThreshold;
		}

		public void setSlowCallDurationThreshold(@Nullable Duration slowCallDurationThreshold) {
			this.slowCallDurationThreshold = slowCallDurationThreshold;
		}

		@Nullable
		public SlidingWindowType getSlidingWindowType() {
			return slidingWindowType;
		}

		public void setSlidingWindowType(SlidingWindowType slidingWindowType) {
			this.slidingWindowType = slidingWindowType;
		}
	}

}
