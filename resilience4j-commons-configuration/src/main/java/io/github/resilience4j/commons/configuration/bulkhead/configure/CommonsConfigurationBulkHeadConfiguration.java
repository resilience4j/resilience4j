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

package io.github.resilience4j.commons.configuration.bulkhead.configure;

import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
import io.github.resilience4j.commons.configuration.exception.ConfigParseException;
import io.github.resilience4j.commons.configuration.util.Constants;
import io.github.resilience4j.commons.configuration.util.StringParseUtil;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CommonsConfigurationBulkHeadConfiguration extends CommonBulkheadConfigurationProperties {
    private static final String BULK_HEAD_CONFIGS_PREFIX = "resilience4j.bulkhead.configs";
    private static final String BULK_HEAD_INSTANCES_PREFIX = "resilience4j.bulkhead.instances";
    private static final String MAX_CONCURRENT_CALLS =  "maxConcurrentCalls";
    private static final String MAX_WAIT_DURATION = "maxWaitDuration";
    private static final String WRITABLE_STACK_TRACE_ENABLED = "writableStackTraceEnabled";
    private static final String EVENT_CONSUMER_BUFFER_SIZE = "eventConsumerBufferSize";

    private CommonsConfigurationBulkHeadConfiguration(){
    }

    /**
     * Initializes a bulkhead configuration from a {@link Configuration} configuration
     * @param configuration the configuration to read from
     * @return  a {@link CommonsConfigurationBulkHeadConfiguration} object
     * @throws ConfigParseException if the configuration is invalid
     */
    public static CommonsConfigurationBulkHeadConfiguration of(final Configuration configuration) throws ConfigParseException {
        CommonsConfigurationBulkHeadConfiguration obj = new CommonsConfigurationBulkHeadConfiguration();
        try{
            obj.getConfigs().putAll(obj.getProperties(configuration.subset(BULK_HEAD_CONFIGS_PREFIX)));
            obj.getInstances().putAll(obj.getProperties(configuration.subset(BULK_HEAD_INSTANCES_PREFIX)));
            return obj;
        }catch (Exception ex){
            throw new ConfigParseException("Error creating bulkhead configuration", ex);
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
        if (configuration.containsKey(MAX_CONCURRENT_CALLS)) {
            instanceProperties.setMaxConcurrentCalls(configuration.getInt(MAX_CONCURRENT_CALLS));
        }
        if (configuration.containsKey(MAX_WAIT_DURATION)) {
            instanceProperties.setMaxWaitDuration(configuration.getDuration(MAX_WAIT_DURATION));
        }
        if (configuration.containsKey(WRITABLE_STACK_TRACE_ENABLED)) {
            instanceProperties.setWritableStackTraceEnabled(configuration.getBoolean(WRITABLE_STACK_TRACE_ENABLED));
        }
        if (configuration.containsKey(Constants.BASE_CONFIG)) {
            instanceProperties.setBaseConfig(configuration.getString(Constants.BASE_CONFIG));
        }
        if (configuration.containsKey(EVENT_CONSUMER_BUFFER_SIZE)) {
            instanceProperties.setEventConsumerBufferSize(configuration.getInt(EVENT_CONSUMER_BUFFER_SIZE));
        }
        return instanceProperties;
    };
}
