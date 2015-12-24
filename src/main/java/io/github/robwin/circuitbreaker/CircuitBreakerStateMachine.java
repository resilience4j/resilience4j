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


import java.util.concurrent.atomic.AtomicReference;

/**
 * CircuitBreaker finite state machine.
 * This CircuitBreaker is implemented via a finite state machine. It does not have a way to know anything about the
 * backend's state by itself, but uses only the information provided by calls to {@link #recordSuccess()} and
 * {@link #recordFailure(java.lang.Throwable)}.
 * The state of the CircuitBreaker changes from `CLOSED` to `OPEN` if a (configurable) number of call attempts have failed consecutively.
 * Then, all access to the backend is blocked for a (configurable) time interval. After that, the CircuitBreaker state changes to `HALF_CLOSED` tentatively, to see if the backend is still dead or has become available again.
 * On success or failure, the state changes back to `CLOSED` or `OPEN`, respectively.
 */
final class CircuitBreakerStateMachine implements CircuitBreaker {

    private final String name;
    private AtomicReference<CircuitBreakerState> stateReference;
    private final CircuitBreakerConfig circuitBreakerConfig;

    /**
     * Creates a circuitBreaker.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     */
    public CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig) {
        this.name = name;
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.stateReference = new AtomicReference<>(new ClosedState(this));
    }

    CircuitBreakerConfig getCircuitBreakerConfig(){
        return this.circuitBreakerConfig;
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
        if(circuitBreakerConfig.getIgnoredExceptions().stream()
                .noneMatch(ignoredException -> ignoredException.isInstance(throwable))){
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("CircuitBreaker '%s'", this.name);
    }

    void resetState(CircuitBreakerState currentState) {
        stateReference.set(new ClosedState(this));
        circuitBreakerConfig.getCircuitBreakerEventListener()
                .ifPresent(listener ->
                        listener.onCircuitBreakerEvent(new CircuitBreakerStateChangeEvent(currentState.getState(), State.CLOSED)));
    }

    void transitionToOpenState(CircuitBreakerState currentState) {
        stateReference.set(new OpenState(this, currentState));
        circuitBreakerConfig.getCircuitBreakerEventListener()
                .ifPresent(listener ->
                        listener.onCircuitBreakerEvent(new CircuitBreakerStateChangeEvent(currentState.getState(), State.OPEN)));
    }

    void transitionToHalfClosedState(CircuitBreakerState currentState) {
        stateReference.set(new HalfClosedState(this, currentState));
        circuitBreakerConfig.getCircuitBreakerEventListener()
                .ifPresent(listener ->
                        listener.onCircuitBreakerEvent(new CircuitBreakerStateChangeEvent(currentState.getState(), State.HALF_CLOSED)));
    }
}
