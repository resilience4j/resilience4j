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


import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import javaslang.circuitbreaker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A CircuitBreaker finite state machine.
 */
final class CircuitBreakerStateMachine implements CircuitBreaker{

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerStateMachine.class);

    private final String name;
    private final AtomicReference<CircuitBreakerState> stateReference;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final PublishSubject<CircuitBreakerEvent> eventPublisher;

    /**
     * Creates a circuitBreaker.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     */
    CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig) {
        this.name = name;
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.stateReference = new AtomicReference<>(new ClosedState(this));
        this.eventPublisher = PublishSubject.create();
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
     * Records a failed call.
     */
    @Override
    public synchronized void recordFailure(Throwable throwable) {
        if(circuitBreakerConfig.getRecordFailurePredicate().test(throwable)){
            eventPublisher.onNext(new CircuitBreakerRecordedFailureEvent(getName(), throwable));
            //circuitBreakerConfig.getCircuitBreakerEventListener().onCircuitBreakerEvent(new CircuitBreakerRecordedFailureEvent(getName(), throwable));
            stateReference.get().recordFailure(throwable);
        }
    }

    /**
     * Records a successful call.
     */
    @Override
    public void recordSuccess() {
        stateReference.get().recordSuccess();
    }

    /**
     * Get the state of this CircuitBreaker.
     *
     * @return the the state of this CircuitBreaker
     */
    @Override
    public State getState() {
        return this.stateReference.get().getState();
    }

    /**
     * Get the name of this CircuitBreaker.
     *
     * @return the the name of this CircuitBreaker
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the config of this CircuitBreaker.
     *
     * @return the config of this CircuitBreaker
     */
    @Override
    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    @Override
    public Metrics getMetrics() {
        return this.stateReference.get().getMetrics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("CircuitBreaker '%s'", this.name);
    }

    void transitionToClosedState(StateTransition stateTransition) {
        stateReference.set(new ClosedState(this));
        publishStateTransitionEvent(stateTransition);
        //circuitBreakerConfig.getCircuitBreakerEventListener().onCircuitBreakerEvent(new CircuitBreakerStateTransitionEvent(getName(), stateTransition));
    }

    void transitionToOpenState(StateTransition stateTransition, CircuitBreakerMetrics circuitBreakerMetrics) {
        stateReference.set(new OpenState(this, circuitBreakerMetrics));
        publishStateTransitionEvent(stateTransition);
        //circuitBreakerConfig.getCircuitBreakerEventListener().onCircuitBreakerEvent(new CircuitBreakerStateTransitionEvent(getName(), stateTransition));
    }

    void transitionToHalfClosedState(StateTransition stateTransition) {
        stateReference.set(new HalfOpenState(this));
        publishStateTransitionEvent(stateTransition);
        //circuitBreakerConfig.getCircuitBreakerEventListener().onCircuitBreakerEvent(new CircuitBreakerStateTransitionEvent(getName(), stateTransition));
    }

    private void publishStateTransitionEvent(StateTransition stateTransition){
        eventPublisher.onNext(new CircuitBreakerStateTransitionEvent(getName(), stateTransition));
    }

    public Observable<CircuitBreakerEvent> observeCircuitBreakerEvents(){
        return eventPublisher;
    }
}
