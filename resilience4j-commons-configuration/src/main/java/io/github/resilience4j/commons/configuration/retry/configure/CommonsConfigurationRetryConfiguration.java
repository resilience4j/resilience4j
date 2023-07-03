package io.github.resilience4j.commons.configuration.retry.configure;

import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.commons.configuration.exception.ConfigParseException;
import io.github.resilience4j.commons.configuration.util.ClassParseUtil;
import io.github.resilience4j.commons.configuration.util.Constants;
import io.github.resilience4j.commons.configuration.util.StringParseUtil;
import io.github.resilience4j.core.IntervalBiFunction;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class CommonsConfigurationRetryConfiguration extends CommonRetryConfigurationProperties {
    private static final String RETRY_CONFIGS_PREFIX = "resilience4j.retry.configs";
    private static final String RETRY_INSTANCES_PREFIX = "resilience4j.retry.instances";
    private static final String WAIT_DURATION = "waitDuration";
    private static final String INTERVAL_BI_FUNCTION = "intervalBiFunction";
    private static final String MAX_ATTEMPTS = "maxAttempts";
    private static final String RETRY_EXCEPTION_PREDICATE = "retryExceptionPredicate";
    private static final String RESULT_PREDICATE = "resultPredicate";
    private static final String CONSUME_RESULT_BEFORE_RETRY_ATTEMPT = "consumeResultBeforeRetryAttempt";
    private static final String RETRY_EXCEPTIONS = "retryExceptions";
    private static final String IGNORE_EXCEPTIONS = "ignoreExceptions";
    private static final String EVENT_CONSUMER_BUFFER_SIZE = "eventConsumerBufferSize";
    private static final String ENABLE_EXPO_BACKOFF = "enableExponentialBackoff";
    private static final String EXPO_BACKOFF_MULTIPLIER = "exponentialBackoffMultiplier";
    private static final String EXPO_MAX_WAIT_DURATION = "exponentialMaxWaitDuration";
    private static final String ENABLE_RANDOMIZED_WAIT = "enableRandomizedWait";
    private static final String RANDOMIZED_WAIT_FACTOR = "randomizedWaitFactor";
    private static final String FAIL_AFTER_MAX_ATTEMPTS = "failAfterMaxAttempts";
    private CommonsConfigurationRetryConfiguration(){
    }

    /**
     * Initializes a retry configuration from a {@link Configuration} configuration
     * @param configuration the configuration to read from
     * @return a {@link CommonsConfigurationRetryConfiguration} object
     * @throws ConfigParseException if the configuration is invalid
     */
    public static CommonsConfigurationRetryConfiguration of(final Configuration configuration) throws ConfigParseException {
        CommonsConfigurationRetryConfiguration obj = new CommonsConfigurationRetryConfiguration();
        try{
            obj.getConfigs().putAll(obj.getProperties(configuration.subset(RETRY_CONFIGS_PREFIX)));
            obj.getInstances().putAll(obj.getProperties(configuration.subset(RETRY_INSTANCES_PREFIX)));
            return obj;
        }catch (Exception ex){
            throw new ConfigParseException("Error creating retry configuration", ex);
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
        if(configuration.containsKey(Constants.BASE_CONFIG)){
            instanceProperties.setBaseConfig(configuration.getString(Constants.BASE_CONFIG));
        }
        if(configuration.containsKey(WAIT_DURATION)){
            instanceProperties.setWaitDuration(configuration.getDuration(WAIT_DURATION));
        }
        if(configuration.containsKey(INTERVAL_BI_FUNCTION)){
            instanceProperties.setIntervalBiFunction((Class<? extends IntervalBiFunction<Object>>)
                    ClassParseUtil.convertStringToClassType(configuration.getString(INTERVAL_BI_FUNCTION), IntervalBiFunction.class));
        }
        if(configuration.containsKey(MAX_ATTEMPTS)){
            instanceProperties.setMaxAttempts(configuration.getInt(MAX_ATTEMPTS));
        }
        if(configuration.containsKey(RETRY_EXCEPTION_PREDICATE)){
            instanceProperties.setRetryExceptionPredicate((Class<? extends Predicate<Throwable>>)
                    ClassParseUtil.convertStringToClassType(configuration.getString(RETRY_EXCEPTION_PREDICATE), Predicate.class));
        }
        if(configuration.containsKey(RESULT_PREDICATE)){
            instanceProperties.setResultPredicate((Class<? extends Predicate<Object>>)
                    ClassParseUtil.convertStringToClassType(configuration.getString(RESULT_PREDICATE), Predicate.class));
        }
        if(configuration.containsKey(CONSUME_RESULT_BEFORE_RETRY_ATTEMPT)){
            instanceProperties.setConsumeResultBeforeRetryAttempt((Class<? extends BiConsumer<Integer, Object>>)
                    ClassParseUtil.convertStringToClassType(configuration.getString(CONSUME_RESULT_BEFORE_RETRY_ATTEMPT), BiConsumer.class));
        }
        if(configuration.containsKey(RETRY_EXCEPTIONS)){
            instanceProperties.setRetryExceptions(ClassParseUtil.convertStringListToClassTypeArray(
                    configuration.getList(String.class, RETRY_EXCEPTIONS), Throwable.class));
        }
        if(configuration.containsKey(IGNORE_EXCEPTIONS)){
            instanceProperties.setIgnoreExceptions(ClassParseUtil.convertStringListToClassTypeArray(
                    configuration.getList(String.class, IGNORE_EXCEPTIONS), Throwable.class));
        }
        if(configuration.containsKey(EVENT_CONSUMER_BUFFER_SIZE)){
            instanceProperties.setEventConsumerBufferSize(configuration.getInt(EVENT_CONSUMER_BUFFER_SIZE));
        }
        if(configuration.containsKey(ENABLE_EXPO_BACKOFF)){
            instanceProperties.setEnableExponentialBackoff(configuration.getBoolean(ENABLE_EXPO_BACKOFF));
        }
        if(configuration.containsKey(EXPO_BACKOFF_MULTIPLIER)){
            instanceProperties.setExponentialBackoffMultiplier(configuration.getDouble(EXPO_BACKOFF_MULTIPLIER));
        }
        if(configuration.containsKey(EXPO_MAX_WAIT_DURATION)){
            instanceProperties.setExponentialMaxWaitDuration(configuration.getDuration(EXPO_MAX_WAIT_DURATION));
        }
        if(configuration.containsKey(ENABLE_RANDOMIZED_WAIT)){
            instanceProperties.setEnableRandomizedWait(configuration.getBoolean(ENABLE_RANDOMIZED_WAIT));
        }
        if(configuration.containsKey(RANDOMIZED_WAIT_FACTOR)){
            instanceProperties.setRandomizedWaitFactor(configuration.getDouble(RANDOMIZED_WAIT_FACTOR));
        }
        if(configuration.containsKey(FAIL_AFTER_MAX_ATTEMPTS)){
            instanceProperties.setFailAfterMaxAttempts(configuration.getBoolean(FAIL_AFTER_MAX_ATTEMPTS));
        }
        return instanceProperties;
    };
}
