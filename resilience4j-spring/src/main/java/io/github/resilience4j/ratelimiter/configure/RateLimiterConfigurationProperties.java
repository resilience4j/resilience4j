/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.configure;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

public class RateLimiterConfigurationProperties {
	// This property gives you control over RateLimiter aspect application order.
	// Integer.MAX_VALUE means that RateLimiter aspect will be last one applied to any decorated bean.
	// It also means that by default RateLimiter will be executed AFTER CircuitBreaker.
	// Be adjusting RateLimiterProperties.rateLimiterAspectOrder and CircuitBreakerProperties.circuitBreakerAspectOrder
	// you explicitly define aspects CircuitBreaker and RateLimiter execution sequence.
	private int rateLimiterAspectOrder = Integer.MAX_VALUE;
	private Map<String, LimiterProperties> limiters = new HashMap<>();
	private Map<String, LimiterProperties> configs = new HashMap<>();

	public RateLimiterConfig createRateLimiterConfig(@Nullable LimiterProperties limiterProperties) {
		if (limiterProperties == null) {
			return RateLimiterConfig.ofDefaults();
		}
		if (!StringUtils.isEmpty(limiterProperties.getBaseConfig())) {
			LimiterProperties baseProperties = configs.get(limiterProperties.baseConfig);
			if (baseProperties == null) {
				throw new ConfigurationNotFoundException(limiterProperties.getBaseConfig());
			}
			return buildConfigFromBaseConfig(baseProperties, limiterProperties);
		}
		return buildRateLimiterConfig(RateLimiterConfig.custom(), limiterProperties);
	}

	private RateLimiterConfig buildConfigFromBaseConfig(LimiterProperties baseProperties, LimiterProperties backendProperties) {
		RateLimiterConfig baseConfig = buildRateLimiterConfig(RateLimiterConfig.custom(), baseProperties);
		return buildRateLimiterConfig(RateLimiterConfig.from(baseConfig), backendProperties);
	}

	private RateLimiterConfig buildRateLimiterConfig(RateLimiterConfig.Builder builder, @Nullable LimiterProperties limiterProperties) {
		if (limiterProperties == null) {
			return RateLimiterConfig.ofDefaults();
		}

		if (limiterProperties.getLimitForPeriod() != null) {
			builder.limitForPeriod(limiterProperties.getLimitForPeriod());
		}

		if (limiterProperties.getLimitRefreshPeriodInMillis() != null) {
			builder.limitRefreshPeriod(Duration.ofMillis(limiterProperties.getLimitRefreshPeriodInMillis()));
		}

		if (limiterProperties.getTimeoutInMillis() != null) {
			builder.timeoutDuration(Duration.ofMillis(limiterProperties.getTimeoutInMillis()));
		}

		return builder.build();
	}

	private LimiterProperties getLimiterProperties(String limiter) {
		return limiters.get(limiter);
	}

	public RateLimiterConfig createRateLimiterConfig(String limiter) {
		return createRateLimiterConfig(getLimiterProperties(limiter));
	}

	public int getRateLimiterAspectOrder() {
		return rateLimiterAspectOrder;
	}

	public void setRateLimiterAspectOrder(int rateLimiterAspectOrder) {
		this.rateLimiterAspectOrder = rateLimiterAspectOrder;
	}

	public Map<String, LimiterProperties> getLimiters() {
		return limiters;
	}

	public Map<String, LimiterProperties> getConfigs() {
		return configs;
	}

	/**
	 * Class storing property values for configuring {@link RateLimiterConfig} instances.
	 */
	public static class LimiterProperties {

		private Integer limitForPeriod;
		private Integer limitRefreshPeriodInMillis;
		private Integer timeoutInMillis;
		private Boolean subscribeForEvents = false;
		private Boolean registerHealthIndicator = false;
		private Integer eventConsumerBufferSize = 100;
		@Nullable
		private String baseConfig;

		/**
		 * Configures the permissions limit for refresh period.
		 * Count of permissions available during one rate limiter period
		 * specified by {@link RateLimiterConfig#limitRefreshPeriod} value.
		 * Default value is 50.
		 *
		 * @return the permissions limit for refresh period
		 */
		@Nullable
		public Integer getLimitForPeriod() {
			return limitForPeriod;
		}

		/**
		 * Configures the permissions limit for refresh period.
		 * Count of permissions available during one rate limiter period
		 * specified by {@link RateLimiterConfig#limitRefreshPeriod} value.
		 * Default value is 50.
		 *
		 * @param limitForPeriod the permissions limit for refresh period
		 */
		public void setLimitForPeriod(Integer limitForPeriod) {
			this.limitForPeriod = limitForPeriod;
		}

		/**
		 * Configures the period of limit refresh.
		 * After each period rate limiter sets its permissions
		 * count to {@link RateLimiterConfig#limitForPeriod} value.
		 * Default value is 500 nanoseconds.
		 *
		 * @return the period of limit refresh
		 */
		@Nullable
		public Integer getLimitRefreshPeriodInMillis() {
			return limitRefreshPeriodInMillis;
		}

		/**
		 * Configures the period of limit refresh.
		 * After each period rate limiter sets its permissions
		 * count to {@link RateLimiterConfig#limitForPeriod} value.
		 * Default value is 500 nanoseconds.
		 *
		 * @param limitRefreshPeriodInMillis the period of limit refresh
		 */
		public void setLimitRefreshPeriodInMillis(Integer limitRefreshPeriodInMillis) {
			this.limitRefreshPeriodInMillis = limitRefreshPeriodInMillis;
		}

		/**
		 * Configures the default wait for permission duration.
		 * Default value is 5 seconds.
		 *
		 * @return wait for permission duration
		 */
		@Nullable
		public Integer getTimeoutInMillis() {
			return timeoutInMillis;
		}

		/**
		 * Configures the default wait for permission duration.
		 * Default value is 5 seconds.
		 *
		 * @param timeoutInMillis wait for permission duration
		 */
		public void setTimeoutInMillis(Integer timeoutInMillis) {
			this.timeoutInMillis = timeoutInMillis;
		}

		public Boolean getSubscribeForEvents() {
			return subscribeForEvents;
		}

		public void setSubscribeForEvents(Boolean subscribeForEvents) {
			this.subscribeForEvents = subscribeForEvents;
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
