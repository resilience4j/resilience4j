package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.collection.HashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractRefreshScopedCircuitBreakerConfiguration {

    protected final CircuitBreakerConfiguration circuitBreakerConfiguration;
    protected final CircuitBreakerConfigurationProperties circuitBreakerProperties;

    protected AbstractRefreshScopedCircuitBreakerConfiguration(
        CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(
            circuitBreakerProperties);
    }

    /**
     * @param eventConsumerRegistry the circuit breaker event consumer registry
     * @return the RefreshScoped CircuitBreakerRegistry
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer) {
        CircuitBreakerRegistry circuitBreakerRegistry =
            circuitBreakerConfiguration.createCircuitBreakerRegistry(circuitBreakerProperties,
                circuitBreakerRegistryEventConsumer);

        // Register the event consumers
        circuitBreakerConfiguration.registerEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);
        HashMap<String, String> allTags = getAllTags();
        // Initialize backends that were initially configured.
        circuitBreakerConfiguration.initCircuitBreakerRegistry(circuitBreakerRegistry,allTags);

        return circuitBreakerRegistry;
    }

    public HashMap<String, String> getAllTags() {
        HashMap<String, String> allTags = HashMap.empty();
        circuitBreakerProperties.getInstances().values().forEach(instanceProperties -> {
            instanceProperties.getTags().forEach(allTags::put);
        });
        circuitBreakerProperties.getConfigs().values().forEach(defaultConfig -> {
            defaultConfig.getTags().forEach(allTags::put);
        });
        return allTags;
    }

}
