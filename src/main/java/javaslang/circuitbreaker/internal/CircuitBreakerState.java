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
package javaslang.circuitbreaker.internal;

import javaslang.circuitbreaker.CircuitBreaker;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * States of the CircuitBreaker state machine.
 */
abstract class CircuitBreakerState {

    protected CircuitBreakerStateMachine stateMachine;
    protected final LongAdder numOfFailures;
    protected final AtomicReference<Instant> retryAfter;

    CircuitBreakerState(CircuitBreakerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.numOfFailures = new LongAdder();
        this.retryAfter = new AtomicReference<>(Instant.now());
    }

    CircuitBreakerState(CircuitBreakerStateMachine stateMachine, CircuitBreakerState currentState) {
        this.stateMachine = stateMachine;
        this.numOfFailures = currentState.getNumOfFailures();
        this.retryAfter = currentState.getRetryAfter();
    }

    private LongAdder getNumOfFailures(){
        return numOfFailures;
    }

    private AtomicReference<Instant> getRetryAfter(){
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
