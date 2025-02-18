/*
 * Copyright 2019 Dan Maas,Mahmoud Romeh
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
package io.github.resilience4j.common.circuitbreaker.configuration;


import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.Builder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.common.CommonProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.utils.ConfigUtils;
import io.github.resilience4j.core.ClassUtils;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.from;

public class CommonCircuitBreakerConfigurationProperties extends CommonProperties {

    private static final String DEFAULT = "default";
    private Map<String, InstanceProperties> instances = new HashMap<>();
    private Map<String, InstanceProperties> configs = new HashMap<>();

    public Optional<InstanceProperties> findCircuitBreakerProperties(String name) {
        InstanceProperties instanceProperties = instances.get(name);
        if (instanceProperties == null) {
            instanceProperties = configs.get(DEFAULT);
        } else if (configs.get(DEFAULT) != null) {
            ConfigUtils.mergePropertiesIfAny(instanceProperties, configs.get(DEFAULT));
        }
        return Optional.ofNullable(instanceProperties);
    }

    public CircuitBreakerConfig createCircuitBreakerConfig(String instanceName,
             @Nullable InstanceProperties instanceProperties,
             CompositeCustomizer<CircuitBreakerConfigCustomizer> customizer) {

        CircuitBreakerConfig baseConfig = null;
        if (instanceProperties != null && StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
            baseConfig = createBaseConfig(instanceName, instanceProperties, customizer);
        } else if (configs.get(instanceName) != null) {
            baseConfig = createDirectConfig(instanceName, instanceProperties, customizer);
        } else if (configs.get(DEFAULT) != null) {
            baseConfig = createDefaultConfig(instanceProperties, customizer);
        }

        return buildConfig(baseConfig != null ? from(baseConfig) : custom(), instanceProperties, customizer, instanceName);
    }

    private CircuitBreakerConfig createBaseConfig(String instanceName,
            InstanceProperties instanceProperties,
            CompositeCustomizer<CircuitBreakerConfigCustomizer> customizer) {

        String baseConfigName = instanceProperties.getBaseConfig();
        if (instanceName.equals(baseConfigName)) {
            throw new IllegalStateException("Circular reference detected in instance config: " + instanceName);
        }

        InstanceProperties baseProperties = configs.get(baseConfigName);
        if (baseProperties == null) {
            throw new ConfigurationNotFoundException(baseConfigName);
        }

        ConfigUtils.mergePropertiesIfAny(instanceProperties, baseProperties);
        return createCircuitBreakerConfig(baseConfigName, baseProperties, customizer);
    }

    private CircuitBreakerConfig createDirectConfig(String instanceName,
            @Nullable InstanceProperties instanceProperties,
            CompositeCustomizer<CircuitBreakerConfigCustomizer> customizer) {

        if (instanceProperties != null) {
            ConfigUtils.mergePropertiesIfAny(instanceProperties, configs.get(instanceName));
        }
        return buildConfig(custom(), configs.get(instanceName), customizer, instanceName);
    }

    private CircuitBreakerConfig createDefaultConfig(
            @Nullable InstanceProperties instanceProperties,
            CompositeCustomizer<CircuitBreakerConfigCustomizer> customizer) {

        if (instanceProperties != null) {
            ConfigUtils.mergePropertiesIfAny(instanceProperties, configs.get(DEFAULT));
        }
        return createCircuitBreakerConfig(DEFAULT, configs.get(DEFAULT), customizer);
    }

    private CircuitBreakerConfig buildConfig(Builder builder, @Nullable InstanceProperties properties,
        CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer,
        String instanceName) {
        if (properties != null) {
            if (properties.enableExponentialBackoff != null && properties.enableExponentialBackoff
                && properties.enableRandomizedWait != null && properties.enableRandomizedWait) {
                throw new IllegalStateException(
                    "you can not enable Exponential backoff policy and randomized delay at the same time , please enable only one of them");
            }

            configureCircuitBreakerOpenStateIntervalFunction(properties, builder);

            if (properties.getFailureRateThreshold() != null) {
                builder.failureRateThreshold(properties.getFailureRateThreshold());
            }

            if (properties.getWritableStackTraceEnabled() != null) {
                builder.writableStackTraceEnabled(properties.getWritableStackTraceEnabled());
            }

            if (properties.getSlowCallRateThreshold() != null) {
                builder.slowCallRateThreshold(properties.getSlowCallRateThreshold());
            }

            if (properties.getSlowCallDurationThreshold() != null) {
                builder.slowCallDurationThreshold(properties.getSlowCallDurationThreshold());
            }

            if (properties.getMaxWaitDurationInHalfOpenState() != null) {
                builder.maxWaitDurationInHalfOpenState(properties.getMaxWaitDurationInHalfOpenState());
            }

            if (properties.getTransitionToStateAfterWaitDuration() != null) {
                builder.transitionToStateAfterWaitDuration(properties.getTransitionToStateAfterWaitDuration());
            }

            if (properties.getSlidingWindowSize() != null) {
                builder.slidingWindowSize(properties.getSlidingWindowSize());
            }

            if (properties.getMinimumNumberOfCalls() != null) {
                builder.minimumNumberOfCalls(properties.getMinimumNumberOfCalls());
            }

            if (properties.getSlidingWindowType() != null) {
                builder.slidingWindowType(properties.getSlidingWindowType());
            }

            if (properties.getPermittedNumberOfCallsInHalfOpenState() != null) {
                builder.permittedNumberOfCallsInHalfOpenState(
                    properties.getPermittedNumberOfCallsInHalfOpenState());
            }

            if (properties.recordExceptions != null) {
                builder.recordExceptions(properties.recordExceptions);
                // if instance config has set recordExceptions, then base config's recordExceptionPredicate is useless.
                builder.recordException(null);
            }

            if (properties.recordFailurePredicate != null) {
                buildRecordFailurePredicate(properties, builder);
            }

            if (properties.recordResultPredicate != null) {
                buildRecordResultPredicate(properties, builder);
            }

            if (properties.ignoreExceptions != null) {
                builder.ignoreExceptions(properties.ignoreExceptions);
                builder.ignoreException(null);
            }

            if (properties.ignoreExceptionPredicate != null) {
                buildIgnoreExceptionPredicate(properties, builder);
            }

            if (properties.automaticTransitionFromOpenToHalfOpenEnabled != null) {
                builder.automaticTransitionFromOpenToHalfOpenEnabled(
                    properties.automaticTransitionFromOpenToHalfOpenEnabled);
            }

            if(properties.getInitialState() != null){
                builder.initialState(properties.getInitialState());
            }

        }
        compositeCircuitBreakerCustomizer.getCustomizer(instanceName).ifPresent(
            circuitBreakerConfigCustomizer -> circuitBreakerConfigCustomizer.customize(builder));
        return builder.build();
    }


    /**
     * decide which circuit breaker delay policy for open state will be configured based into the
     * configured properties
     *
     * @param properties the backend circuit breaker properties
     * @param builder    the circuit breaker config builder
     */
    private void configureCircuitBreakerOpenStateIntervalFunction(InstanceProperties properties,
        CircuitBreakerConfig.Builder builder) {
        Duration waitDurationInOpenState = properties.getWaitDurationInOpenState();
        if (waitDurationInOpenState != null
            && waitDurationInOpenState.toMillis() > 0) {
            if (properties.getEnableExponentialBackoff() != null
                && properties.getEnableExponentialBackoff()) {
                configureEnableExponentialBackoff(properties, builder);
            } else if (properties.getEnableRandomizedWait() != null
                && properties.getEnableRandomizedWait()) {
                configureEnableRandomizedWait(properties, builder);
            } else {
                builder.waitDurationInOpenState(waitDurationInOpenState);
            }
        }
    }

    private void configureEnableExponentialBackoff(InstanceProperties properties, Builder builder) {
        Duration maxWaitDuration = properties.getExponentialMaxWaitDurationInOpenState();
        Double backoffMultiplier = properties.getExponentialBackoffMultiplier();
        Duration waitDuration = properties.getWaitDurationInOpenState();
        if (maxWaitDuration != null
            && backoffMultiplier != null) {
            builder.waitIntervalFunctionInOpenState(
                IntervalFunction.ofExponentialBackoff(waitDuration, backoffMultiplier, maxWaitDuration));
        } else if (backoffMultiplier != null) {
            builder.waitIntervalFunctionInOpenState(
                IntervalFunction.ofExponentialBackoff(waitDuration, backoffMultiplier));
        } else {
            builder.waitIntervalFunctionInOpenState(
                IntervalFunction.ofExponentialBackoff(waitDuration));
        }
    }

    private void configureEnableRandomizedWait(InstanceProperties properties, Builder builder) {
        Duration waitDuration = properties.getWaitDurationInOpenState();
        if (properties.getRandomizedWaitFactor() != null) {
            builder.waitIntervalFunctionInOpenState(
                IntervalFunction.ofRandomized(waitDuration, properties.getRandomizedWaitFactor()));
        } else {
            builder.waitIntervalFunctionInOpenState(
                IntervalFunction.ofRandomized(waitDuration));
        }
    }

    private void buildRecordFailurePredicate(InstanceProperties properties, Builder builder) {
        if (properties.getRecordFailurePredicate() != null) {
            Predicate<Throwable> predicate = ClassUtils.instantiatePredicateClass(properties.getRecordFailurePredicate());
            if (predicate != null) {
                builder.recordException(predicate);
            }
        }
    }

    private void buildRecordResultPredicate(InstanceProperties properties, Builder builder) {
        if (properties.getRecordResultPredicate() != null) {
            Predicate<Object> predicate = ClassUtils.instantiatePredicateClass(properties.getRecordResultPredicate());
            if (predicate != null) {
                builder.recordResult(predicate);
            }
        }
    }

    private void buildIgnoreExceptionPredicate(InstanceProperties properties, Builder builder) {
        if (properties.getIgnoreExceptionPredicate() != null) {
            Predicate<Throwable> predicate = ClassUtils.instantiatePredicateClass(properties.getIgnoreExceptionPredicate());
            if (predicate != null) {
                builder.ignoreException(predicate);
            }
        }
    }

    @Nullable
    public InstanceProperties getBackendProperties(String backend) {
        return instances.get(backend);
    }

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

    /**
     * Class storing property values for configuring {@link io.github.resilience4j.circuitbreaker.CircuitBreaker}
     * instances.
     */
    public static class InstanceProperties {

        @Nullable
        private Duration waitDurationInOpenState;

        @Nullable
        private Duration slowCallDurationThreshold;

        @Nullable
        private Duration maxWaitDurationInHalfOpenState;

        @Nullable
        private State transitionToStateAfterWaitDuration;

        @Nullable
        private Float failureRateThreshold;

        @Nullable
        private Float slowCallRateThreshold;

        @Nullable
        private SlidingWindowType slidingWindowType;

        @Nullable
        private Integer slidingWindowSize;

        @Nullable
        private Integer minimumNumberOfCalls;

        @Nullable
        private Integer permittedNumberOfCallsInHalfOpenState;

        @Nullable
        private Boolean automaticTransitionFromOpenToHalfOpenEnabled;

        @Nullable
        private State initialState;

        @Nullable
        private Boolean writableStackTraceEnabled;

        @Nullable
        private Boolean allowHealthIndicatorToFail;

        @Nullable
        private Integer eventConsumerBufferSize;

        @Nullable
        private Boolean registerHealthIndicator;

        @Nullable
        private Class<Predicate<Throwable>> recordFailurePredicate;

        @Nullable
        private Class<? extends Throwable>[] recordExceptions;

        @Nullable
        private Class<Predicate<Object>> recordResultPredicate;

        @Nullable
        private Class<Predicate<Throwable>> ignoreExceptionPredicate;

        @Nullable
        private Class<? extends Throwable>[] ignoreExceptions;

        @Nullable
        private String baseConfig;

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
        private Duration exponentialMaxWaitDurationInOpenState;

        /**
         * flag to enable randomized delay  policy or not for retry policy delay
         */
        @Nullable
        private Boolean enableRandomizedWait;

        /**
         * randomized delay factor value
         */
        private Double randomizedWaitFactor;

        @Nullable
        private Boolean ignoreClassBindingExceptions;

        /**
         * Returns the failure rate threshold for the circuit breaker as percentage.
         *
         * @return the failure rate threshold
         */
        @Nullable
        public Float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        /**
         * Sets the failure rate threshold for the circuit breaker as percentage.
         *
         * @param failureRateThreshold the failure rate threshold
         */
        public InstanceProperties setFailureRateThreshold(Float failureRateThreshold) {
            Objects.requireNonNull(failureRateThreshold);
            if (failureRateThreshold < 1 || failureRateThreshold > 100) {
                throw new IllegalArgumentException(
                    "failureRateThreshold must be between 1 and 100.");
            }

            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        /**
         * Returns the wait duration the CircuitBreaker will stay open, before it switches to half
         * closed.
         *
         * @return the wait duration
         */
        @Nullable
        public Duration getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        /**
         * Sets the wait duration the CircuitBreaker should stay open, before it switches to half
         * closed.
         *
         * @param waitDurationInOpenStateMillis the wait duration
         */
        public InstanceProperties setWaitDurationInOpenState(
            Duration waitDurationInOpenStateMillis) {
            Objects.requireNonNull(waitDurationInOpenStateMillis);
            if (waitDurationInOpenStateMillis.toMillis() < 1) {
                throw new IllegalArgumentException(
                    "waitDurationInOpenStateMillis must be greater than or equal to 1 millis.");
            }

            this.waitDurationInOpenState = waitDurationInOpenStateMillis;
            return this;
        }

        /**
         * Returns if we should automatically transition to half open after the timer has run out.
         *
         * @return setAutomaticTransitionFromOpenToHalfOpenEnabled if we should automatically go to
         * half open or not
         */
        public Boolean getAutomaticTransitionFromOpenToHalfOpenEnabled() {
            return this.automaticTransitionFromOpenToHalfOpenEnabled;
        }

        /**
         * Sets if we should automatically transition to half open after the timer has run out.
         *
         * @param automaticTransitionFromOpenToHalfOpenEnabled The flag for automatic transition to
         *                                                     half open after the timer has run
         *                                                     out.
         */
        public InstanceProperties setAutomaticTransitionFromOpenToHalfOpenEnabled(
            Boolean automaticTransitionFromOpenToHalfOpenEnabled) {
            this.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled;
            return this;
        }

        /**
         * Returns state by which Circuit breaker was initialized
         *
         * @return initialState
         */
        @Nullable
        public State getInitialState(){
            return this.initialState;
        }


        /**
         * Sets initial state of Circuit Breaker
         *
         * @param state inital state of Circuit breaker, Will set initializion using this state
         */
        public InstanceProperties setInitialState(State state){
            this.initialState = state;
            return this;
        }

        /**
         * Returns if we should enable writable stack traces or not.
         *
         * @return writableStackTraceEnabled if we should enable writable stack traces or not.
         */
        @Nullable
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

        @Nullable
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
         * default, health indicators for circuit breakers will never go into an unhealthy state.
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

        @Nullable
        public Boolean getRegisterHealthIndicator() {
            return registerHealthIndicator;
        }

        public InstanceProperties setRegisterHealthIndicator(Boolean registerHealthIndicator) {
            this.registerHealthIndicator = registerHealthIndicator;
            return this;
        }

        @Nullable
        public Class<Predicate<Throwable>> getRecordFailurePredicate() {
            return recordFailurePredicate;
        }

        public InstanceProperties setRecordFailurePredicate(
            Class<Predicate<Throwable>> recordFailurePredicate) {
            this.recordFailurePredicate = recordFailurePredicate;
            return this;
        }

        @Nullable
        public Class<Predicate<Object>> getRecordResultPredicate() {
            return recordResultPredicate;
        }

        public InstanceProperties setRecordResultPredicate(
            Class<Predicate<Object>> recordResultPredicate) {
            this.recordResultPredicate = recordResultPredicate;
            return this;
        }

        @Nullable
        public Class<? extends Throwable>[] getRecordExceptions() {
            return recordExceptions;
        }

        public InstanceProperties setRecordExceptions(
            Class<? extends Throwable>[] recordExceptions) {
            this.recordExceptions = recordExceptions;
            return this;
        }

        @Nullable
        public Class<Predicate<Throwable>> getIgnoreExceptionPredicate() {
            return ignoreExceptionPredicate;
        }

        public InstanceProperties setIgnoreExceptionPredicate(
            Class<Predicate<Throwable>> ignoreExceptionPredicate) {
            this.ignoreExceptionPredicate = ignoreExceptionPredicate;
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

        @Nullable
        public Integer getPermittedNumberOfCallsInHalfOpenState() {
            return permittedNumberOfCallsInHalfOpenState;
        }

        public InstanceProperties setPermittedNumberOfCallsInHalfOpenState(
            Integer permittedNumberOfCallsInHalfOpenState) {
            Objects.requireNonNull(permittedNumberOfCallsInHalfOpenState);
            if (permittedNumberOfCallsInHalfOpenState < 1) {
                throw new IllegalArgumentException(
                    "permittedNumberOfCallsInHalfOpenState must be greater than or equal to 1.");
            }

            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
            return this;
        }

        @Nullable
        public Integer getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public InstanceProperties setMinimumNumberOfCalls(Integer minimumNumberOfCalls) {
            Objects.requireNonNull(minimumNumberOfCalls);
            if (minimumNumberOfCalls < 1) {
                throw new IllegalArgumentException(
                    "minimumNumberOfCalls must be greater than or equal to 1.");
            }

            this.minimumNumberOfCalls = minimumNumberOfCalls;
            return this;
        }

        @Nullable
        public Integer getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public InstanceProperties setSlidingWindowSize(Integer slidingWindowSize) {
            Objects.requireNonNull(slidingWindowSize);
            if (slidingWindowSize < 1) {
                throw new IllegalArgumentException(
                    "slidingWindowSize must be greater than or equal to 1.");
            }

            this.slidingWindowSize = slidingWindowSize;
            return this;
        }

        @Nullable
        public Float getSlowCallRateThreshold() {
            return slowCallRateThreshold;
        }

        public InstanceProperties setSlowCallRateThreshold(Float slowCallRateThreshold) {
            Objects.requireNonNull(slowCallRateThreshold);
            if (slowCallRateThreshold < 1 || slowCallRateThreshold > 100) {
                throw new IllegalArgumentException(
                    "slowCallRateThreshold must be between 1 and 100.");
            }

            this.slowCallRateThreshold = slowCallRateThreshold;
            return this;
        }

        @Nullable
        public Duration getSlowCallDurationThreshold() {
            return slowCallDurationThreshold;
        }

        @Nullable
        public Duration getMaxWaitDurationInHalfOpenState() {
            return maxWaitDurationInHalfOpenState;
        }

        @Nullable
        public State getTransitionToStateAfterWaitDuration() {
            return transitionToStateAfterWaitDuration;
        }

        public InstanceProperties setSlowCallDurationThreshold(Duration slowCallDurationThreshold) {
            Objects.requireNonNull(slowCallDurationThreshold);
            if (slowCallDurationThreshold.toNanos() < 1) {
                throw new IllegalArgumentException(
                    "slowCallDurationThreshold must be greater than or equal to 1 nanos.");
            }

            this.slowCallDurationThreshold = slowCallDurationThreshold;
            return this;
        }

        public InstanceProperties setMaxWaitDurationInHalfOpenState(Duration maxWaitDurationInHalfOpenState) {
            Objects.requireNonNull(maxWaitDurationInHalfOpenState);
            if (maxWaitDurationInHalfOpenState.toMillis() < 0) {
                throw new IllegalArgumentException(
                    "maxWaitDurationInHalfOpenState must be greater than or equal to 0 ms.");
            }

            this.maxWaitDurationInHalfOpenState = maxWaitDurationInHalfOpenState;
            return this;
        }

        public void setTransitionToStateAfterWaitDuration(State transitionToStateAfterWaitDuration) {
            Objects.requireNonNull(transitionToStateAfterWaitDuration);
            if (transitionToStateAfterWaitDuration != State.OPEN && transitionToStateAfterWaitDuration != State.CLOSED) {
                throw new IllegalArgumentException("transitionToStateAfterWaitDuration must be either OPEN or CLOSED");
            }
            this.transitionToStateAfterWaitDuration = transitionToStateAfterWaitDuration;
        }

        @Nullable
        public SlidingWindowType getSlidingWindowType() {
            return slidingWindowType;
        }

        public InstanceProperties setSlidingWindowType(SlidingWindowType slidingWindowType) {
            this.slidingWindowType = slidingWindowType;
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

        public InstanceProperties setExponentialBackoffMultiplier(
            Double exponentialBackoffMultiplier) {
            if (exponentialBackoffMultiplier <= 0) {
                throw new IllegalArgumentException(
                    "Illegal argument exponentialBackoffMultiplier: " + exponentialBackoffMultiplier + " is less or equal 0");
            }
            this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
            return this;
        }

        @Nullable
        public Duration getExponentialMaxWaitDurationInOpenState() {
            return exponentialMaxWaitDurationInOpenState;
        }

        public InstanceProperties setExponentialMaxWaitDurationInOpenState(
            Duration exponentialMaxWaitDurationInOpenState) {
            if (exponentialMaxWaitDurationInOpenState.toMillis() < 1) {
                throw new IllegalArgumentException(
                    "Illegal argument interval: " + exponentialMaxWaitDurationInOpenState + " is less than 1 millisecond");
            }
            this.exponentialMaxWaitDurationInOpenState = exponentialMaxWaitDurationInOpenState;
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
        public Boolean getIgnoreClassBindingExceptions() {
            return ignoreClassBindingExceptions;
        }

        public InstanceProperties setIgnoreClassBindingExceptions(Boolean ignoreClassBindingExceptions) {
            this.ignoreClassBindingExceptions = ignoreClassBindingExceptions;
            return this;
        }
    }

}
