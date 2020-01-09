package io.github.resilience4j.common.circuitbreaker.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.CustomizerWithName;

/**
 * Enable customization circuit breaker configuration builders programmatically.
 */
public interface CircuitBreakerConfigCustomizer extends CustomizerWithName {

    /**
     * Customize circuit breaker configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(CircuitBreakerConfig.Builder configBuilder);

}
