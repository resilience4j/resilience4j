/*
 * Copyright 2020 Ingyu Hwang
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

package io.github.resilience4j.common.timelimiter.configuration;

import io.github.resilience4j.common.CommonProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.utils.ConfigUtils;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TimeLimiterConfigurationProperties extends CommonProperties {

    private final Map<String, InstanceProperties> instances = new HashMap<>();
    private final Map<String, InstanceProperties> configs = new HashMap<>();

    public Map<String, InstanceProperties> getInstances() {
        return instances;
    }

    public Map<String, InstanceProperties> getConfigs() {
        return configs;
    }

    /**
     * @param backend timeLimiter backend name
     * @return the configured spring backend properties
     */
    @Nullable
    public InstanceProperties getInstanceProperties(String backend) {
        return instances.get(backend);
    }

    public TimeLimiterConfig createTimeLimiterConfig(String backendName,
        @Nullable InstanceProperties instanceProperties,
        CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        if (instanceProperties == null) {
            return TimeLimiterConfig.ofDefaults();
        }
        if (StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
            InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
            }
            return buildConfigFromBaseConfig(baseProperties, instanceProperties,
                compositeTimeLimiterCustomizer, backendName);
        }
        return buildTimeLimiterConfig(TimeLimiterConfig.custom(), instanceProperties,
            compositeTimeLimiterCustomizer, backendName);
    }

    private static TimeLimiterConfig buildConfigFromBaseConfig(
        InstanceProperties baseProperties, InstanceProperties instanceProperties,
        CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer, String backendName) {

        ConfigUtils.mergePropertiesIfAny(baseProperties, instanceProperties);
        TimeLimiterConfig baseConfig = buildTimeLimiterConfig(TimeLimiterConfig.custom(), baseProperties,
            compositeTimeLimiterCustomizer, backendName);
        return buildTimeLimiterConfig(TimeLimiterConfig.from(baseConfig), instanceProperties,
            compositeTimeLimiterCustomizer, backendName);
    }

    private static TimeLimiterConfig buildTimeLimiterConfig(
        TimeLimiterConfig.Builder builder, @Nullable InstanceProperties instanceProperties,
        CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer, String backendName) {

        if (instanceProperties == null) {
            return builder.build();
        }

        if (instanceProperties.getTimeoutDuration() != null) {
            builder.timeoutDuration(instanceProperties.getTimeoutDuration());
        }

        if (instanceProperties.getCancelRunningFuture() != null) {
            builder.cancelRunningFuture(instanceProperties.getCancelRunningFuture());
        }

        compositeTimeLimiterCustomizer.getCustomizer(backendName).ifPresent(
            timeLimiterConfigCustomizer -> timeLimiterConfigCustomizer.customize(builder));

        return builder.build();
    }

    public TimeLimiterConfig createTimeLimiterConfig(String limiter) {
        return createTimeLimiterConfig(limiter, getInstanceProperties(limiter),
            new CompositeCustomizer<>(Collections.emptyList()));
    }

    public static class InstanceProperties {

        private Duration timeoutDuration;
        private Boolean cancelRunningFuture;
        @Nullable
        private Integer eventConsumerBufferSize;

        @Nullable
        private String baseConfig;

        public Duration getTimeoutDuration() {
            return timeoutDuration;
        }

        public InstanceProperties setTimeoutDuration(Duration timeoutDuration) {
            Objects.requireNonNull(timeoutDuration);
            if (timeoutDuration.toMillis() < 0) {
                throw new IllegalArgumentException(
                        "timeoutDuration must be greater than or equal to 0.");
            }
            this.timeoutDuration = timeoutDuration;
            return this;
        }

        public Boolean getCancelRunningFuture() {
            return cancelRunningFuture;
        }

        public InstanceProperties setCancelRunningFuture(Boolean cancelRunningFuture) {
            this.cancelRunningFuture = cancelRunningFuture;
            return this;
        }

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

        public InstanceProperties setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
            Objects.requireNonNull(eventConsumerBufferSize);
            if (eventConsumerBufferSize < 1) {
                throw new IllegalArgumentException("eventConsumerBufferSize must be greater than or equal to 1.");
            }

            this.eventConsumerBufferSize = eventConsumerBufferSize;
            return this;
        }

        @Nullable
        public String getBaseConfig() {
            return baseConfig;
        }

        public InstanceProperties setBaseConfig(@Nullable String baseConfig) {
            this.baseConfig = baseConfig;
            return this;
        }
    }
}
