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
package io.github.resilience4j.common.retry.configuration;

import io.github.resilience4j.common.CommonProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.utils.ConfigUtils;
import io.github.resilience4j.core.*;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static io.github.resilience4j.retry.RetryConfig.custom;
import static io.github.resilience4j.retry.RetryConfig.from;

/**
 * Main spring properties for retry configuration
 */
public class CommonRetryConfigurationProperties extends CommonProperties {

    private static final String DEFAULT = "default";
    private final Map<String, InstanceProperties> instances = new HashMap<>();
    private Map<String, InstanceProperties> configs = new HashMap<>();

    /**
     * @param backend backend name
     * @return the retry configuration
     */
    public RetryConfig createRetryConfig(String backend,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        return createRetryConfig(instances.get(backend), compositeRetryCustomizer, backend);
    }

    /**
     * @param backend retry backend name
     * @return the configured spring backend properties
     */
    @Nullable
    public InstanceProperties getBackendProperties(String backend) {
        InstanceProperties instanceProperties = instances.get(backend);
        if (instanceProperties == null) {
            instanceProperties = configs.get(DEFAULT);
        } else if (configs.get(DEFAULT) != null) {
            ConfigUtils.mergePropertiesIfAny(configs.get(DEFAULT), instanceProperties);
        }
        return instanceProperties;
    }

    /**
     * @return the configured retry backend properties
     */
    public Map<String, InstanceProperties> getInstances() {
        return instances;
    }

    /**
     * For backwards compatibility when setting backends in configuration properties.
     */
    public Map<String, InstanceProperties> getBackends() {
        return instances;
    }

    /**
     * @return common configuration for retry backend
     */
    public Map<String, InstanceProperties> getConfigs() {
        return configs;
    }

    /**
     * @param instanceProperties the retry backend spring properties
     * @return the retry configuration
     */
    public RetryConfig createRetryConfig(@Nullable InstanceProperties instanceProperties,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer, String instanceName) {
        RetryConfig baseConfig = null;
        if (instanceProperties != null && StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
            InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
            }
            ConfigUtils.mergePropertiesIfAny(baseProperties, instanceProperties);
            baseConfig = createRetryConfig(baseProperties, compositeRetryCustomizer, instanceProperties.getBaseConfig());
        } else if (!instanceName.equals(DEFAULT) && configs.get(DEFAULT) != null) {
            if (instanceProperties != null) {
                ConfigUtils.mergePropertiesIfAny(configs.get(DEFAULT), instanceProperties);
            }
            baseConfig = createRetryConfig(configs.get(DEFAULT), compositeRetryCustomizer, DEFAULT);
        }
        return buildConfig(baseConfig != null ? from(baseConfig) : custom(), instanceProperties, compositeRetryCustomizer, instanceName);
    }

    /**
     * @param properties the configured spring backend properties
     * @return retry config builder instance
     */
    @SuppressWarnings("unchecked")
    private RetryConfig buildConfig(RetryConfig.Builder builder,
                                    @Nullable InstanceProperties properties,
                                    CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer,
                                    String backend) {
        if (properties != null) {
            configureRetryIntervalFunction(properties, builder);

            if (properties.getMaxAttempts() != null && properties.getMaxAttempts() != 0) {
                builder.maxAttempts(properties.getMaxAttempts());
            }

            if (properties.getRetryExceptionPredicate() != null) {
                Predicate<Throwable> predicate = ClassUtils
                    .instantiatePredicateClass(properties.getRetryExceptionPredicate());
                builder.retryOnException(predicate);
            }

            if (properties.getIgnoreExceptions() != null) {
                builder.ignoreExceptions(properties.getIgnoreExceptions());
            }

            if (properties.getRetryExceptions() != null) {
                builder.retryExceptions(properties.getRetryExceptions());
            }

            if (properties.getResultPredicate() != null) {
                Predicate<Object> predicate = ClassUtils
                    .instantiatePredicateClass(properties.getResultPredicate());
                builder.retryOnResult(predicate);
            }

            if(properties.getConsumeResultBeforeRetryAttempt() != null){
                BiConsumer<Integer, Object> biConsumer = ClassUtils.instantiateBiConsumer(properties.getConsumeResultBeforeRetryAttempt());
                builder.consumeResultBeforeRetryAttempt(biConsumer);
            }

            if (properties.getIntervalBiFunction() != null) {
                IntervalBiFunction<Object> intervalBiFunction = ClassUtils
                    .instantiateIntervalBiFunctionClass(properties.getIntervalBiFunction());
                builder.intervalBiFunction(intervalBiFunction);
            }
            if(properties.getFailAfterMaxAttempts() != null) {
                builder.failAfterMaxAttempts(properties.getFailAfterMaxAttempts());
            }
        }

        compositeRetryCustomizer.getCustomizer(backend)
            .ifPresent(customizer -> customizer.customize(builder));

        return builder.build();
    }

    /**
     * decide which retry delay policy will be configured based into the configured properties
     *
     * @param properties the backend retry properties
     * @param builder    the retry config builder
     */
    private void configureRetryIntervalFunction(InstanceProperties properties, RetryConfig.Builder<Object> builder) {
        // these take precedence over deprecated properties. Setting one or the other will still work.
        Duration waitDuration = properties.getWaitDuration();
        if (waitDuration != null && waitDuration.toMillis() >= 0) {
            if (Boolean.TRUE.equals(properties.getEnableExponentialBackoff()) &&
                Boolean.TRUE.equals(properties.getEnableRandomizedWait())) {
                configureExponentialBackoffAndRandomizedWait(properties, builder);
            } else if (Boolean.TRUE.equals(properties.getEnableExponentialBackoff())) {
                configureExponentialBackoff(properties, builder);
            } else if (Boolean.TRUE.equals(properties.getEnableRandomizedWait())) {
                configureRandomizedWait(properties, builder);
            } else {
                builder.waitDuration(waitDuration);
            }
        }
    }

    private void configureExponentialBackoffAndRandomizedWait(InstanceProperties properties, RetryConfig.Builder<Object> builder) {
        Duration waitDuration = properties.getWaitDuration();
        Double backoffMultiplier = properties.getExponentialBackoffMultiplier();
        Double randomizedWaitFactor = properties.getRandomizedWaitFactor();
        Duration maxWaitDuration = properties.getExponentialMaxWaitDuration();
        if (maxWaitDuration != null &&
            randomizedWaitFactor != null &&
            backoffMultiplier != null) {
            withIntervalBiFunction(builder,
                    IntervalFunction.ofExponentialRandomBackoff(waitDuration, backoffMultiplier, randomizedWaitFactor, maxWaitDuration));
        } else if (randomizedWaitFactor != null &&
            backoffMultiplier != null) {
            withIntervalBiFunction(builder,
                    IntervalFunction.ofExponentialRandomBackoff(waitDuration, backoffMultiplier, randomizedWaitFactor));
        } else if (backoffMultiplier != null) {
            withIntervalBiFunction(builder, IntervalFunction.ofExponentialRandomBackoff(waitDuration, backoffMultiplier));
        } else {
            withIntervalBiFunction(builder, IntervalFunction.ofExponentialRandomBackoff(waitDuration));
        }
    }

    private void configureExponentialBackoff(InstanceProperties properties, RetryConfig.Builder<Object> builder) {
        Duration waitDuration = properties.getWaitDuration();
        Double backoffMultiplier = properties.getExponentialBackoffMultiplier();
        Duration maxWaitDuration = properties.getExponentialMaxWaitDuration();
        if (maxWaitDuration != null &&
            backoffMultiplier != null) {
            withIntervalBiFunction(builder, IntervalFunction.ofExponentialBackoff(waitDuration, backoffMultiplier, maxWaitDuration));
        } else if (backoffMultiplier != null) {
            withIntervalBiFunction(builder, IntervalFunction.ofExponentialBackoff(waitDuration, backoffMultiplier));
        } else {
            withIntervalBiFunction(builder, IntervalFunction.ofExponentialBackoff(waitDuration));
        }
    }

    private void configureRandomizedWait(InstanceProperties properties, RetryConfig.Builder<Object> builder) {
        Duration waitDuration = properties.getWaitDuration();
        Double randomizedWaitFactor = properties.getRandomizedWaitFactor();
        if (randomizedWaitFactor != null) {
            withIntervalBiFunction(builder, IntervalFunction.ofRandomized(waitDuration, randomizedWaitFactor));
        } else {
            withIntervalBiFunction(builder, IntervalFunction.ofRandomized(waitDuration));
        }
    }

    private void withIntervalBiFunction(RetryConfig.Builder<Object> builder, IntervalFunction intervalFunction) {
        builder.intervalBiFunction(IntervalBiFunction.ofIntervalFunction(intervalFunction));
    }

    /**
     * Class storing property values for configuring {@link io.github.resilience4j.retry.Retry}
     * instances.
     */
    public static class InstanceProperties {

        /*
         * wait long value for the next try
         */
        @Nullable
        private Duration waitDuration;

        /*
         * retry intervalBiFunction class to be used to calculate wait based on exception or result
         */
        @Nullable
        private Class<? extends IntervalBiFunction<Object>> intervalBiFunction;

        @Nullable
        private Integer maxAttempts;

        /**
         * retry exception predicate class to be used to evaluate the exception to retry or not
         */
        @Nullable
        private Class<? extends Predicate<Throwable>> retryExceptionPredicate;

        /**
         * retry setResultPredicate predicate class to be used to evaluate the result to retry or not
         */
        @Nullable
        private Class<? extends Predicate<Object>> resultPredicate;

        /**
         * class to be used to perform post actions on the object if it needs to be retried
         */
        @Nullable
        private Class<? extends BiConsumer<Integer, Object>> consumeResultBeforeRetryAttempt;

        /**
         * list of retry exception classes
         */
        @Nullable
        private Class<? extends Throwable>[] retryExceptions;

        /**
         * list of retry ignored exception classes
         */
        @Nullable
        private Class<? extends Throwable>[] ignoreExceptions;

        /**
         * event buffer size for generated retry events
         */
        @Nullable
        private Integer eventConsumerBufferSize;

        /**
         * flag to enable Exponential backoff policy or not for retry policy delay
         */
        @Nullable
        private Boolean enableExponentialBackoff;

        /**
         * exponential backoff multiplier value
         */
        private Double exponentialBackoffMultiplier;

        /**
         * exponential max interval value
         */
        private Duration exponentialMaxWaitDuration;

        /**
         * flag to enable randomized delay  policy or not for retry policy delay
         */
        @Nullable
        private Boolean enableRandomizedWait;

        /**
         * randomized delay factor value
         */
        private Double randomizedWaitFactor;

        /**
         * flag to enable explicit MaxRetriesExceededException to be thrown when max retries are exceeded
         */
        @Nullable
        private Boolean failAfterMaxAttempts;

        @Nullable
        private String baseConfig;

        @Nullable
        public Duration getWaitDuration() {
            return waitDuration;
        }

        public InstanceProperties setWaitDuration(Duration waitDuration) {
            Objects.requireNonNull(waitDuration);
            if (waitDuration.isNegative()) {
                throw new IllegalArgumentException(
                    "Illegal argument waitDuration: " + waitDuration + " is negative");
            }

            this.waitDuration = waitDuration;
            return this;
        }

        @Nullable
        public Class<? extends IntervalBiFunction<Object>> getIntervalBiFunction() {
            return intervalBiFunction;
        }

        public void setIntervalBiFunction(Class<? extends IntervalBiFunction<Object>> intervalBiFunction) {
            this.intervalBiFunction = intervalBiFunction;
        }

        @Nullable
        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public InstanceProperties setMaxAttempts(Integer maxAttempts) {
            Objects.requireNonNull(maxAttempts);
            if (maxAttempts < 1) {
                throw new IllegalArgumentException(
                    "maxAttempts must be greater than or equal to 1.");
            }

            this.maxAttempts = maxAttempts;
            return this;
        }

        @Nullable
        public Class<? extends Predicate<Throwable>> getRetryExceptionPredicate() {
            return retryExceptionPredicate;
        }

        public InstanceProperties setRetryExceptionPredicate(
            Class<? extends Predicate<Throwable>> retryExceptionPredicate) {
            this.retryExceptionPredicate = retryExceptionPredicate;
            return this;
        }

        @Nullable
        public Class<? extends Predicate<Object>> getResultPredicate() {
            return resultPredicate;
        }

        public InstanceProperties setResultPredicate(
            Class<? extends Predicate<Object>> resultPredicate) {
            this.resultPredicate = resultPredicate;
            return this;
        }

        @Nullable
        Class<? extends BiConsumer<Integer, Object>> getConsumeResultBeforeRetryAttempt(){
            return consumeResultBeforeRetryAttempt;
        }

        public InstanceProperties setConsumeResultBeforeRetryAttempt(Class<? extends BiConsumer<Integer, Object>> consumer){
            this.consumeResultBeforeRetryAttempt = consumer;
            return this;
        }

        @Nullable
        public Class<? extends Throwable>[] getRetryExceptions() {
            return retryExceptions;
        }

        public InstanceProperties setRetryExceptions(Class<? extends Throwable>[] retryExceptions) {
            this.retryExceptions = retryExceptions;
            return this;
        }

        @Nullable
        public Class<? extends Throwable>[] getIgnoreExceptions() {
            return ignoreExceptions;
        }

        public InstanceProperties setIgnoreExceptions(
            Class<? extends Throwable>[] ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
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

        public Boolean getEnableExponentialBackoff() {
            return enableExponentialBackoff;
        }

        public InstanceProperties setEnableExponentialBackoff(Boolean enableExponentialBackoff) {
            this.enableExponentialBackoff = enableExponentialBackoff;
            return this;
        }

        @Nullable
        public Double getExponentialBackoffMultiplier() {
            return exponentialBackoffMultiplier;
        }

        public InstanceProperties setExponentialBackoffMultiplier(Double exponentialBackoffMultiplier) {
            if (exponentialBackoffMultiplier <= 0) {
                throw new IllegalArgumentException(
                    "Illegal argument exponentialBackoffMultiplier: " + exponentialBackoffMultiplier + " is less or equal 0");
            }
            this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
            return this;
        }

        @Nullable
        public Duration getExponentialMaxWaitDuration() {
            return exponentialMaxWaitDuration;
        }

        public InstanceProperties setExponentialMaxWaitDuration(Duration exponentialMaxWaitDuration) {
            if (exponentialMaxWaitDuration.isNegative()) {
                throw new IllegalArgumentException(
                    "Illegal argument exponentialMaxWaitDuration: " + exponentialMaxWaitDuration + " is negative");
            }
            this.exponentialMaxWaitDuration = exponentialMaxWaitDuration;
            return this;
        }

        @Nullable
        public Boolean getEnableRandomizedWait() {
            return enableRandomizedWait;
        }

        public InstanceProperties setEnableRandomizedWait(Boolean enableRandomizedWait) {
            this.enableRandomizedWait = enableRandomizedWait;
            return this;
        }

        @Nullable
        public Double getRandomizedWaitFactor() {
            return randomizedWaitFactor;
        }

        public InstanceProperties setRandomizedWaitFactor(Double randomizedWaitFactor) {
            if (randomizedWaitFactor < 0 || randomizedWaitFactor >= 1) {
                throw new IllegalArgumentException(
                    "Illegal argument randomizedWaitFactor: " + randomizedWaitFactor + " is not in range [0..1)");
            }
            this.randomizedWaitFactor = randomizedWaitFactor;
            return this;
        }

        @Nullable
        public Boolean getFailAfterMaxAttempts() { return failAfterMaxAttempts; }

        public InstanceProperties setFailAfterMaxAttempts(Boolean failAfterMaxAttempts) {
            this.failAfterMaxAttempts = failAfterMaxAttempts;
            return this;
        }

        /**
         * Gets the shared configuration name. If this is set, the configuration builder will use
         * the shared configuration backend over this one.
         *
         * @return The shared configuration name.
         */
        @Nullable
        public String getBaseConfig() {
            return baseConfig;
        }

        /**
         * Sets the shared configuration name. If this is set, the configuration builder will use
         * the shared configuration backend over this one.
         *
         * @param baseConfig The shared configuration name.
         */
        public InstanceProperties setBaseConfig(String baseConfig) {
            this.baseConfig = baseConfig;
            return this;
        }
    }

}
