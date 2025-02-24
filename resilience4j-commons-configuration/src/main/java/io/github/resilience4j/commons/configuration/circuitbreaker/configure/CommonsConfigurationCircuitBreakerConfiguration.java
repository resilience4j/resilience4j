/*
 *   Copyright 2023: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.github.resilience4j.commons.configuration.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.commons.configuration.exception.ConfigParseException;
import io.github.resilience4j.commons.configuration.util.ClassParseUtil;
import io.github.resilience4j.commons.configuration.util.Constants;
import io.github.resilience4j.commons.configuration.util.StringParseUtil;
import org.apache.commons.configuration2.Configuration;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class CommonsConfigurationCircuitBreakerConfiguration extends CommonCircuitBreakerConfigurationProperties {
    private static final String CIRCUITBREAKER_CONFIGS_PREFIX = "resilience4j.circuitbreaker.configs";
    private static final String CIRCUITBREAKER_INSTANCES_PREFIX = "resilience4j.circuitbreaker.instances";
    protected static final String SLIDING_WINDOW_SIZE = "slidingWindowSize";
    protected static final String PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE = "permittedNumberOfCallsInHalfOpenState";
    protected static final String WAIT_DURATION_IN_OPEN_STATE = "waitDurationInOpenState";
    protected static final String FAILURE_RATE_THRESHOLD = "failureRateThreshold";
    protected static final String SLOW_CALL_RATE_THRESHOLD = "slowCallRateThreshold";
    protected static final String SLOW_CALL_DURATION_THRESHOLD = "slowCallDurationThreshold";
    protected static final String RECORD_EXCEPTIONS = "recordExceptions";
    protected static final String MAX_WAIT_DURATION_IN_HALF_OPEN_STATE = "maxWaitDurationInHalfOpenState";
    protected static final String TRANSITION_TO_STATE_AFTER_WAIT_DURATION = "transitionToStateAfterWaitDuration";
    protected static final String SLIDING_WINDOW_TYPE = "slidingWindowType";
    protected static final String MINIMUM_NUMBER_OF_CALLS = "minimumNumberOfCalls";
    protected static final String AUTOMATIC_TRANSITION_FROM_OPEN_TO_HALF_OPEN_ENABLED = "automaticTransitionFromOpenToHalfOpenEnabled";
    protected static final String INITIAL_STATE = "initialState";
    protected static final String WRITABLE_STACK_TRACE_ENABLED = "writableStackTraceEnabled";
    protected static final String ALLOW_HEALTH_INDICATOR_TO_FAIL = "allowHealthIndicatorToFail";
    protected static final String EVENT_CONSUMER_BUFFER_SIZE = "eventConsumerBufferSize";
    protected static final String REGISTER_HEALTH_INDICATOR = "registerHealthIndicator";
    protected static final String RECORD_FAILURE_PREDICATE = "recordFailurePredicate";
    protected static final String RECORD_RESULT_PREDICATE = "recordResultPredicate";
    protected static final String IGNORE_EXCEPTION_PREDICATE = "ignoreExceptionPredicate";
    protected static final String IGNORE_EXCEPTIONS = "ignoreExceptions";
    protected static final String ENABLE_EXPONENTIAL_BACKOFF = "enableExponentialBackoff";
    protected static final String EXPONENTIAL_BACKOFF_MULTIPLIER = "exponentialBackoffMultiplier";
    protected static final String EXPONENTIAL_MAX_WAIT_DURATION_IN_OPEN_STATE = "exponentialMaxWaitDurationInOpenState";
    protected static final String ENABLE_RANDOMIZED_WAIT = "enableRandomizedWait";
    protected static final String RANDOMIZED_WAIT_FACTOR = "randomizedWaitFactor";
    protected static final String IGNORE_CLASS_BINDING_EXCEPTIONS = "ignoreClassBindingExceptions";

    private CommonsConfigurationCircuitBreakerConfiguration() {
    }

    /**
     * Creates {@link CommonsConfigurationCircuitBreakerConfiguration} object from {@link Configuration} object.
     * @param configuration - configuration to read from
     * @return created {@link CommonsConfigurationCircuitBreakerConfiguration} object
     * @throws ConfigParseException if the configuration is invalid
     */
    public static CommonsConfigurationCircuitBreakerConfiguration of(final Configuration configuration) throws ConfigParseException{
        CommonsConfigurationCircuitBreakerConfiguration obj = new CommonsConfigurationCircuitBreakerConfiguration();
        try {
            obj.getConfigs().putAll(obj.getProperties(configuration.subset(CIRCUITBREAKER_CONFIGS_PREFIX)));
            obj.getInstances().putAll(obj.getProperties(configuration.subset(CIRCUITBREAKER_INSTANCES_PREFIX)));
            return obj;
        } catch (Exception ex) {
            throw new ConfigParseException("Error creating circuitbreaker configuration", ex);
        }
    }

    private Map<String, InstanceProperties> getProperties(final Configuration configuration) {
        Set<String> uniqueInstances = StringParseUtil.extractUniquePrefixes(configuration.getKeys(), Constants.PROPERTIES_KEY_DELIMITER);
        Map<String, InstanceProperties> instanceConfigsMap = new HashMap<>();
        uniqueInstances.forEach(instance -> {
            instanceConfigsMap.put(instance, mapConfigurationToInstanceProperties.apply(configuration.subset(instance)));
        });
        return instanceConfigsMap;
    }

    private final Function<Configuration, InstanceProperties> mapConfigurationToInstanceProperties = configuration -> {
        InstanceProperties instanceProperties = new InstanceProperties();
        if (configuration.containsKey(Constants.BASE_CONFIG))
            instanceProperties.setBaseConfig(configuration.getString(Constants.BASE_CONFIG));
        if (configuration.containsKey(WAIT_DURATION_IN_OPEN_STATE))
            instanceProperties.setWaitDurationInOpenState(configuration.getDuration(WAIT_DURATION_IN_OPEN_STATE));
        if (configuration.containsKey(SLOW_CALL_DURATION_THRESHOLD))
            instanceProperties.setSlowCallDurationThreshold(configuration.getDuration(SLOW_CALL_DURATION_THRESHOLD));
        if (configuration.containsKey(MAX_WAIT_DURATION_IN_HALF_OPEN_STATE))
            instanceProperties.setMaxWaitDurationInHalfOpenState(configuration.getDuration(MAX_WAIT_DURATION_IN_HALF_OPEN_STATE));
        if (configuration.containsKey(TRANSITION_TO_STATE_AFTER_WAIT_DURATION))
            instanceProperties.setTransitionToStateAfterWaitDuration(configuration.get(CircuitBreaker.State.class, TRANSITION_TO_STATE_AFTER_WAIT_DURATION));
        if (configuration.containsKey(FAILURE_RATE_THRESHOLD))
            instanceProperties.setFailureRateThreshold(configuration.getFloat(FAILURE_RATE_THRESHOLD));
        if (configuration.containsKey(SLOW_CALL_RATE_THRESHOLD))
            instanceProperties.setSlowCallRateThreshold(configuration.getFloat(SLOW_CALL_RATE_THRESHOLD));
        if (configuration.containsKey(SLIDING_WINDOW_TYPE))
            instanceProperties.setSlidingWindowType(configuration.getEnum(SLIDING_WINDOW_TYPE, CircuitBreakerConfig.SlidingWindowType.class));
        if (configuration.containsKey(SLIDING_WINDOW_SIZE))
            instanceProperties.setSlidingWindowSize(configuration.getInt(SLIDING_WINDOW_SIZE));
        if (configuration.containsKey(MINIMUM_NUMBER_OF_CALLS))
            instanceProperties.setMinimumNumberOfCalls(configuration.getInt(MINIMUM_NUMBER_OF_CALLS));
        if (configuration.containsKey(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE))
            instanceProperties.setPermittedNumberOfCallsInHalfOpenState(configuration.getInt(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE));
        if (configuration.containsKey(AUTOMATIC_TRANSITION_FROM_OPEN_TO_HALF_OPEN_ENABLED))
            instanceProperties.setAutomaticTransitionFromOpenToHalfOpenEnabled(configuration.getBoolean(AUTOMATIC_TRANSITION_FROM_OPEN_TO_HALF_OPEN_ENABLED));
        if (configuration.containsKey(WRITABLE_STACK_TRACE_ENABLED))
            instanceProperties.setWritableStackTraceEnabled(configuration.getBoolean(WRITABLE_STACK_TRACE_ENABLED));
        if (configuration.containsKey(ALLOW_HEALTH_INDICATOR_TO_FAIL))
            instanceProperties.setAllowHealthIndicatorToFail(configuration.getBoolean(ALLOW_HEALTH_INDICATOR_TO_FAIL));
        if (configuration.containsKey(EVENT_CONSUMER_BUFFER_SIZE))
            instanceProperties.setEventConsumerBufferSize(configuration.getInt(EVENT_CONSUMER_BUFFER_SIZE));
        if (configuration.containsKey(REGISTER_HEALTH_INDICATOR))
            instanceProperties.setRegisterHealthIndicator(configuration.getBoolean(REGISTER_HEALTH_INDICATOR));
        if (configuration.containsKey(RECORD_FAILURE_PREDICATE))
            instanceProperties.setRecordFailurePredicate((Class<Predicate<Throwable>>) ClassParseUtil.convertStringToClassType(
                    configuration.getString(RECORD_FAILURE_PREDICATE), Predicate.class));
        if (configuration.containsKey(RECORD_EXCEPTIONS))
            instanceProperties.setRecordExceptions(ClassParseUtil.convertStringListToClassTypeArray(configuration.getList(String.class,
                    RECORD_EXCEPTIONS), Throwable.class));
        if (configuration.containsKey(RECORD_RESULT_PREDICATE))
            instanceProperties.setRecordResultPredicate((Class<Predicate<Object>>) ClassParseUtil.convertStringToClassType(
                    configuration.getString(RECORD_RESULT_PREDICATE), Predicate.class));
        if (configuration.containsKey(IGNORE_EXCEPTION_PREDICATE))
            instanceProperties.setIgnoreExceptionPredicate((Class<Predicate<Throwable>>) ClassParseUtil.convertStringToClassType(
                    configuration.getString(IGNORE_EXCEPTION_PREDICATE), Predicate.class));
        if (configuration.containsKey(IGNORE_EXCEPTIONS))
            instanceProperties.setIgnoreExceptions(ClassParseUtil.convertStringListToClassTypeArray(configuration.getList(String.class,
                    IGNORE_EXCEPTIONS), Throwable.class));
        if (configuration.containsKey(ENABLE_EXPONENTIAL_BACKOFF))
            instanceProperties.setEnableExponentialBackoff(configuration.getBoolean(ENABLE_EXPONENTIAL_BACKOFF));
        if (configuration.containsKey(EXPONENTIAL_BACKOFF_MULTIPLIER))
            instanceProperties.setExponentialBackoffMultiplier(configuration.getDouble(EXPONENTIAL_BACKOFF_MULTIPLIER));
        if (configuration.containsKey(EXPONENTIAL_MAX_WAIT_DURATION_IN_OPEN_STATE))
            instanceProperties.setExponentialMaxWaitDurationInOpenState(configuration.getDuration(EXPONENTIAL_MAX_WAIT_DURATION_IN_OPEN_STATE));
        if (configuration.containsKey(ENABLE_RANDOMIZED_WAIT))
            instanceProperties.setEnableRandomizedWait(configuration.getBoolean(ENABLE_RANDOMIZED_WAIT));
        if (configuration.containsKey(RANDOMIZED_WAIT_FACTOR))
            instanceProperties.setRandomizedWaitFactor(configuration.getDouble(RANDOMIZED_WAIT_FACTOR));
        if (configuration.containsKey(IGNORE_CLASS_BINDING_EXCEPTIONS))
            instanceProperties.setIgnoreClassBindingExceptions(configuration.getBoolean(IGNORE_CLASS_BINDING_EXCEPTIONS));

        if(configuration.containsKey(INITIAL_STATE))
            instanceProperties.setInitialState(configuration.getEnum(INITIAL_STATE, CircuitBreaker.State.class));

        return instanceProperties;
    };
}
