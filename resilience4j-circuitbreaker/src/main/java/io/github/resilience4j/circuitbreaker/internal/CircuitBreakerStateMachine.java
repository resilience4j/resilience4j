/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.internal;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.*;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.*;

/**
 * A CircuitBreaker finite state machine.
 */
public final class CircuitBreakerStateMachine implements CircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerStateMachine.class);

    private final String name;
    private final AtomicReference<CircuitBreakerState> stateReference;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final CircuitBreakerEventProcessor eventProcessor;

    /**
     * Creates a circuitBreaker.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     */
    public CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig) {
        this.name = name;
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.stateReference = new AtomicReference<>(new ClosedState(this));
        this.eventProcessor = new CircuitBreakerEventProcessor();
    }

    /**
     * Creates a circuitBreaker with default config.
     *
     * @param name the name of the CircuitBreaker
     */
    public CircuitBreakerStateMachine(String name) {
        this(name, CircuitBreakerConfig.ofDefaults());
    }

    /**
     * Creates a circuitBreaker.
     *
     * @param name                 the name of the CircuitBreaker
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
        boolean callPermitted = stateReference.get().isCallPermitted();
        if (!callPermitted) {
            publishCallNotPermittedEvent();
        }
        return callPermitted;
    }

    @Override
    public void onError(long durationInNanos, Throwable throwable) {
        if (circuitBreakerConfig.getRecordFailurePredicate().test(throwable)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("CircuitBreaker '%s' recorded a failure:", name), throwable);
            }
            publishCircuitErrorEvent(name, durationInNanos, throwable);
            stateReference.get().onError(throwable);
        } else {
            publishCircuitIgnoredErrorEvent(name, durationInNanos, throwable);
        }
    }

    @Override
    public void onSuccess(long durationInNanos) {
        publishSuccessEvent(durationInNanos);
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

    @Override
    public void resetToClosedState() {
        CircuitBreakerState previousState = stateReference.getAndUpdate(currentState -> new ClosedState(this));
        if (previousState.getState() != CLOSED) {
            publishStateResetEvent(StateTransition.transitionToClosedState(previousState.getState()));
        }
    }

    @Override
    public void transitionToClosedState() {
        CircuitBreakerState previousState = stateReference.getAndUpdate(currentState -> {
            if (currentState.getState() == CLOSED) {
                return currentState;
            }
            return new ClosedState(this, currentState.getMetrics());
        });
        if (previousState.getState() != CLOSED) {
            publishStateTransitionEvent(StateTransition.transitionToClosedState(previousState.getState()));
        }

    }

    @Override
    public void transitionToOpenState() {
        CircuitBreakerState previousState = stateReference.getAndUpdate(currentState -> {
            if (currentState.getState() == OPEN) {
                return currentState;
            }
            return new OpenState(this, currentState.getMetrics());
        });
        if (previousState.getState() != OPEN) {
            publishStateTransitionEvent(StateTransition.transitionToOpenState(previousState.getState()));
        }
    }

    @Override
    public void transitionToHalfOpenState() {
        CircuitBreakerState previousState = stateReference.getAndUpdate(currentState -> {
            if (currentState.getState() == HALF_OPEN) {
                return currentState;
            }
            return new HalfOpenState(this);
        });
        if (previousState.getState() != HALF_OPEN) {
            publishStateTransitionEvent(StateTransition.transitionToHalfOpenState(previousState.getState()));
        }
    }

    private void publishStateTransitionEvent(final StateTransition stateTransition) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                String.format("CircuitBreaker '%s' changed state from %s to %s",
                    name, stateTransition.getFromState(), stateTransition.getToState())
            );
        }
        if(eventProcessor.hasConsumers()){
            eventProcessor.consumeEvent(new CircuitBreakerOnStateTransitionEvent(name, stateTransition));
        }

    }

    private void publishStateResetEvent(final StateTransition stateTransition) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                String.format("CircuitBreaker '%s' reset state from %s to %s",
                    name, stateTransition.getFromState(), stateTransition.getToState())
            );
        }
        if(eventProcessor.hasConsumers()){
            eventProcessor.consumeEvent(new CircuitBreakerOnStateResetEvent(name, stateTransition));
        }

    }

    private void publishCallNotPermittedEvent() {
        if(eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(new CircuitBreakerOnCallNotPermittedEvent(name));
        }
    }

    private void publishSuccessEvent(final long durationInNanos) {
        if(eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(new CircuitBreakerOnSuccessEvent(name, Duration.ofNanos(durationInNanos)));
        }
    }

    private void publishCircuitErrorEvent(final String name, final long durationInNanos, final Throwable throwable) {
        if(eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(new CircuitBreakerOnErrorEvent(name, Duration.ofNanos(durationInNanos), throwable));
        }
    }

    private void publishCircuitIgnoredErrorEvent(String name, long durationInNanos, Throwable throwable) {
        if(eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(new CircuitBreakerOnIgnoredErrorEvent(name, Duration.ofNanos(durationInNanos), throwable));
        }
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    private class CircuitBreakerEventProcessor extends EventProcessor<CircuitBreakerEvent> implements EventConsumer<CircuitBreakerEvent>, EventPublisher {
        @Override
        public EventPublisher onSuccess(EventConsumer<CircuitBreakerOnSuccessEvent> onSuccessEventConsumer) {
            registerConsumer(CircuitBreakerOnSuccessEvent.class, onSuccessEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onError(EventConsumer<CircuitBreakerOnErrorEvent> onErrorEventConsumer) {
            registerConsumer(CircuitBreakerOnErrorEvent.class, onErrorEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onStateTransition(EventConsumer<CircuitBreakerOnStateTransitionEvent> onStateTransitionEventConsumer) {
            registerConsumer(CircuitBreakerOnStateTransitionEvent.class, onStateTransitionEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onStateReset(EventConsumer<CircuitBreakerOnStateResetEvent> onStateResetEventConsumer) {
            registerConsumer(CircuitBreakerOnStateResetEvent.class, onStateResetEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onIgnoredError(EventConsumer<CircuitBreakerOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
            registerConsumer(CircuitBreakerOnIgnoredErrorEvent.class, onIgnoredErrorEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCallNotPermitted(EventConsumer<CircuitBreakerOnCallNotPermittedEvent> onCallNotPermittedEventConsumer) {
            registerConsumer(CircuitBreakerOnCallNotPermittedEvent.class, onCallNotPermittedEventConsumer);
            return this;
        }

        @Override
        public void consumeEvent(CircuitBreakerEvent event) {
            super.processEvent(event);
        }
    }
}
