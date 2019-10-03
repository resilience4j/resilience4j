package io.github.resilience4j.circuitbreaker;

/**
 * A {@link IllegalStateTransitionException} signals that someone tried to trigger an illegal state
 * transition..
 */
public class IllegalStateTransitionException extends RuntimeException {

    private final String name;
    private final CircuitBreaker.State fromState;
    private final CircuitBreaker.State toState;

    IllegalStateTransitionException(String name, CircuitBreaker.State fromState,
        CircuitBreaker.State toState) {
        super(String
            .format("CircuitBreaker '%s' tried an illegal state transition from %s to %s", name,
                fromState.toString(), toState.toString()));
        this.name = name;
        this.fromState = fromState;
        this.toState = toState;
    }

    public CircuitBreaker.State getFromState() {
        return fromState;
    }

    public CircuitBreaker.State getToState() {
        return toState;
    }

    public String getName() {
        return name;
    }
}
