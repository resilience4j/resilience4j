package io.github.robwin.circuitbreaker;

import com.codahale.metrics.health.HealthCheck;

public class CircuitBreakerHealthCheck extends HealthCheck {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerHealthCheck(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public HealthCheck.Result check() throws Exception {
        CircuitBreaker.State state = circuitBreakerRegistry.circuitBreaker("testName").getState();
        switch(state){
            case CLOSED: return HealthCheck.Result.healthy();
            case HALF_CLOSED: return HealthCheck.Result.healthy();
            default: return HealthCheck.Result.unhealthy(String.format("CircuitBreaker '%s' is OPEN.", "testName"));
        }
    }
}