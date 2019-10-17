package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.core.lang.Nullable;

import java.util.List;

public class CircuitBreakerEndpointResponse {

    @Nullable
    private List<String> circuitBreakers;

    public CircuitBreakerEndpointResponse() {
    }

    public CircuitBreakerEndpointResponse(List<String> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }

    @Nullable
    public List<String> getCircuitBreakers() {
        return circuitBreakers;
    }

    public void setCircuitBreakers(List<String> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }
}
