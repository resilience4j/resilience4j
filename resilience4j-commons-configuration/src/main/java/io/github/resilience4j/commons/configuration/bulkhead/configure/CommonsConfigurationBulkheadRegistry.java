package io.github.resilience4j.commons.configuration.bulkhead.configure;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

public class CommonsConfigurationBulkheadRegistry {

    private CommonsConfigurationBulkheadRegistry() {
    }

    /**
     * Create a BulkheadRegistry from apache commons configuration instance
     * @param configuration - apache commons configuration instance
     * @param customizer - customizer for bulkhead configuration
     * @return a BulkheadRegistry with a Map of shared Bulkhead configurations.
     */
    public static BulkheadRegistry of(Configuration configuration, CompositeCustomizer<BulkheadConfigCustomizer> customizer){
        CommonBulkheadConfigurationProperties bulkheadProperties = CommonsConfigurationBulkHeadConfiguration.of(configuration);
        Map<String, BulkheadConfig> bulkheadConfigMap = bulkheadProperties.getInstances()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> bulkheadProperties.createBulkheadConfig(entry.getValue(), customizer, entry.getKey())));
        return BulkheadRegistry.of(bulkheadConfigMap);
    }
}
