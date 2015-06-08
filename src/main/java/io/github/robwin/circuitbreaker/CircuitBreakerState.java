package io.github.robwin.circuitbreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * States of the CircuitBreaker state machine.
 */
abstract class CircuitBreakerState {

    protected CircuitBreakerStateMachine stateMachine;
    protected int maxFailures;
    protected long waitInterval;

    protected final AtomicInteger numOfFailures;
    protected final AtomicLong retryAfter;

    public CircuitBreakerState(CircuitBreakerStateMachine stateMachine, int numOfFailures, long retryAfter) {
        this.stateMachine = stateMachine;
        this.numOfFailures = new AtomicInteger(numOfFailures);
        this.retryAfter = new AtomicLong(retryAfter);
        this.maxFailures = stateMachine.getCircuitBreakerConfig().getMaxFailures();
        this.waitInterval = stateMachine.getCircuitBreakerConfig().getWaitInterval();
    }

    public CircuitBreakerState(CircuitBreakerStateMachine stateMachine, CircuitBreakerState currentState) {
        this.stateMachine = stateMachine;
        this.numOfFailures = currentState.getNumOfFailures();
        this.retryAfter = currentState.getRetryAfter();
        this.maxFailures = stateMachine.getCircuitBreakerConfig().getMaxFailures();
        this.waitInterval = stateMachine.getCircuitBreakerConfig().getWaitInterval();
    }

    private AtomicInteger getNumOfFailures(){
        return numOfFailures;
    }

    private AtomicLong getRetryAfter(){
        return retryAfter;
    }

    /**
     * Requests permission to call this circuitBreaker's backend.
     *
     * @return boolean whether a call should be permitted
     */
    abstract boolean isCallPermitted();

    /**
     * Records a backend failure.
     * This must be called if a call to this backend fails
     */
    abstract void recordFailure();

    /**
     * Records success of a call to this backend.
     * This must be called after a successful call.
     */
    abstract void recordSuccess();

    /**
     * Get the state of the CircuitBreaker
     */
    abstract CircuitBreaker.State getState();
}
