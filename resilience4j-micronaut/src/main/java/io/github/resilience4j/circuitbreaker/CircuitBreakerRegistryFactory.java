package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Factory
@Requires(property = "resilience4j.circuitbreaker.enabled")
public class CircuitBreakerRegistryFactory {
    @Bean
    @Named("compositeRateLimiterCustomizer")
    public CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer(
        @Nullable List<CircuitBreakerConfigCustomizer> configCustomizer ) {
        return new CompositeCustomizer<>(configCustomizer);
    }

    @Singleton
    @Requires(beans = CircuitBreakerProperties.class)
    public CircuitBreakerRegistry circuitBreakerRegistry(
        CircuitBreakerProperties timeLimiterProperties,
        EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry,
        RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
        @Named("compositeTimeLimiterCustomizer") CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        return null;
    }
}
