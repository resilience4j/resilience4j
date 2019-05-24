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
package io.github.resilience4j.ratpack.ratelimiter;

import com.google.common.base.Strings;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class RateLimiterConfigurationProperties {

	private Map<String, BackendProperties> backends = new HashMap<>();
	private Map<String, BackendProperties> configs = new HashMap<>();

	public RateLimiterConfig createRateLimiterConfig(@Nullable BackendProperties backendProperties) {
		if (backendProperties == null) {
			return RateLimiterConfig.ofDefaults();
		}
		if (!Strings.isNullOrEmpty(backendProperties.getBaseConfig())) {
			BackendProperties baseProperties = configs.get(backendProperties.baseConfig);
			if (baseProperties == null) {
				throw new ConfigurationNotFoundException(backendProperties.getBaseConfig());
			}
			return buildConfigFromBaseConfig(baseProperties, backendProperties);
		}
		return buildRateLimiterConfig(RateLimiterConfig.custom(), backendProperties);
	}

	private RateLimiterConfig buildConfigFromBaseConfig(BackendProperties baseProperties, BackendProperties backendProperties) {
		RateLimiterConfig baseConfig = buildRateLimiterConfig(RateLimiterConfig.custom(), baseProperties);
		return buildRateLimiterConfig(RateLimiterConfig.from(baseConfig), backendProperties);
	}

	private RateLimiterConfig buildRateLimiterConfig(RateLimiterConfig.Builder builder, @Nullable BackendProperties backendProperties) {
		if (backendProperties == null) {
			return RateLimiterConfig.ofDefaults();
		}

		if (backendProperties.getLimitForPeriod() != null) {
			builder.limitForPeriod(backendProperties.getLimitForPeriod());
		}

		if (backendProperties.getLimitRefreshPeriodInNanos() != null) {
			builder.limitRefreshPeriod(Duration.ofNanos(backendProperties.getLimitRefreshPeriodInNanos()));
		}

		if (backendProperties.getTimeoutInMillis() != null) {
			builder.timeoutDuration(Duration.ofMillis(backendProperties.getTimeoutInMillis()));
		}

		return builder.build();
	}

	private BackendProperties getLimiterProperties(String limiter) {
		return backends.get(limiter);
	}

	public RateLimiterConfig createRateLimiterConfig(String limiter) {
		return createRateLimiterConfig(getLimiterProperties(limiter));
	}

	@Nullable
	public BackendProperties getBackendProperties(String backend) {
		return backends.get(backend);
	}

	public Map<String, BackendProperties> getBackends() {
		return backends;
	}

	public Map<String, BackendProperties> getConfigs() {
		return configs;
	}

	/**
	 * Class storing property values for configuring {@link RateLimiterConfig} instances.
	 */
	public static class BackendProperties {

		private Integer limitForPeriod;
		private Integer limitRefreshPeriodInNanos;
		private Integer timeoutInMillis;
		private Boolean subscribeForEvents = false;
		private Boolean registerHealthIndicator = false;
		private Integer eventConsumerBufferSize = 100;
		@Nullable
		private String baseConfig;

		/**
		 * Configures the permissions limit for refresh period.
		 * Count of permissions available during one rate limiter period
		 * specified by {@link RateLimiterConfig#getLimitRefreshPeriod()} value.
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
		 * specified by {@link RateLimiterConfig#getLimitRefreshPeriod()} value.
		 * Default value is 50.
		 *
		 * @param limitForPeriod the permissions limit for refresh period
		 */
		public BackendProperties limitForPeriod(Integer limitForPeriod) {
			this.limitForPeriod = limitForPeriod;
			return this;
		}

		/**
		 * Configures the period of limit refresh.
		 * After each period rate limiter sets its permissions
		 * count to {@link RateLimiterConfig#getLimitForPeriod()} value.
		 * Default value is 500 nanoseconds.
		 *
		 * @return the period of limit refresh
		 */
		@Nullable
		public Integer getLimitRefreshPeriodInNanos() {
			return limitRefreshPeriodInNanos;
		}

		/**
		 * Configures the period of limit refresh.
		 * After each period rate limiter sets its permissions
		 * count to {@link RateLimiterConfig#getLimitForPeriod()} value.
		 * Default value is 500 nanoseconds.
		 *
		 * @param limitRefreshPeriodInNanos the period of limit refresh
		 */
		public BackendProperties limitRefreshPeriodInNanos(Integer limitRefreshPeriodInNanos) {
			this.limitRefreshPeriodInNanos = limitRefreshPeriodInNanos;
			return this;
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
		public BackendProperties timeoutInMillis(Integer timeoutInMillis) {
			this.timeoutInMillis = timeoutInMillis;
			return this;
		}

		public Boolean getSubscribeForEvents() {
			return subscribeForEvents;
		}

		public BackendProperties subscribeForEvents(Boolean subscribeForEvents) {
			this.subscribeForEvents = subscribeForEvents;
			return this;
		}

		public Integer getEventConsumerBufferSize() {
			return eventConsumerBufferSize;
		}

		public BackendProperties eventConsumerBufferSize(Integer eventConsumerBufferSize) {
			this.eventConsumerBufferSize = eventConsumerBufferSize;
			return this;
		}

		public Boolean getRegisterHealthIndicator() {
			return registerHealthIndicator;
		}

		public BackendProperties registerHealthIndicator(Boolean registerHealthIndicator) {
			this.registerHealthIndicator = registerHealthIndicator;
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
		public BackendProperties baseConfig(String baseConfig) {
			this.baseConfig = baseConfig;
			return this;
		}
	}

}
