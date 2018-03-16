package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import java.util.List;

public class CircuitBreakerEndpointResponse {
    private List<String> circuitBreakers;

    public CircuitBreakerEndpointResponse(){
    }

    public CircuitBreakerEndpointResponse(List<String> circuitBreakers){
        this.circuitBreakers = circuitBreakers;
    }

    public List<String> getCircuitBreakers() {
        return circuitBreakers;
    }

    public void setCircuitBreakers(List<String> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }
}
