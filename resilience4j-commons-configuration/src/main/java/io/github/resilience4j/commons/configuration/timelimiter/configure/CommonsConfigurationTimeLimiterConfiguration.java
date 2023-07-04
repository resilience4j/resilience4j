package io.github.resilience4j.commons.configuration.timelimiter.configure;

import io.github.resilience4j.common.timelimiter.configuration.CommonTimeLimiterConfigurationProperties;
import io.github.resilience4j.commons.configuration.exception.ConfigParseException;
import io.github.resilience4j.commons.configuration.util.Constants;
import io.github.resilience4j.commons.configuration.util.StringParseUtil;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CommonsConfigurationTimeLimiterConfiguration extends CommonTimeLimiterConfigurationProperties {
    private static final String TIME_LIMITER_CONFIGS_PREFIX = "resilience4j.timelimiter.configs";
    private static final String TIME_LIMITER_INSTANCES_PREFIX = "resilience4j.timelimiter.instances";
    private static final String TIMEOUT_DURATION = "timeoutDuration";
    private static final String CANCEL_RUNNING_FUTURE = "cancelRunningFuture";
    private static final String EVENT_CONSUMER_BUFFER_SIZE = "eventConsumerBufferSize";

    private CommonsConfigurationTimeLimiterConfiguration() {
    }

    /**
     * Creates a CommonsConfigurationTimeLimiterConfiguration from a Commons Configuration.
     * @param configuration a Commons Configuration
     * @return a CommonsConfigurationTimeLimiterConfiguration
     * @throws ConfigParseException if the configuration is invalid
     */
    public static CommonsConfigurationTimeLimiterConfiguration of(final Configuration configuration) throws ConfigParseException {
        CommonsConfigurationTimeLimiterConfiguration obj = new CommonsConfigurationTimeLimiterConfiguration();
        try {
            obj.getConfigs().putAll(obj.getProperties(configuration.subset(TIME_LIMITER_CONFIGS_PREFIX)));
            obj.getInstances().putAll(obj.getProperties(configuration.subset(TIME_LIMITER_INSTANCES_PREFIX)));
            return obj;
        } catch (Exception ex) {
            throw new ConfigParseException("Error creating timelimiter configuration", ex);
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
        if (configuration.containsKey(Constants.BASE_CONFIG)) {
            instanceProperties.setBaseConfig(configuration.getString(Constants.BASE_CONFIG));
        }
        if (configuration.containsKey(TIMEOUT_DURATION)) {
            instanceProperties.setTimeoutDuration(configuration.getDuration(TIMEOUT_DURATION));
        }
        if (configuration.containsKey(CANCEL_RUNNING_FUTURE)) {
            instanceProperties.setCancelRunningFuture(configuration.getBoolean(CANCEL_RUNNING_FUTURE));
        }
        if (configuration.containsKey(EVENT_CONSUMER_BUFFER_SIZE)) {
            instanceProperties.setEventConsumerBufferSize(configuration.getInt(EVENT_CONSUMER_BUFFER_SIZE));
        }
        return instanceProperties;
    };

}
