package io.github.resilience4j.circuitbreaker;

import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;

@Factory
public class CircuitBreakerRegistryFactory {
    @Singleton
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties circuitBreakerProperties) {
        return null;
    }
}
