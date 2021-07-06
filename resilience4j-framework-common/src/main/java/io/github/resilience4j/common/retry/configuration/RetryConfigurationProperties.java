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
import io.github.resilience4j.core.ClassUtils;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Main spring properties for retry configuration
 */
public class RetryConfigurationProperties extends CommonProperties {

    private final Map<String, InstanceProperties> instances = new HashMap<>();
    private Map<String, InstanceProperties> configs = new HashMap<>();

    /**
     * @param backend backend name
     * @return the retry configuration
     */
    public RetryConfig createRetryConfig(String backend,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        return createRetryConfig(getBackendProperties(backend), compositeRetryCustomizer, backend);
    }

    /**
     * @param backend retry backend name
     * @return the configured spring backend properties
     */
    @Nullable
    public InstanceProperties getBackendProperties(String backend) {
        return instances.get(backend);
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
    public RetryConfig createRetryConfig(InstanceProperties instanceProperties,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer, String backend) {
        if (instanceProperties != null && StringUtils
            .isNotEmpty(instanceProperties.getBaseConfig())) {
            InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
            }
            return buildConfigFromBaseConfig(baseProperties, instanceProperties,
                compositeRetryCustomizer, backend);
        }
        return buildRetryConfig(RetryConfig.custom(), instanceProperties, compositeRetryCustomizer,
            backend);
    }

    private RetryConfig buildConfigFromBaseConfig(InstanceProperties baseProperties,
        InstanceProperties instanceProperties,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer,
        String backend) {
        RetryConfig baseConfig = createRetryConfig(baseProperties,
            compositeRetryCustomizer, backend);
        ConfigUtils.mergePropertiesIfAny(baseProperties, instanceProperties);
        return buildRetryConfig(RetryConfig.from(baseConfig), instanceProperties,
            compositeRetryCustomizer, backend);
    }

    /**
     * @param properties the configured spring backend properties
     * @return retry config builder instance
     */
    @SuppressWarnings("unchecked")
    private RetryConfig buildRetryConfig(RetryConfig.Builder builder,
        InstanceProperties properties,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer,
        String backend) {
        if (properties == null) {
            return builder.build();
        }

        configureRetryIntervalFunction(properties, builder);

        if (properties.getMaxRetryAttempts() != null && properties.getMaxRetryAttempts() != 0) {
            builder.maxAttempts(properties.getMaxRetryAttempts());
        }

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
        if (properties.getIntervalBiFunction() != null) {
            IntervalBiFunction<Object> intervalBiFunction = ClassUtils
                .instantiateIntervalBiFunctionClass(properties.getIntervalBiFunction());
            builder.intervalBiFunction(intervalBiFunction);
        }
        if(properties.getFailAfterMaxAttempts() != null) {
            builder.failAfterMaxAttempts(properties.getFailAfterMaxAttempts());
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
        if (properties.getWaitDuration() != null && properties.getWaitDuration().toMillis() > 0) {
            if (Boolean.TRUE.equals(properties.getEnableExponentialBackoff()) &&
                Boolean.TRUE.equals(properties.getEnableRandomizedWait())) {
                configureExponentialBackoffAndRandomizedWait(properties, builder);
            } else if (Boolean.TRUE.equals(properties.getEnableExponentialBackoff())) {
                configureExponentialBackoff(properties, builder);
            } else if (Boolean.TRUE.equals(properties.getEnableRandomizedWait())) {
                configureRandomizedWait(properties, builder);
            } else {
                builder.waitDuration(properties.getWaitDuration());
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
            builder.intervalFunction(
                IntervalFunction.ofExponentialRandomBackoff(waitDuration, backoffMultiplier, randomizedWaitFactor, maxWaitDuration));
        } else if (randomizedWaitFactor != null &&
            backoffMultiplier != null) {
            builder.intervalFunction(
                IntervalFunction.ofExponentialRandomBackoff(waitDuration, backoffMultiplier, randomizedWaitFactor));
        } else if (backoffMultiplier != null) {
            builder.intervalFunction(
                IntervalFunction.ofExponentialRandomBackoff(waitDuration, backoffMultiplier));
        } else {
            builder.intervalFunction(
                IntervalFunction.ofExponentialRandomBackoff(waitDuration));
        }
    }

    private void configureExponentialBackoff(InstanceProperties properties, RetryConfig.Builder<Object> builder) {
        Duration waitDuration = properties.getWaitDuration();
        Double backoffMultiplier = properties.getExponentialBackoffMultiplier();
        Duration maxWaitDuration = properties.getExponentialMaxWaitDuration();
        if (maxWaitDuration != null &&
            backoffMultiplier != null) {
            builder.intervalFunction(
                IntervalFunction.ofExponentialBackoff(waitDuration, backoffMultiplier, maxWaitDuration));
        } else if (backoffMultiplier != null) {
            builder.intervalFunction(
                IntervalFunction.ofExponentialBackoff(waitDuration, backoffMultiplier));
        } else {
            builder.intervalFunction(
                IntervalFunction.ofExponentialBackoff(waitDuration));
        }
    }

    private void configureRandomizedWait(InstanceProperties properties, RetryConfig.Builder<Object> builder) {
        Duration waitDuration = properties.getWaitDuration();
        Double randomizedWaitFactor = properties.getRandomizedWaitFactor();
        if (randomizedWaitFactor != null) {
            builder.intervalFunction(
                IntervalFunction.ofRandomized(waitDuration, randomizedWaitFactor));
        } else {
            builder.intervalFunction(
                IntervalFunction.ofRandomized(waitDuration));
        }
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

        /**
         * max retry attempts value
         *
         * @deprecated use maxAttempts
         */
        @Nullable
        @Deprecated
        private Integer maxRetryAttempts;

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
         * list of retry exception classes
         */
        @SuppressWarnings("unchecked")
        @Nullable
        private Class<? extends Throwable>[] retryExceptions;

        /**
         * list of retry ignored exception classes
         */
        @SuppressWarnings("unchecked")
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
            if (waitDuration.toMillis() < 0) {
                throw new IllegalArgumentException(
                    "waitDuration must be a positive value");
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

        /**
         *
         * @deprecated use getMaxAttempts()
         */
        @Nullable
        @Deprecated
        public Integer getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        @Nullable
        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        /**
         *
         * @deprecated use setMaxAttempts()
         */
        @Deprecated
        public InstanceProperties setMaxRetryAttempts(Integer maxRetryAttempts) {
            Objects.requireNonNull(maxRetryAttempts);
            if (maxRetryAttempts < 1) {
                throw new IllegalArgumentException(
                    "maxRetryAttempts must be greater than or equal to 1.");
            }

            this.maxRetryAttempts = maxRetryAttempts;
            return this;
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
            this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
            return this;
        }

        @Nullable
        public Duration getExponentialMaxWaitDuration() {
            return exponentialMaxWaitDuration;
        }

        public InstanceProperties setExponentialMaxWaitDuration(Duration exponentialMaxWaitDuration) {
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
