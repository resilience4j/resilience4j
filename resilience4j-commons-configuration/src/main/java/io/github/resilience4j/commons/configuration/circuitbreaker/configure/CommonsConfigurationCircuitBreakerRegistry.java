package io.github.resilience4j.commons.configuration.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

public class CommonsConfigurationCircuitBreakerRegistry {
    private CommonsConfigurationCircuitBreakerRegistry() {
    }

    /***
     * Create a CircuitBreakerRegistry from apache commons configuration instance
     * @param configuration - apache commons configuration instance
     * @param customizer - customizer for circuit breaker configuration
     * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
     */
    public static CircuitBreakerRegistry of(Configuration configuration, CompositeCustomizer<CircuitBreakerConfigCustomizer> customizer){
        CommonCircuitBreakerConfigurationProperties circuitBreakerProperties = CommonsConfigurationCircuitBreakerConfiguration.of(configuration);
        Map<String, CircuitBreakerConfig> circuitBreakerConfigMap = circuitBreakerProperties.getInstances()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> circuitBreakerProperties.createCircuitBreakerConfig(entry.getKey(), entry.getValue(), customizer)));
        return CircuitBreakerRegistry.of(circuitBreakerConfigMap);
    }
}
