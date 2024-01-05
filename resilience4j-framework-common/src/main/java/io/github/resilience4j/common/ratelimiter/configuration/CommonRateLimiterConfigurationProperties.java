/*
 * Copyright 2019 Dan Maas, Mahmoud Romeh
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
package io.github.resilience4j.common.ratelimiter.configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.resilience4j.common.CommonProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.utils.ConfigUtils;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import static io.github.resilience4j.ratelimiter.RateLimiterConfig.custom;
import static io.github.resilience4j.ratelimiter.RateLimiterConfig.from;

public class CommonRateLimiterConfigurationProperties extends CommonProperties {

    private static final String DEFAULT = "default";
    private Map<String, InstanceProperties> instances = new HashMap<>();
    private Map<String, InstanceProperties> configs = new HashMap<>();

    public Optional<InstanceProperties> findRateLimiterProperties(String name) {
        InstanceProperties instanceProperties = instances.get(name);
        if (instanceProperties == null) {
            instanceProperties = configs.get(DEFAULT);
        } else if (configs.get(DEFAULT) != null) {
            ConfigUtils.mergePropertiesIfAny(configs.get(DEFAULT), instanceProperties);
        }
        return Optional.ofNullable(instanceProperties);
    }

    public RateLimiterConfig createRateLimiterConfig(
        @Nullable InstanceProperties instanceProperties,
        CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer,
        String instanceName) {
        RateLimiterConfig baseConfig = null;
        if (instanceProperties != null && StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
            InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
            }
            ConfigUtils.mergePropertiesIfAny(baseProperties, instanceProperties);
            baseConfig = createRateLimiterConfig(baseProperties, compositeRateLimiterCustomizer, instanceProperties.getBaseConfig());
        } else if (!instanceName.equals(DEFAULT) && configs.get(DEFAULT) != null) {
            if (instanceProperties != null) {
                ConfigUtils.mergePropertiesIfAny(configs.get(DEFAULT), instanceProperties);
            }
            baseConfig = createRateLimiterConfig(configs.get(DEFAULT), compositeRateLimiterCustomizer, DEFAULT);
        }
        return buildConfig(baseConfig != null ? from(baseConfig) : custom(), instanceProperties, compositeRateLimiterCustomizer, instanceName);
    }

    private RateLimiterConfig buildConfig(RateLimiterConfig.Builder builder,
        @Nullable InstanceProperties instanceProperties,
        CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer,
        String instanceName) {
        if (instanceProperties != null) {
            if (instanceProperties.getLimitForPeriod() != null) {
                builder.limitForPeriod(instanceProperties.getLimitForPeriod());
            }

            if (instanceProperties.getLimitRefreshPeriod() != null) {
                builder.limitRefreshPeriod(instanceProperties.getLimitRefreshPeriod());
            }

            if (instanceProperties.getTimeoutDuration() != null) {
                builder.timeoutDuration(instanceProperties.getTimeoutDuration());
            }

            if (instanceProperties.getWritableStackTraceEnabled() != null) {
                builder.writableStackTraceEnabled(instanceProperties.getWritableStackTraceEnabled());
            }
        }
        compositeRateLimiterCustomizer.getCustomizer(instanceName).ifPresent(
            rateLimiterConfigCustomizer -> rateLimiterConfigCustomizer.customize(builder));
        return builder.build();
    }

    private InstanceProperties getLimiterProperties(String limiter) {
        return instances.get(limiter);
    }

    public RateLimiterConfig createRateLimiterConfig(String limiter,
        CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer) {
        return createRateLimiterConfig(getLimiterProperties(limiter),
            compositeRateLimiterCustomizer, limiter);
    }

    @Nullable
    public InstanceProperties getInstanceProperties(String instance) {
        return instances.get(instance);
    }

    public Map<String, InstanceProperties> getInstances() {
        return instances;
    }

    /**
     * For backwards compatibility when setting limiters in configuration properties.
     */
    public Map<String, InstanceProperties> getLimiters() {
        return instances;
    }

    public Map<String, InstanceProperties> getConfigs() {
        return configs;
    }

    /**
     * Class storing property values for configuring {@link RateLimiterConfig} instances.
     */
    public static class InstanceProperties {

        private Integer limitForPeriod;
        private Duration limitRefreshPeriod;
        private Duration timeoutDuration;
        @Nullable
        private Boolean subscribeForEvents;
        @Nullable
        private Boolean allowHealthIndicatorToFail;
        @Nullable
        private Boolean registerHealthIndicator;
        @Nullable
        private Integer eventConsumerBufferSize;
        @Nullable
        private Boolean writableStackTraceEnabled;
        @Nullable
        private String baseConfig;

        /**
         * Configures the permissions limit for refresh period. Count of permissions available
         * during one rate limiter period specified by {@link RateLimiterConfig#getLimitRefreshPeriod()}
         * value. Default value is 50.
         *
         * @return the permissions limit for refresh period
         */
        @Nullable
        public Integer getLimitForPeriod() {
            return limitForPeriod;
        }

        /**
         * Configures the permissions limit for refresh period. Count of permissions available
         * during one rate limiter period specified by {@link RateLimiterConfig#getLimitRefreshPeriod()}
         * value. Default value is 50.
         *
         * @param limitForPeriod the permissions limit for refresh period
         */
        public InstanceProperties setLimitForPeriod(Integer limitForPeriod) {
            if (limitForPeriod < 1) {
                throw new IllegalArgumentException(
                    "Illegal argument limitForPeriod: " + limitForPeriod + " is less than 1");
            }
            this.limitForPeriod = limitForPeriod;
            return this;
        }

        /**
         * Configures the period of limit refresh. After each period rate limiter sets its
         * permissions count to {@link RateLimiterConfig#getLimitForPeriod()} value. Default value
         * is 500 nanoseconds.
         *
         * @return the period of limit refresh
         */
        @Nullable
        public Duration getLimitRefreshPeriod() {
            return limitRefreshPeriod;
        }

        /**
         * Configures the period of limit refresh. After each period rate limiter sets its
         * permissions count to {@link RateLimiterConfig#getLimitForPeriod()} value. Default value
         * is 500 nanoseconds.
         *
         * @param limitRefreshPeriod the period of limit refresh
         */
        public InstanceProperties setLimitRefreshPeriod(Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = limitRefreshPeriod;
            return this;
        }

        /**
         * Configures the default wait for permission duration. Default value is 5 seconds.
         *
         * @return wait for permission duration
         */
        @Nullable
        public Duration getTimeoutDuration() {
            return timeoutDuration;
        }

        /**
         * Configures the default wait for permission duration. Default value is 5 seconds.
         *
         * @param timeout wait for permission duration
         */
        public InstanceProperties setTimeoutDuration(Duration timeout) {
            this.timeoutDuration = timeout;
            return this;
        }

        /**
         * Returns if we should enable writable stack traces or not.
         *
         * @return writableStackTraceEnabled if we should enable writable stack traces or not.
         */
        public Boolean getWritableStackTraceEnabled() {
            return this.writableStackTraceEnabled;
        }

        /**
         * Sets if we should enable writable stack traces or not.
         *
         * @param writableStackTraceEnabled The flag to enable writable stack traces.
         */
        public InstanceProperties setWritableStackTraceEnabled(Boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        public Boolean getSubscribeForEvents() {
            return subscribeForEvents;
        }

        public InstanceProperties setSubscribeForEvents(Boolean subscribeForEvents) {
            this.subscribeForEvents = subscribeForEvents;
            return this;
        }

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

        public InstanceProperties setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
            Objects.requireNonNull(eventConsumerBufferSize);
            if (eventConsumerBufferSize < 1) {
                throw new IllegalArgumentException(
                    "eventConsumerBufferSize must be greater than or equal to 1.");
            }

            this.eventConsumerBufferSize = eventConsumerBufferSize;
            return this;
        }

        /**
         * @return the flag that controls if health indicators are allowed to go into a failed
         * (DOWN) status.
         * @see #setAllowHealthIndicatorToFail(Boolean)
         */
        @Nullable
        public Boolean getAllowHealthIndicatorToFail() {
            return allowHealthIndicatorToFail;
        }

        /**
         * When set to true, it allows the health indicator to go to a failed (DOWN) status. By
         * default, health indicators for rate limiters will never go into an unhealthy state.
         *
         * @param allowHealthIndicatorToFail flag to control if the health indicator is allowed to
         *                                   fail
         * @return the InstanceProperties
         */
        public InstanceProperties setAllowHealthIndicatorToFail(
            Boolean allowHealthIndicatorToFail) {
            this.allowHealthIndicatorToFail = allowHealthIndicatorToFail;
            return this;
        }

        public Boolean getRegisterHealthIndicator() {
            return registerHealthIndicator;
        }

        public InstanceProperties setRegisterHealthIndicator(Boolean registerHealthIndicator) {
            this.registerHealthIndicator = registerHealthIndicator;
            return this;
        }

        /**
         * Gets the shared configuration name. If this is set, the configuration builder will use
         * the shared configuration instance over this one.
         *
         * @return The shared configuration name.
         */
        @Nullable
        public String getBaseConfig() {
            return baseConfig;
        }

        /**
         * Sets the shared configuration name. If this is set, the configuration builder will use
         * the shared configuration instance over this one.
         *
         * @param baseConfig The shared configuration name.
         */
        public InstanceProperties setBaseConfig(String baseConfig) {
            this.baseConfig = baseConfig;
            return this;
        }
    }

}
