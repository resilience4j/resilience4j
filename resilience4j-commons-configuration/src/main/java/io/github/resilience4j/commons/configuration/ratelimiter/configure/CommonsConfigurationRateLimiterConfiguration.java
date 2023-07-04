package io.github.resilience4j.commons.configuration.ratelimiter.configure;

import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.github.resilience4j.commons.configuration.exception.ConfigParseException;
import io.github.resilience4j.commons.configuration.util.Constants;
import io.github.resilience4j.commons.configuration.util.StringParseUtil;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CommonsConfigurationRateLimiterConfiguration extends CommonRateLimiterConfigurationProperties {
    private static final String RATE_LIMITER_CONFIGS_PREFIX = "resilience4j.ratelimiter.configs";
    private static final String RATE_LIMITER_INSTANCES_PREFIX = "resilience4j.ratelimiter.instances";
    protected static final String BASE_CONFIG = "baseConfig";
    protected static final String LIMIT_FOR_PERIOD = "limitForPeriod";
    protected static final String LIMIT_REFRESH_PERIOD = "limitRefreshPeriod";
    protected static final String TIMEOUT_DURATION = "timeoutDuration";
    protected static final String SUBSCRIBE_FOR_EVENTS = "subscribeForEvents";
    protected static final String ALLOW_HEALTH_INDICATOR_TO_FAIL = "allowHealthIndicatorToFail";
    protected static final String REGISTER_HEALTH_INDICATOR = "registerHealthIndicator";
    protected static final String EVENT_CONSUMER_BUFFER_SIZE = "eventConsumerBufferSize";
    protected static final String WRITABLE_STACK_TRACE_ENABLED = "writableStackTraceEnabled";

    private CommonsConfigurationRateLimiterConfiguration() {
    }

    /**
     * Creates a CommonsConfigurationRateLimiterConfiguration from a Commons Configuration.
     * @param configuration a Commons Configuration
     * @return a CommonsConfigurationRateLimiterConfiguration
     * @throws ConfigParseException if the configuration is invalid
     */
    public static CommonsConfigurationRateLimiterConfiguration of(final Configuration configuration) throws ConfigParseException {
        CommonsConfigurationRateLimiterConfiguration obj = new CommonsConfigurationRateLimiterConfiguration();
        try {
            obj.getConfigs().putAll(obj.getProperties(configuration.subset(RATE_LIMITER_CONFIGS_PREFIX)));
            obj.getInstances().putAll(obj.getProperties(configuration.subset(RATE_LIMITER_INSTANCES_PREFIX)));
            return obj;
        } catch (Exception ex) {
            throw new ConfigParseException("Error creating ratelimiter configuration", ex);
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
        if (configuration.containsKey(BASE_CONFIG)) {
            instanceProperties.setBaseConfig(configuration.getString(BASE_CONFIG));
        }
        if (configuration.containsKey(LIMIT_FOR_PERIOD)) {
            instanceProperties.setLimitForPeriod(configuration.getInt(LIMIT_FOR_PERIOD));
        }
        if (configuration.containsKey(LIMIT_REFRESH_PERIOD)) {
            instanceProperties.setLimitRefreshPeriod(configuration.getDuration(LIMIT_REFRESH_PERIOD));
        }
        if (configuration.containsKey(TIMEOUT_DURATION)) {
            instanceProperties.setTimeoutDuration(configuration.getDuration(TIMEOUT_DURATION));
        }
        if (configuration.containsKey(SUBSCRIBE_FOR_EVENTS)) {
            instanceProperties.setSubscribeForEvents(configuration.getBoolean(SUBSCRIBE_FOR_EVENTS));
        }
        if (configuration.containsKey(ALLOW_HEALTH_INDICATOR_TO_FAIL)) {
            instanceProperties.setAllowHealthIndicatorToFail(configuration.getBoolean(ALLOW_HEALTH_INDICATOR_TO_FAIL));
        }
        if (configuration.containsKey(REGISTER_HEALTH_INDICATOR)) {
            instanceProperties.setRegisterHealthIndicator(configuration.getBoolean(REGISTER_HEALTH_INDICATOR));
        }
        if (configuration.containsKey(EVENT_CONSUMER_BUFFER_SIZE)) {
            instanceProperties.setEventConsumerBufferSize(configuration.getInt(EVENT_CONSUMER_BUFFER_SIZE));
        }
        if (configuration.containsKey(WRITABLE_STACK_TRACE_ENABLED)) {
            instanceProperties.setWritableStackTraceEnabled(configuration.getBoolean(WRITABLE_STACK_TRACE_ENABLED));
        }
        return instanceProperties;
    };
}
