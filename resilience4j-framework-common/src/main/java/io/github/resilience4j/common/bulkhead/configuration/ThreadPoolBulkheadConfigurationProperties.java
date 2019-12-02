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
package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ThreadPoolBulkheadConfigurationProperties {

    /**
     * The Optional configured global registry tags if any that can be used with the exported
     * metrics
     */
    private Map<String, String> tags = new HashMap<>();
    private Map<String, InstanceProperties> instances = new HashMap<>();
    private Map<String, InstanceProperties> configs = new HashMap<>();

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

    @Nullable
    public InstanceProperties getBackendProperties(String backend) {
        return instances.get(backend);
    }

    /**
     * @return the Optional configured registry global tags if any that can be used with the
     * exported metrics
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * @param tags the optional configured tags values into registry
     */
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    // Thread pool bulkhead section
    public ThreadPoolBulkheadConfig createThreadPoolBulkheadConfig(String backend) {
        return createThreadPoolBulkheadConfig(getBackendProperties(backend));
    }

    public ThreadPoolBulkheadConfig createThreadPoolBulkheadConfig(
        InstanceProperties instanceProperties) {
        if (instanceProperties != null && StringUtils
            .isNotEmpty(instanceProperties.getBaseConfig())) {
            InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
            }
            return buildThreadPoolConfigFromBaseConfig(baseProperties, instanceProperties);
        }
        return buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom(), instanceProperties);
    }

    private ThreadPoolBulkheadConfig buildThreadPoolConfigFromBaseConfig(
        InstanceProperties baseProperties, InstanceProperties instanceProperties) {
        ThreadPoolBulkheadConfig baseConfig = buildThreadPoolBulkheadConfig(
            ThreadPoolBulkheadConfig.custom(), baseProperties);
        return buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.from(baseConfig),
            instanceProperties);
    }

    public ThreadPoolBulkheadConfig buildThreadPoolBulkheadConfig(
        ThreadPoolBulkheadConfig.Builder builder, InstanceProperties properties) {
        if (properties == null) {
            return ThreadPoolBulkheadConfig.custom().build();
        }

        if (properties.getQueueCapacity() > 0) {
            builder.queueCapacity(properties.getQueueCapacity());
        }
        if (properties.getCoreThreadPoolSize() > 0) {
            builder.coreThreadPoolSize(properties.getCoreThreadPoolSize());
        }
        if (properties.getMaxThreadPoolSize() > 0) {
            builder.maxThreadPoolSize(properties.getMaxThreadPoolSize());
        }
        if (properties.getKeepAliveDuration() != null) {
            builder.keepAliveDuration(properties.getKeepAliveDuration());
        }
        if (properties.getWritableStackTraceEnabled() != null) {
            builder.writableStackTraceEnabled(properties.getWritableStackTraceEnabled());
        }

        return builder.build();
    }


    /**
     * Class storing property values for configuring {@link Bulkhead} instances.
     */
    public static class InstanceProperties {

        @Nullable
        private Integer eventConsumerBufferSize;

        @Nullable
        private String baseConfig;

        @Nullable
        private Boolean writableStackTraceEnabled;

        private int maxThreadPoolSize;
        private int coreThreadPoolSize;
        private int queueCapacity;
        private Duration keepAliveDuration;

        public int getMaxThreadPoolSize() {
            return maxThreadPoolSize;
        }

        public InstanceProperties setMaxThreadPoolSize(int maxThreadPoolSize) {
            this.maxThreadPoolSize = maxThreadPoolSize;
            return this;
        }

        public int getCoreThreadPoolSize() {
            return coreThreadPoolSize;
        }

        public InstanceProperties setCoreThreadPoolSize(int coreThreadPoolSize) {
            this.coreThreadPoolSize = coreThreadPoolSize;
            return this;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public InstanceProperties setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }


        public Duration getKeepAliveDuration() {
            return keepAliveDuration;
        }

        public InstanceProperties setKeepAliveDuration(Duration keepAliveDuration) {
            this.keepAliveDuration = keepAliveDuration;
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

        public Boolean getWritableStackTraceEnabled() {
            return writableStackTraceEnabled;
        }

        public InstanceProperties setWritableStackTraceEnabled(Integer eventConsumerBufferSize) {
            this.eventConsumerBufferSize = eventConsumerBufferSize;
            return this;
        }

        /**
         * Gets the shared configuration name. If this is set, the configuration builder will use
         * the the shared configuration backend over this one.
         *
         * @return The shared configuration name.
         */
        @Nullable
        public String getBaseConfig() {
            return baseConfig;
        }

        /**
         * Sets the shared configuration name. If this is set, the configuration builder will use
         * the the shared configuration backend over this one.
         *
         * @param baseConfig The shared configuration name.
         */
        public InstanceProperties setBaseConfig(String baseConfig) {
            this.baseConfig = baseConfig;
            return this;
        }
    }
}
