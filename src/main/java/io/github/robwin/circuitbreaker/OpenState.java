package io.github.robwin.circuitbreaker;

final public class OpenState extends CircuitBreakerState {

    OpenState(CircuitBreakerStateMachine stateMachine, CircuitBreakerState currentState) {
        super(stateMachine, currentState);
    }

    /**
     * Requests permission to call this circuitBreaker's backend.
     *
     * @return boolean whether a call should be permitted
     */
    @Override
    public boolean isCallPermitted() {
        if (System.currentTimeMillis() >= retryAfter.get()) {
            stateMachine.transitionToHalfClosedState(this);
            return true;
        }
        return false;
    }

    /**
     * Records a backend failure.
     * This must be called if a call to this backend fails
     */
    @Override
    public void recordFailure() {
        numOfFailures.incrementAndGet();
    }

    /**
     * Records success of a call to this backend.
     * This must be called after a successful call.
     */
    @Override
    public void recordSuccess() {
        stateMachine.transitionToInitialClosedState();
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    public CircuitBreaker.State getState() {
        return CircuitBreaker.State.OPEN;
    }
}
