package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.core.lang.Nullable;

import java.util.Map;

public class CircuitBreakerEndpointResponse {

    @Nullable
    private Map<String, CircuitBreakerDetails> circuitBreakers;

    public CircuitBreakerEndpointResponse() {
    }

    public CircuitBreakerEndpointResponse(Map<String, CircuitBreakerDetails> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }

    @Nullable
    public Map<String, CircuitBreakerDetails> getCircuitBreakers() {
        return circuitBreakers;
    }

    public void setCircuitBreakers(@Nullable Map<String, CircuitBreakerDetails> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }
}
