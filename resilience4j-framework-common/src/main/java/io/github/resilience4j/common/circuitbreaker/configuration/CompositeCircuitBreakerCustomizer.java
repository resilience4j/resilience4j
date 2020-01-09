package io.github.resilience4j.common.circuitbreaker.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * the composite  of any circuit breaker {@link CircuitBreakerConfigCustomizer} implementations.
 */
public class CompositeCircuitBreakerCustomizer {

    private final Map<String, CircuitBreakerConfigCustomizer> customizerMap = new HashMap<>();

    public CompositeCircuitBreakerCustomizer(List<CircuitBreakerConfigCustomizer> customizers) {

        if (customizers != null && !customizers.isEmpty()) {
            customizerMap.putAll(customizers.stream()
                .collect(
                    Collectors.toMap(CircuitBreakerConfigCustomizer::name, Function.identity())));
        }

    }

    /**
     * @param circuitBreakerInstanceName the circuit breaker instance name
     * @return the found {@link CircuitBreakerConfigCustomizer} if any .
     */
    public Optional<CircuitBreakerConfigCustomizer> getCircuitBreakerConfigCustomizer(
        String circuitBreakerInstanceName) {
        return Optional.ofNullable(customizerMap.get(circuitBreakerInstanceName));
    }

}
