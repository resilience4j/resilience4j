package io.github.resilience4j.commons.configuration.retry.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

public class CommonsConfigurationRetryRegistry {
    private CommonsConfigurationRetryRegistry(){
    }

    /**
     * Create a RetryRegistry from apache commons configuration instance
     * @param configuration - apache commons configuration instance
     * @param customizer - customizer for retry configuration
     * @return a RetryRegistry with a Map of shared Retry configurations.
     */
    public static RetryRegistry of(Configuration configuration, CompositeCustomizer<RetryConfigCustomizer> customizer) {
        CommonRetryConfigurationProperties retryConfiguration = CommonsConfigurationRetryConfiguration.of(configuration);
        Map<String, RetryConfig> retryConfigMap = retryConfiguration.getInstances()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> retryConfiguration.createRetryConfig(entry.getValue(), customizer, entry.getKey())));
        return RetryRegistry.of(retryConfigMap);
    }
}
