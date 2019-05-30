package io.github.resilience4j.retry.configure;
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

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Main spring properties for retry configuration
 */
public class RetryConfigurationProperties {
	private final Map<String, BackendProperties> backends = new HashMap<>();
	private Map<String, BackendProperties> configs = new HashMap<>();
	/*  This property gives you control over Retry aspect application order.
		By default Retry will be executed BEFORE Circuit breaker, rateLimiter and bulkhead.
		By adjusting each aspect order from ConfigurationProperties
		you explicitly define aspects execution sequence.
	*/
	private int retryAspectOrder = Integer.MAX_VALUE - 3;

	/**
	 * @param backend backend name
	 * @return the retry configuration
	 */
	public RetryConfig createRetryConfig(String backend) {
		return createRetryConfig(getBackendProperties(backend));
	}

	/**
	 * @return spring aspect apply order
	 */
	public int getRetryAspectOrder() {
		return retryAspectOrder;
	}

	/**
	 * @param retryAspectOrder spring the aspect apply order
	 */
	public void setRetryAspectOrder(int retryAspectOrder) {
		this.retryAspectOrder = retryAspectOrder;
	}

	/**
	 * @return the configured retry backend properties
	 */
	public Map<String, BackendProperties> getBackends() {
		return backends;
	}

	/**
	 * @return common configuration for retry backend
	 */
	public Map<String, BackendProperties> getConfigs() {
		return configs;
	}

	/**
	 * @param backendProperties the retry backend spring properties
	 * @return the retry configuration
	 */
	public RetryConfig createRetryConfig(BackendProperties backendProperties) {
		if (!StringUtils.isEmpty(backendProperties.getBaseConfig())) {
			BackendProperties baseProperties = configs.get(backendProperties.getBaseConfig());
			if (baseProperties == null) {
				throw new ConfigurationNotFoundException(backendProperties.getBaseConfig());
			}
			return buildConfigFromBaseConfig(baseProperties, backendProperties);
		}
		return buildRetryConfig(RetryConfig.custom(), backendProperties);
	}

	private RetryConfig buildConfigFromBaseConfig(BackendProperties baseProperties, BackendProperties backendProperties) {
		RetryConfig baseConfig = buildRetryConfig(RetryConfig.custom(), baseProperties);
		return buildRetryConfig(RetryConfig.from(baseConfig), backendProperties);
	}

	/**
	 * @param backend retry backend name
	 * @return the configured spring backend properties
	 */
	@Nullable
	public BackendProperties getBackendProperties(String backend) {
		return backends.get(backend);
	}

	/**
	 * @param properties the configured spring backend properties
	 * @return retry config builder instance
	 */
	@SuppressWarnings("unchecked")
	private RetryConfig buildRetryConfig(RetryConfig.Builder builder, BackendProperties properties) {
		if (properties == null) {
			return builder.build();
		}

		if (properties.enableExponentialBackoff && properties.enableRandomizedWait) {
			throw new IllegalStateException("you can not enable Exponential backoff policy and randomized delay at the same time , please enable only one of them");
		}

		configureRetryIntervalFunction(properties, builder);

		if (properties.getMaxRetryAttempts() != null && properties.getMaxRetryAttempts() != 0) {
			builder.maxAttempts(properties.getMaxRetryAttempts());
		}

		if (properties.getRetryExceptionPredicate() != null) {
			builder.retryOnException(BeanUtils.instantiateClass(properties.getRetryExceptionPredicate()));
		}

		if (properties.getIgnoreExceptions() != null) {
			builder.ignoreExceptions(properties.getIgnoreExceptions());
		}

		if (properties.getRetryExceptions() != null) {
			builder.retryExceptions(properties.getRetryExceptions());
		}

		if (properties.getResultPredicate() != null) {
			builder.retryOnResult(BeanUtils.instantiateClass(properties.getResultPredicate()));
		}

		return builder.build();
	}

	/**
	 * decide which retry delay polciy will be configured based into the configured properties
	 *
	 * @param properties the backend retry properties
	 * @param builder    the retry config builder
	 */
	private void configureRetryIntervalFunction(BackendProperties properties, RetryConfig.Builder<Object> builder) {
		if (properties.getWaitDuration() != null && properties.getWaitDuration() != 0) {
			long waitDuration = properties.getWaitDuration();
			if (properties.getEnableExponentialBackoff()) {
				if (properties.getExponentialBackoffMultiplier() != 0) {
					builder.intervalFunction(IntervalFunction.ofExponentialBackoff(waitDuration, properties.getExponentialBackoffMultiplier()));
				} else {
					builder.intervalFunction(IntervalFunction.ofExponentialBackoff(properties.getWaitDuration()));
				}
			} else if (properties.getEnableRandomizedWait()) {
				if (properties.getRandomizedWaitFactor() != 0) {
					builder.intervalFunction(IntervalFunction.ofRandomized(waitDuration, properties.getRandomizedWaitFactor()));
				} else {
					builder.intervalFunction(IntervalFunction.ofRandomized(waitDuration));
				}
			} else {
				builder.waitDuration(Duration.ofMillis(properties.getWaitDuration()));
			}
		}
	}

	/**
	 * Class storing property values for configuring {@link io.github.resilience4j.retry.Retry} instances.
	 */
	public static class BackendProperties {
		/*
		 * wait long value for the next try
		 */
		@Min(100)
		@Nullable
		private Long waitDuration;
		/*
		 * max retry attempts value
		 */
		@Min(1)
		@Nullable
		private Integer maxRetryAttempts;
		/*
		 * retry exception predicate class to be used to evaluate the exception to retry or not
		 */
		@Nullable
		private Class<? extends Predicate<Throwable>> retryExceptionPredicate;
		/*
		 * retry resultPredicate predicate class to be used to evaluate the result to retry or not
		 */
		@Nullable
		private Class<? extends Predicate> resultPredicate;
		/*
		 * list of retry exception classes
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		private Class<? extends Throwable>[] retryExceptions;
		/*
		 * list of retry ignored exception classes
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		private Class<? extends Throwable>[] ignoreExceptions;
		/*
		 * event buffer size for generated retry events
		 */
		@Min(1)
		private Integer eventConsumerBufferSize = 100;
		/*
		 * flag to enable Exponential backoff policy or not for retry policy delay
		 */
		@NotNull
		private Boolean enableExponentialBackoff = false;
		/*
		 * exponential backoff multiplier value
		 */
		private double exponentialBackoffMultiplier;
		@NotNull
		/*
		 * flag to enable randomized delay  policy or not for retry policy delay
		 */
		private Boolean enableRandomizedWait = false;
		/*
		 * randomized delay factor value
		 */
		private double randomizedWaitFactor;

		@Nullable
		private String baseConfig;

		@Nullable
		public Long getWaitDuration() {
			return waitDuration;
		}

		public void setWaitDuration(Long waitDuration) {
			this.waitDuration = waitDuration;
		}

		@Nullable
		public Integer getMaxRetryAttempts() {
			return maxRetryAttempts;
		}

		public void setMaxRetryAttempts(Integer maxRetryAttempts) {
			this.maxRetryAttempts = maxRetryAttempts;
		}

		@Nullable
		public Class<? extends Predicate<Throwable>> getRetryExceptionPredicate() {
			return retryExceptionPredicate;
		}

		public void setRetryExceptionPredicate(Class<? extends Predicate<Throwable>> retryExceptionPredicate) {
			this.retryExceptionPredicate = retryExceptionPredicate;
		}

		@Nullable
		public Class<? extends Predicate> getResultPredicate() {
			return resultPredicate;
		}

		public void setResultPredicate(Class<? extends Predicate> resultPredicate) {
			this.resultPredicate = resultPredicate;
		}

		@Nullable
		public Class<? extends Throwable>[] getRetryExceptions() {
			return retryExceptions;
		}

		public void setRetryExceptions(Class<? extends Throwable>[] retryExceptions) {
			this.retryExceptions = retryExceptions;
		}

		@Nullable
		public Class<? extends Throwable>[] getIgnoreExceptions() {
			return ignoreExceptions;
		}

		public void setIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
			this.ignoreExceptions = ignoreExceptions;
		}

		@Nullable
		public Integer getEventConsumerBufferSize() {
			return eventConsumerBufferSize;
		}

		public void setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
			this.eventConsumerBufferSize = eventConsumerBufferSize;
		}

		public Boolean getEnableExponentialBackoff() {
			return enableExponentialBackoff;
		}

		public void setEnableExponentialBackoff(Boolean enableExponentialBackoff) {
			this.enableExponentialBackoff = enableExponentialBackoff;
		}

		@Nullable
		public double getExponentialBackoffMultiplier() {
			return exponentialBackoffMultiplier;
		}

		public void setExponentialBackoffMultiplier(double exponentialBackoffMultiplier) {
			this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
		}

		@Nullable
		public Boolean getEnableRandomizedWait() {
			return enableRandomizedWait;
		}

		public void setEnableRandomizedWait(Boolean enableRandomizedWait) {
			this.enableRandomizedWait = enableRandomizedWait;
		}

		@Nullable
		public double getRandomizedWaitFactor() {
			return randomizedWaitFactor;
		}

		public void setRandomizedWaitFactor(double randomizedWaitFactor) {
			this.randomizedWaitFactor = randomizedWaitFactor;
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
