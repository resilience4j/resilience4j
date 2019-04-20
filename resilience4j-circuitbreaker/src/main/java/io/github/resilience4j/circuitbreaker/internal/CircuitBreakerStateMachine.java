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

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
    private Clock clock;
    private SchedulerFactory schedulerFactory;

    /**
     * Creates a circuitBreaker.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     * @param clock A Clock which can be mocked in tests.
     * @param schedulerFactory A SchedulerFactory which can be mocked in tests.
     */
    CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig, Clock clock, SchedulerFactory schedulerFactory) {
        this.name = name;
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.stateReference = new AtomicReference<>(new ClosedState(this));
        this.eventProcessor = new CircuitBreakerEventProcessor();
        this.clock = clock;
        this.schedulerFactory = schedulerFactory;
    }

    /**
     * Creates a circuitBreaker.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     * @param schedulerFactory A SchedulerFactory which can be mocked in tests.
     */
    public CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig, SchedulerFactory schedulerFactory) {
        this(name, circuitBreakerConfig, Clock.systemUTC(), schedulerFactory);
    }

    /**
     * Creates a circuitBreaker.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     */
    public CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig, Clock clock) {
        this(name, circuitBreakerConfig, clock, SchedulerFactory.getInstance());
    }

    /**
     * Creates a circuitBreaker.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     */
    public CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig) {
       this(name, circuitBreakerConfig, Clock.systemUTC());
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
        // Handle the case if the completable future throw CompletionException wrapping the original exception
        // where original exception is the the one to retry not the CompletionException.
        Predicate<Throwable> recordFailurePredicate = circuitBreakerConfig.getRecordFailurePredicate();
        if (throwable instanceof CompletionException) {
            Throwable cause = throwable.getCause();
            handleThrowable(durationInNanos, recordFailurePredicate, cause);
        }else{
            handleThrowable(durationInNanos, recordFailurePredicate, throwable);
        }
    }

    private void handleThrowable(long durationInNanos, Predicate<Throwable> recordFailurePredicate, Throwable throwable) {
        if (recordFailurePredicate.test(throwable)) {
            LOG.debug("CircuitBreaker '{}' recorded a failure:", name, throwable);
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
    public void reset() {
        CircuitBreakerState previousState = stateReference.getAndUpdate(currentState -> new ClosedState(this));
        if (previousState.getState() != CLOSED) {
            publishStateTransitionEvent(StateTransition.transitionBetween(previousState.getState(), CLOSED));
        }
        publishResetEvent();
    }

    private void stateTransition(State newState, UnaryOperator<CircuitBreakerState> newStateGenerator) {
        CircuitBreakerState previousState = stateReference.getAndUpdate(currentState -> {
            if (currentState.getState() == newState) {
                return currentState;
            }
            return newStateGenerator.apply(currentState);
        });
        if (previousState.getState() != newState) {
            publishStateTransitionEvent(StateTransition.transitionBetween(previousState.getState(), newState));
        }
    }


    @Override
    public void transitionToDisabledState() {
        stateTransition(DISABLED, currentState -> new DisabledState(this));
    }

    @Override
    public void transitionToForcedOpenState() {
        stateTransition(FORCED_OPEN, currentState -> new ForcedOpenState(this));
    }

    @Override
    public void transitionToClosedState() {
        stateTransition(CLOSED, currentState -> new ClosedState(this, currentState.getMetrics()));
    }

    @Override
    public void transitionToOpenState() {
        stateTransition(OPEN, currentState -> new OpenState(this, currentState.getMetrics(), schedulerFactory));
    }

    @Override
    public void transitionToHalfOpenState() {
        stateTransition(HALF_OPEN, currentState -> new HalfOpenState(this));
    }


    private boolean shouldPublishEvents(CircuitBreakerEvent event) {
        return stateReference.get().shouldPublishEvents(event);
    }


    private void publishEventIfPossible(CircuitBreakerEvent event) {
        if(shouldPublishEvents(event)) {
            if (eventProcessor.hasConsumers()) {
                LOG.debug("Event {} published: {}", event.getEventType(), event);
                try{
                    eventProcessor.consumeEvent(event);
                }catch (Throwable t){
                    LOG.warn("Failed to handle event {}", event.getEventType(), t);
                }
            } else {
                LOG.debug("No Consumers: Event {} not published", event.getEventType());
            }
        } else {
            LOG.debug("Publishing not allowed: Event {} not published", event.getEventType());
        }
    }

    private void publishStateTransitionEvent(final StateTransition stateTransition) {
        final CircuitBreakerOnStateTransitionEvent event = new CircuitBreakerOnStateTransitionEvent(name, stateTransition);
        publishEventIfPossible(event);
    }

    private void publishResetEvent() {
        final CircuitBreakerOnResetEvent event = new CircuitBreakerOnResetEvent(name);
        publishEventIfPossible(event);
    }

    private void publishCallNotPermittedEvent() {
        final CircuitBreakerOnCallNotPermittedEvent event = new CircuitBreakerOnCallNotPermittedEvent(name);
        publishEventIfPossible(event);
    }

    private void publishSuccessEvent(final long durationInNanos) {
        final CircuitBreakerOnSuccessEvent event = new CircuitBreakerOnSuccessEvent(name, Duration.ofNanos(durationInNanos));
        publishEventIfPossible(event);
    }

    private void publishCircuitErrorEvent(final String name, final long durationInNanos, final Throwable throwable) {
        final CircuitBreakerOnErrorEvent event = new CircuitBreakerOnErrorEvent(name, Duration.ofNanos(durationInNanos), throwable);
        publishEventIfPossible(event);
    }

    private void publishCircuitIgnoredErrorEvent(String name, long durationInNanos, Throwable throwable) {
        final CircuitBreakerOnIgnoredErrorEvent event = new CircuitBreakerOnIgnoredErrorEvent(name, Duration.ofNanos(durationInNanos), throwable);
        publishEventIfPossible(event);
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    Clock getClock() {
        return clock;
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
        public EventPublisher onReset(EventConsumer<CircuitBreakerOnResetEvent> onResetEventConsumer) {
            registerConsumer(CircuitBreakerOnResetEvent.class, onResetEventConsumer);
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
