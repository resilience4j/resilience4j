package io.github.resilience4j.circuitbreaker;

/**
 * A {@link IllegalStateTransitionException} signals that someone tried to trigger an illegal state transition..
 */
public class IllegalStateTransitionException extends RuntimeException {

    private CircuitBreaker.State fromState;
    private CircuitBreaker.State toState;

    IllegalStateTransitionException(CircuitBreaker.State fromState, CircuitBreaker.State toState) {
        super(String.format("Illegal state transition from %s to %s", fromState.toString(), toState.toString()));
        this.fromState = fromState;
        this.toState = toState;
    }

    public CircuitBreaker.State getFromState() {
        return fromState;
    }

    public CircuitBreaker.State getToState() {
        return toState;
    }
}
