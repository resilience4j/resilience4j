package io.github.resilience4j.circuitbreaker.event;

public class CircuitBreakerOnSlowCallRateExceededEvent extends AbstractCircuitBreakerEvent {

    private final float slowCallRate;

    public CircuitBreakerOnSlowCallRateExceededEvent(String circuitBreakerName, float slowCallRate) {
        super(circuitBreakerName);
        this.slowCallRate = slowCallRate;
    }

    public float getSlowCallRate() {
        return slowCallRate;
    }

    @Override
    public Type getEventType() {
        return Type.SLOW_CALL_RATE_EXCEEDED;
    }

    @Override
    public String toString() {
        return String
            .format("%s: CircuitBreaker '%s' exceeded slow call rate threshold. Current slow call rate: %s",
                getCreationTime(),
                getCircuitBreakerName(),
                getSlowCallRate());
    }
}
