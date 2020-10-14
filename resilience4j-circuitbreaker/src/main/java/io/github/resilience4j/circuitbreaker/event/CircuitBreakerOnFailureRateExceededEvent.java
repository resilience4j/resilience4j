package io.github.resilience4j.circuitbreaker.event;

public class CircuitBreakerOnFailureRateExceededEvent extends AbstractCircuitBreakerEvent {

    private final float failureRate;

    public CircuitBreakerOnFailureRateExceededEvent(String circuitBreakerName, float failureRate) {
        super(circuitBreakerName);
        this.failureRate = failureRate;
    }

    public float getFailureRate() {
        return failureRate;
    }

    @Override
    public Type getEventType() {
        return Type.FAILURE_RATE_EXCEEDED;
    }

    @Override
    public String toString() {
        return String
            .format("%s: CircuitBreaker '%s' exceeded failure rate threshold. Current failure rate: %s",
                getCreationTime(),
                getCircuitBreakerName(),
                getFailureRate());
    }
}
