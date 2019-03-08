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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.beans.BeanUtils;

import io.github.resilience4j.retry.RetryConfig;

/**
 * Main spring properties for retry configuration
 */
public class RetryConfigurationProperties {
	/* This property gives you control over Retry aspect application order.
	By default Retry will be executed BEFORE Circuit breaker and rateLimiter.
	By adjusting RateLimiterProperties.rateLimiterAspectOrder and CircuitBreakerProperties.circuitBreakerAspectOrder
	you explicitly define aspects CircuitBreaker and RateLimiter execution sequence.*/
	private int retryAspectOrder = Integer.MAX_VALUE - 1;
	private final Map<String, BackendProperties> backends = new HashMap<>();

	/**
	 * @param backend backend name
	 * @return the retry configuration
	 */
	public RetryConfig createRetryBreakerConfig(String backend) {
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
	 * @param backendProperties the retry backend spring properties
	 * @return the retry configuration
	 */
	private RetryConfig createRetryConfig(BackendProperties backendProperties) {
		return buildRetryConfig(backendProperties).build();
	}

	/**
	 * @param backend retry backend name
	 * @return the configured spring backend properties
	 */
	private BackendProperties getBackendProperties(String backend) {
		return backends.get(backend);
	}

	/**
	 * @param properties the configured spring backend properties
	 * @return retry config builder instance
	 */
	public RetryConfig.Builder buildRetryConfig(BackendProperties properties) {
		if (properties == null) {
			return new RetryConfig.Builder();
		}

		RetryConfig.Builder<Object> builder = RetryConfig.custom();

		if (properties.getWaitDuration() != 0) {
			builder.waitDuration(Duration.ofMillis(properties.getWaitDuration()));
		}

		if (properties.getMaxRetryAttempts() != 0) {
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

		return builder;
	}

	/**
	 * Class storing property values for configuring {@link io.github.resilience4j.retry.Retry} instances.
	 */
	public static class BackendProperties {
		@Min(100)
		private Long waitDuration;
		@Min(1)
		private Integer maxRetryAttempts;
		private Class<? extends Predicate<Throwable>> retryExceptionPredicate;
		private Class<? extends Predicate> resultPredicate;
		@SuppressWarnings("unchecked")
		private Class<? extends Throwable>[] retryExceptions;
		@SuppressWarnings("unchecked")
		private Class<? extends Throwable>[] ignoreExceptions;
		@Min(1)
		private Integer eventConsumerBufferSize = 100;
		@NotNull
		private Boolean registerHealthIndicator = true;

		public Long getWaitDuration() {
			return waitDuration;
		}

		public void setWaitDuration(Long waitDuration) {
			this.waitDuration = waitDuration;
		}

		public Integer getMaxRetryAttempts() {
			return maxRetryAttempts;
		}

		public void setMaxRetryAttempts(Integer maxRetryAttempts) {
			this.maxRetryAttempts = maxRetryAttempts;
		}

		public Class<? extends Predicate<Throwable>> getRetryExceptionPredicate() {
			return retryExceptionPredicate;
		}

		public void setRetryExceptionPredicate(Class<? extends Predicate<Throwable>> retryExceptionPredicate) {
			this.retryExceptionPredicate = retryExceptionPredicate;
		}

		public Class<? extends Predicate> getResultPredicate() {
			return resultPredicate;
		}

		public void setResultPredicate(Class<? extends Predicate> resultPredicate) {
			this.resultPredicate = resultPredicate;
		}

		public Class<? extends Throwable>[] getRetryExceptions() {
			return retryExceptions;
		}

		public void setRetryExceptions(Class<? extends Throwable>[] retryExceptions) {
			this.retryExceptions = retryExceptions;
		}

		public Class<? extends Throwable>[] getIgnoreExceptions() {
			return ignoreExceptions;
		}

		public void setIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
			this.ignoreExceptions = ignoreExceptions;
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

	}

}
