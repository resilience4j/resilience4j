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
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.vavr.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
    private final FlowableProcessor<CircuitBreakerEvent> eventPublisher;
    private final Lazy<EventConsumer> lazyEventConsumer;

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
        PublishProcessor<CircuitBreakerEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
        this.lazyEventConsumer = Lazy.of(() -> new EventDispatcher(getEventStream()));
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
        if (eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(new CircuitBreakerOnStateTransitionEvent(name, stateTransition));
        }
    }

    private void publishCallNotPermittedEvent() {
        if (eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(new CircuitBreakerOnCallNotPermittedEvent(name));
        }
    }

    private void publishSuccessEvent(final long durationInNanos) {
        if (eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(new CircuitBreakerOnSuccessEvent(name, Duration.ofNanos(durationInNanos)));
        }
    }

    private void publishCircuitErrorEvent(final String name, final long durationInNanos, final Throwable throwable) {
        if (eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(new CircuitBreakerOnErrorEvent(name, Duration.ofNanos(durationInNanos), throwable));
        }
    }

    private void publishCircuitIgnoredErrorEvent(String name, long durationInNanos, Throwable throwable) {
        if (eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(new CircuitBreakerOnIgnoredErrorEvent(name, Duration.ofNanos(durationInNanos), throwable));
        }
    }

    public Flowable<CircuitBreakerEvent> getEventStream() {
        return eventPublisher;
    }

    @Override
    public EventConsumer getEventConsumer() {
        return lazyEventConsumer.get();
    }

    private class EventDispatcher implements EventConsumer, io.reactivex.functions.Consumer<CircuitBreakerEvent> {

        private volatile Consumer<CircuitBreakerOnSuccessEvent> onSuccessEventConsumer;
        private volatile Consumer<CircuitBreakerOnErrorEvent> onErrorEventConsumer;
        private volatile Consumer<CircuitBreakerOnStateTransitionEvent> onStateTransitionEventConsumer;
        private volatile Consumer<CircuitBreakerOnIgnoredErrorEvent> onIgnoredErrorEventConsumer;
        private volatile Consumer<CircuitBreakerOnCallNotPermittedEvent> onCallNotPermittedEventConsumer;

        EventDispatcher(Flowable<CircuitBreakerEvent> eventStream) {
            eventStream.subscribe(this);
        }

        @Override
        public EventConsumer onSuccess(Consumer<CircuitBreakerOnSuccessEvent> onSuccessEventConsumer) {
            this.onSuccessEventConsumer = onSuccessEventConsumer;
            return this;
        }

        @Override
        public EventConsumer onError(Consumer<CircuitBreakerOnErrorEvent> onErrorEventConsumer) {
            this.onErrorEventConsumer = onErrorEventConsumer;
            return this;
        }

        @Override
        public EventConsumer onStateTransition(Consumer<CircuitBreakerOnStateTransitionEvent> onStateTransitionEventConsumer) {
            this.onStateTransitionEventConsumer = onStateTransitionEventConsumer;
            return this;
        }

        @Override
        public EventConsumer onIgnoredError(Consumer<CircuitBreakerOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
            this.onIgnoredErrorEventConsumer = onIgnoredErrorEventConsumer;
            return this;
        }

        @Override
        public EventConsumer onCallNotPermitted(Consumer<CircuitBreakerOnCallNotPermittedEvent> onCallNotPermittedEventConsumer) {
            this.onCallNotPermittedEventConsumer = onCallNotPermittedEventConsumer;
            return this;
        }

        @Override
        public void accept(CircuitBreakerEvent event) throws Exception {
            CircuitBreakerEvent.Type eventType = event.getEventType();
            switch (eventType) {
                case SUCCESS:
                    if(onSuccessEventConsumer != null){
                        onSuccessEventConsumer.accept((CircuitBreakerOnSuccessEvent) event);
                    }
                    break;
                case ERROR:
                    if(onErrorEventConsumer != null) {
                        onErrorEventConsumer.accept((CircuitBreakerOnErrorEvent) event);
                    }
                    break;
                case STATE_TRANSITION:
                    if(onStateTransitionEventConsumer != null) {
                        onStateTransitionEventConsumer.accept((CircuitBreakerOnStateTransitionEvent) event);
                    }
                    break;
                case IGNORED_ERROR:
                    if(onIgnoredErrorEventConsumer != null) {
                        onIgnoredErrorEventConsumer.accept((CircuitBreakerOnIgnoredErrorEvent) event);
                    }
                    break;
                case NOT_PERMITTED:
                    if(onCallNotPermittedEventConsumer != null){
                        onCallNotPermittedEventConsumer.accept((CircuitBreakerOnCallNotPermittedEvent) event);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
