package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.core.lang.Nullable;

import java.util.List;

public class CircuitBreakerEventsEndpointResponse {

    @Nullable
    private List<CircuitBreakerEventDTO> circuitBreakerEvents;

    public CircuitBreakerEventsEndpointResponse() {
    }

    public CircuitBreakerEventsEndpointResponse(
        @Nullable List<CircuitBreakerEventDTO> circuitBreakerEvents) {
        this.circuitBreakerEvents = circuitBreakerEvents;
    }

    @Nullable
    public List<CircuitBreakerEventDTO> getCircuitBreakerEvents() {
        return circuitBreakerEvents;
    }

    public void setCircuitBreakerEvents(
        @Nullable List<CircuitBreakerEventDTO> circuitBreakerEvents) {
        this.circuitBreakerEvents = circuitBreakerEvents;
    }
}
