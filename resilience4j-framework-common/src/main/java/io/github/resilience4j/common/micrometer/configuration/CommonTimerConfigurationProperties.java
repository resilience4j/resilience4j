/*
 * Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.common.micrometer.configuration;

import io.github.resilience4j.common.CommonProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.utils.ConfigUtils;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.micrometer.TimerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.github.resilience4j.core.ClassUtils.instantiateFunction;
import static io.github.resilience4j.micrometer.TimerConfig.custom;
import static io.github.resilience4j.micrometer.TimerConfig.from;
import static java.util.Objects.requireNonNull;

/**
 * Main spring properties for timer configuration
 */
public class CommonTimerConfigurationProperties extends CommonProperties {

    private static final String DEFAULT = "default";
    private final Map<String, InstanceProperties> instances = new HashMap<>();
    private final Map<String, InstanceProperties> configs = new HashMap<>();

    /**
     * @param instance instance name
     * @return the timer configuration
     */
    public TimerConfig createTimerConfig(String instance, CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer) {
        return createTimerConfig(instances.get(instance), compositeTimerCustomizer, instance);
    }

    /**
     * @param instance timer instance name
     * @return the configured spring instance properties
     */
    @Nullable
    public InstanceProperties getInstanceProperties(String instance) {
        InstanceProperties instanceProperties = instances.get(instance);
        if (instanceProperties == null) {
            instanceProperties = configs.get(DEFAULT);
        } else if (configs.get(DEFAULT) != null) {
            ConfigUtils.mergePropertiesIfAny(configs.get(DEFAULT), instanceProperties);
        }
        return instanceProperties;
    }

    /**
     * @return the configured timer instance properties
     */
    public Map<String, InstanceProperties> getInstances() {
        return instances;
    }

    /**
     * @return common configuration for timer instance
     */
    public Map<String, InstanceProperties> getConfigs() {
        return configs;
    }

    /**
     * @param instanceProperties the timer instance spring properties
     * @return the timer configuration
     */
    public TimerConfig createTimerConfig(@Nullable InstanceProperties instanceProperties,
                                         CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer, String instanceName) {
        TimerConfig baseConfig = null;
        if (instanceProperties != null && StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
            InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
            }
            ConfigUtils.mergePropertiesIfAny(baseProperties, instanceProperties);
            baseConfig = createTimerConfig(baseProperties, compositeTimerCustomizer, instanceProperties.getBaseConfig());
        } else if (!instanceName.equals(DEFAULT) && configs.get(DEFAULT) != null) {
            if (instanceProperties != null) {
                ConfigUtils.mergePropertiesIfAny(configs.get(DEFAULT), instanceProperties);
            }
            baseConfig = createTimerConfig(configs.get(DEFAULT), compositeTimerCustomizer, DEFAULT);
        }
        return buildConfig(baseConfig != null ? from(baseConfig) : custom(), instanceProperties, compositeTimerCustomizer, instanceName);
    }

    /**
     * @param properties the configured spring instance properties
     * @return timer config builder instance
     */
    @SuppressWarnings("unchecked")
    private TimerConfig buildConfig(TimerConfig.Builder builder,
                                    @Nullable InstanceProperties properties,
                                    CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer,
                                    String instance) {
        if (properties != null) {
            if (properties.getMetricNames() != null) {
                builder.metricNames(properties.getMetricNames());
            }
            if (properties.getSuccessResultNameResolver() != null) {
                Function<Object, String> function = instantiateFunction(properties.getSuccessResultNameResolver());
                builder.successResultNameResolver(function);
            }
            if (properties.getFailureResultNameResolver() != null) {
                Function<Throwable, String> function = instantiateFunction(properties.getFailureResultNameResolver());
                builder.failureResultNameResolver(function);
            }
        }
        compositeTimerCustomizer.getCustomizer(instance).ifPresent(customizer -> customizer.customize(builder));
        return builder.build();
    }

    /**
     * Class storing property values for configuring {@link io.github.resilience4j.micrometer.Timer}
     * instances.
     */
    public static class InstanceProperties {

        /**
         * The metric names
         */
        @Nullable
        private String metricNames;

        /**
         * The function that resolves a result name from the output returned from the decorated operation.
         */
        @Nullable
        private Class<? extends Function<Object, String>> successResultNameResolver;

        /**
         * The function that resolves a result name from the exception thrown from the decorated operation.
         */
        @Nullable
        private Class<? extends Function<Throwable, String>> failureResultNameResolver;

        @Nullable
        private String baseConfig;

        /**
         * event buffer size for generated timer events
         */
        @Nullable
        private Integer eventConsumerBufferSize;

        @Nullable
        public String getMetricNames() {
            return metricNames;
        }

        public InstanceProperties setMetricNames(@Nullable String metricNames) {
            this.metricNames = metricNames;
            return this;
        }

        @Nullable
        public Class<? extends Function<Object, String>> getSuccessResultNameResolver() {
            return successResultNameResolver;
        }

        public InstanceProperties setSuccessResultNameResolver(@Nullable Class<? extends Function<Object, String>> successResultNameResolver) {
            this.successResultNameResolver = successResultNameResolver;
            return this;
        }

        @Nullable
        public Class<? extends Function<Throwable, String>> getFailureResultNameResolver() {
            return failureResultNameResolver;
        }

        public InstanceProperties setFailureResultNameResolver(@Nullable Class<? extends Function<Throwable, String>> failureResultNameResolver) {
            this.failureResultNameResolver = failureResultNameResolver;
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

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

        public InstanceProperties setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
            requireNonNull(eventConsumerBufferSize, "eventConsumerBufferSize must not be null");
            if (eventConsumerBufferSize < 1) {
                throw new IllegalArgumentException("eventConsumerBufferSize must be greater than or equal to 1");
            }
            this.eventConsumerBufferSize = eventConsumerBufferSize;
            return this;
        }
    }
}
