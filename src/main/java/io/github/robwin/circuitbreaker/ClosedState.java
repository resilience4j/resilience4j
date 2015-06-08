package io.github.robwin.circuitbreaker;

final public class ClosedState extends CircuitBreakerState {

    ClosedState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine, 0, 0);
    }

    /**
     * Requests permission to call this circuitBreaker's backend.
     *
     * @return boolean whether a call should be permitted
     */
    @Override
    public boolean isCallPermitted() {
        return true;
    }

    /**
     * Records a backend failure.
     * This must be called if a call to this backend fails
     */
    @Override
    public void recordFailure() {
        // if CLOSED, increase number of failures
        int currentNumOfFailures = numOfFailures.incrementAndGet();
        if (currentNumOfFailures > this.maxFailures) {
            // Too many failures, set new retryAfter to current time + wait interval
            retryAfter.set(System.currentTimeMillis() + this.waitInterval);
            stateMachine.transitionToOpenState(this);
        }
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
        return CircuitBreaker.State.CLOSED;
    }
}
