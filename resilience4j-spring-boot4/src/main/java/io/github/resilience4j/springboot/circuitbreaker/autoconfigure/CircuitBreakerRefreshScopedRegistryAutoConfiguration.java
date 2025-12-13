package io.github.resilience4j.springboot.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = CircuitBreakerAutoConfiguration.class, after = RefreshAutoConfiguration.class)
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@ConditionalOnClass({CircuitBreaker.class, RefreshScope.class})
@ConditionalOnBean(org.springframework.cloud.context.scope.refresh.RefreshScope.class)
public class CircuitBreakerRefreshScopedRegistryAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    private final CircuitBreakerConfiguration circuitBreakerConfiguration;

    public CircuitBreakerRefreshScopedRegistryAutoConfiguration(
        CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(
            circuitBreakerProperties);
    }

    /**
     * Overriding {@link CircuitBreakerAutoConfiguration#circuitBreakerRegistry} to be refreshable.
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer,
        @Qualifier("compositeCircuitBreakerCustomizer") CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer) {
        return circuitBreakerConfiguration
            .circuitBreakerRegistry(eventConsumerRegistry, circuitBreakerRegistryEventConsumer,
                compositeCircuitBreakerCustomizer);
    }

}
