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


import javaslang.circuitbreaker.CircuitBreakerConfig;
import javaslang.circuitbreaker.CircuitBreakerStateTransitionEvent;
import javaslang.circuitbreaker.CircuitBreaker;
import javaslang.circuitbreaker.CircuitBreakerEventListener;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * A CircuitBreaker finite state machine. The CircuitBreaker does not have a way to know anything about the
 * backend's state by itself, but uses only the information provided by calls to {@link #recordSuccess()} and
 * {@link #recordFailure(java.lang.Throwable)}.
 * The state of the CircuitBreaker changes from `CLOSED` to `OPEN` if a (configurable) number of call attempts have failed consecutively.
 * Then, all access to the backend is blocked for a (configurable) time interval. After that, the CircuitBreaker state changes to `HALF_CLOSED` tentatively, to see if the backend is still dead or has become available again.
 * On success or failure, the state changes back to `CLOSED` or `OPEN`, respectively.
 */
final class CircuitBreakerStateMachine implements CircuitBreaker {

    private final String name;
    private AtomicReference<CircuitBreakerState> stateReference;
    private Predicate<Throwable> exceptionPredicate;
    private CircuitBreakerEventListener circuitBreakerEventListener;
    private int maxFailures;
    private Duration waitDuration;

    /**
     * Creates a circuitBreaker.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     */
    CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig) {
        this.name = name;
        this.stateReference = new AtomicReference<>(new ClosedState(this));
        this.exceptionPredicate = circuitBreakerConfig.getExceptionPredicate();
        this.circuitBreakerEventListener = circuitBreakerConfig.getCircuitBreakerEventListener();
        this.maxFailures = circuitBreakerConfig.getMaxFailures();
        this.waitDuration = circuitBreakerConfig.getWaitDuration();
    }

    /**
     * Requests permission to call this backend.
     *
     * @return true, if the call is allowed.
     */
    @Override
    public boolean isCallPermitted() {
        return stateReference.get().isCallPermitted();
    }

    /**
     * Records a failure.
     */
    @Override
    public void recordFailure(Throwable throwable) {
        if(exceptionPredicate.test(throwable)){
            stateReference.get().recordFailure();
        }
    }

    /**
     * Records a success.
     */
    @Override
    public void recordSuccess() {
        this.stateReference.get().recordSuccess();
    }

    /**
     * Get the state of the CircuitBreaker
     */
    @Override
    public State getState() {
        return this.stateReference.get().getState();
    }

    /**
     * Get the name of the CircuitBreaker
     */
    @Override
    public String getName() {
        return this.name;
    }

    public int getMaxFailures() {
        return maxFailures;
    }

    public Duration getWaitDuration() {
        return waitDuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("CircuitBreaker '%s'", this.name);
    }

    void resetState() {
        stateReference.set(new ClosedState(this));
    }

    void transitionToClosedState(StateTransition stateTransition) {
        stateReference.set(new ClosedState(this));
        circuitBreakerEventListener.onCircuitBreakerEvent(new CircuitBreakerStateTransitionEvent(getName(), stateTransition));
    }

    void transitionToOpenState(CircuitBreakerState currentState, StateTransition stateTransition) {
        stateReference.set(new OpenState(this, currentState));
        circuitBreakerEventListener.onCircuitBreakerEvent(new CircuitBreakerStateTransitionEvent(getName(), stateTransition));
    }

    void transitionToHalfClosedState(CircuitBreakerState currentState, StateTransition stateTransition) {
        stateReference.set(new HalfClosedState(this, currentState));
        circuitBreakerEventListener.onCircuitBreakerEvent(new CircuitBreakerStateTransitionEvent(getName(), stateTransition));
    }
}
