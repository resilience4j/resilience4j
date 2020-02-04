package io.github.resilience4j.common.circuitbreaker.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.CustomizerWithName;
import io.github.resilience4j.core.lang.NonNull;

import java.util.function.Consumer;

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

    /**
     * A convenient method to create CircuitBreakerConfigCustomizer using {@link Consumer}
     *
     * @param instanceName the name of the instance
     * @param consumer     delegate call to Consumer when  {@link CircuitBreakerConfigCustomizer#customize(CircuitBreakerConfig.Builder)}
     *                     is called
     * @return Customizer instance
     */
    static CircuitBreakerConfigCustomizer of(@NonNull String instanceName,
        @NonNull Consumer<CircuitBreakerConfig.Builder> consumer) {
        return new CircuitBreakerConfigCustomizer() {

            @Override
            public void customize(CircuitBreakerConfig.Builder builder) {
                consumer.accept(builder);
            }

            @Override
            public String name() {
                return instanceName;
            }
        };
    }

}
