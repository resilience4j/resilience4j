package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import java.util.List;

public class CircuitBreakerEventsEndpointResponse {
    private List<CircuitBreakerEventDTO> circuitBreakerEvents;

    public CircuitBreakerEventsEndpointResponse(){
    }

    public CircuitBreakerEventsEndpointResponse(List<CircuitBreakerEventDTO> circuitBreakerEvents){
        this.circuitBreakerEvents = circuitBreakerEvents;
    }

    public List<CircuitBreakerEventDTO> getCircuitBreakerEvents() {
        return circuitBreakerEvents;
    }

    public void setCircuitBreakerEvents(List<CircuitBreakerEventDTO> circuitBreakerEvents) {
        this.circuitBreakerEvents = circuitBreakerEvents;
    }
}
