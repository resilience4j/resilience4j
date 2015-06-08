/*
 *
 *  Copyright 2015 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
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
     * Requests permission to call a circuitBreaker's backend.
     *
     * @return boolean whether a call should be permitted
     */
    abstract boolean isCallPermitted();

    /**
     * Records a backend failure.
     * This must be called if a call to a backend fails
     */
    abstract void recordFailure();

    /**
     * Records success of a call to this backend.
     * This must be called after a successful call.
     */
    abstract void recordSuccess();

    /**
     * Get the state of the CircuitBreaker
     *
     * @return the state of the CircuitBreaker
     */
    abstract CircuitBreaker.State getState();
}
