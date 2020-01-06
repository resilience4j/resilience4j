package io.github.resilience4j.common.circuitbreaker.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/**
 * Enable customization circuit breaker configuration builders programmatically.
 */
public interface CircuitBreakerConfigCustomizer {

    /**
     * Customize circuit breaker configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(CircuitBreakerConfig.Builder configBuilder);

    /**
     * @return name of the circuit breaker instance to be customized
     */
    String name();
}
