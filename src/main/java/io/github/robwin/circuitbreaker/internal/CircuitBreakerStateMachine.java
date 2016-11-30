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
package io.github.robwin.circuitbreaker.internal;


import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.event.*;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A CircuitBreaker finite state machine.
 */
public final class CircuitBreakerStateMachine implements CircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerStateMachine.class);

    private final String name;
    private final AtomicReference<CircuitBreakerState> stateReference;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final PublishProcessor<CircuitBreakerEvent> eventPublisher;

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
        this.eventPublisher = PublishProcessor.create();
    }

    /**
     * Creates a circuitBreaker with default config.
     *
     * @param name      the name of the CircuitBreaker
     */
    public CircuitBreakerStateMachine(String name) {
        this(name, CircuitBreakerConfig.ofDefaults());
    }

    /**
     * Creates a circuitBreaker.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration supplier.
     */
    public CircuitBreakerStateMachine(String name, Supplier<CircuitBreakerConfig> circuitBreakerConfig) {
        this(name, circuitBreakerConfig.get());
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
    public synchronized void onError(Throwable throwable) {
        if(circuitBreakerConfig.getRecordFailurePredicate().test(throwable)){
            if(LOG.isDebugEnabled()){
                LOG.debug(String.format("CircuitBreaker '%s' recorded a failure:", name), throwable);
            }
            publishCircuitBreakerEvent(new CircuitBreakerOnErrorEvent(getName(), throwable));
            stateReference.get().onError(throwable);
        }
        publishCircuitBreakerEvent(new CircuitBreakerOnErrorIgnoredEvent(getName(), throwable));
    }

    /**
     * Records a successful call.
     */
    @Override
    public synchronized void onSuccess() {
        publishCircuitBreakerEvent(new CircuitBreakerOnSuccessEvent(getName()));
        stateReference.get().onSuccess();
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

    synchronized void transitionToClosedState(StateTransition stateTransition) {
        stateReference.set(new ClosedState(this));
        publishStateTransitionEvent(stateTransition);
    }

    synchronized void transitionToOpenState(StateTransition stateTransition, CircuitBreakerMetrics circuitBreakerMetrics) {
        stateReference.set(new OpenState(this, circuitBreakerMetrics));
        publishStateTransitionEvent(stateTransition);
    }

    synchronized void transitionToHalfClosedState(StateTransition stateTransition) {
        stateReference.set(new HalfOpenState(this));
        publishStateTransitionEvent(stateTransition);
    }

    private void publishStateTransitionEvent(StateTransition stateTransition){
        if(LOG.isDebugEnabled()){
            LOG.debug(String.format("CircuitBreaker '%s' changed state from %s to %s", name, stateTransition.getFromState(), stateTransition.getToState()));
        }
        publishCircuitBreakerEvent(new CircuitBreakerOnStateTransitionEvent(getName(), stateTransition));
    }

    private void publishCircuitBreakerEvent(CircuitBreakerEvent event) {
        if(eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(event);
        }
    }

    public Flowable<CircuitBreakerEvent> getEventStream(){
        return eventPublisher;
    }
}
