/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.event.*;
import io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;
import io.github.resilience4j.core.EventConsumer;
import io.vavr.*;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A CircuitBreaker instance is thread-safe can be used to decorate multiple requests.
 *
 * A {@link CircuitBreaker} manages the state of a backend system.
 * The CircuitBreaker is implemented via a finite state machine with three states: CLOSED, OPEN and HALF_OPEN.
 * The CircuitBreaker does not know anything about the backend's state by itself, but uses the information provided by the decorators via
 * {@link CircuitBreaker#onSuccess} and {@link CircuitBreaker#onError} events.
 * Before communicating with the backend, the the permission to do so must be obtained via the method {@link CircuitBreaker#isCallPermitted()}.
 *
 * The state of the CircuitBreaker changes from CLOSED to OPEN when the failure rate is above a (configurable) threshold.
 * Then, all access to the backend is blocked for a (configurable) time duration. {@link CircuitBreaker#isCallPermitted} throws a {@link CircuitBreakerOpenException}, if the CircuitBreaker is OPEN.
 *
 * After the time duration has elapsed, the CircuitBreaker state changes from OPEN to HALF_OPEN and allows calls to see if the backend is still unavailable or has become available again.
 * If the failure rate is above the configured threshold, the state changes back to OPEN. If the failure rate is below or equal to the threshold, the state changes back to CLOSED.
 */
@SuppressWarnings("ALL")
public interface CircuitBreaker {

    /**
     * Requests permission to call this circuitBreaker's backend.
     *
     * @return boolean whether a call should be permitted
     */
    boolean isCallPermitted();

    /**
     * Records a failed call.
     * This method must be invoked when a call failed.
     *
     * @param durationInNanos The elapsed time duration of the call
     * @param throwable The throwable which must be recorded
     */
    void onError(long durationInNanos, Throwable throwable);

     /**
      * Records a successful call.
      *
      * @param durationInNanos The elapsed time duration of the call
      * This method must be invoked when a call was successful.
      */
    void onSuccess(long durationInNanos);


    /**
     * Returns the circuit breaker to its original closed state, losing statistics.
     *
     * Should only be used, when you want to want to fully reset the circuit breaker without creating a new one.
     */
    void reset();

    /**
     * Transitions the state machine to CLOSED state.
     *
     * Should only be used, when you want to force a state transition. State transition are normally done internally.
     */
    void transitionToClosedState();

    /**
     * Transitions the state machine to OPEN state.
     *
     * Should only be used, when you want to force a state transition. State transition are normally done internally.
     */
    void transitionToOpenState();

    /**
     * Transitions the state machine to HALF_OPEN state.
     *
     * Should only be used, when you want to force a state transition. State transition are normally done internally.
     */
    void transitionToHalfOpenState();

    /**
     * Transitions the state machine to a DISABLED state, stopping state transition, metrics and event publishing.
     *
     * Should only be used, when you want to disable the circuit breaker allowing all calls to pass.
     * To recover from this state you must force a new state transition
     */
    void transitionToDisabledState();

    /**
     * Transitions the state machine to a FORCED_OPEN state, stopping state transition, metrics and event publishing.
     *
     * Should only be used, when you want to disable the circuit breaker allowing no call to pass.
     * To recover from this state you must force a new state transition
     */
    void transitionToForcedOpenState();

    /**
     * Returns the name of this CircuitBreaker.
     *
     * @return the name of this CircuitBreaker
     */
    String getName();

    /**
     * Returns the state of this CircuitBreaker.
     *
     * @return the state of this CircuitBreaker
     */
    State getState();

    /**
     * Returns the CircuitBreakerConfig of this CircuitBreaker.
     *
     * @return the CircuitBreakerConfig of this CircuitBreaker
     */
    CircuitBreakerConfig getCircuitBreakerConfig();

    /**
     * Returns the Metrics of this CircuitBreaker.
     *
     * @return the Metrics of this CircuitBreaker
     */
    Metrics getMetrics();

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T> the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier){
        return decorateSupplier(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     *
     * @return the result of the decorated Callable.
     * @param <T> the result type of callable
     * @throws Exception if unable to compute a result
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception{
        return decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable){
        decorateRunnable(this, runnable).run();
    }

    /**
     * Decorates and executes the decorated CompletionStage.
     *
     * @param supplier the original CompletionStage
     * @param <T> the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier){
        return decorateCompletionStage(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    default <T> T executeCheckedSupplier(CheckedFunction0<T> checkedSupplier) throws Throwable {
        return decorateCheckedSupplier(this, checkedSupplier).apply();
    }

    /**
     * States of the CircuitBreaker state machine.
     */
    enum State {
         /** A DISABLED breaker is not operating (no state transition, no events)
          and allowing all requests through. */
        DISABLED(3, false),
        /** A CLOSED breaker is operating normally and allowing
         requests through. */
        CLOSED(0, true),
        /** An OPEN breaker has tripped and will not allow requests
         through. */
        OPEN(1, true),
        /** A FORCED_OPEN breaker is not operating (no state transition, no events)
         and not allowing any requests through. */
        FORCED_OPEN(4, false),
        /** A HALF_OPEN breaker has completed its wait interval
         and will allow requests */
        HALF_OPEN(2, true);

        private final int order;
        public final boolean allowPublish;

        /**
         * Order is a FIXED integer, it should be preserved regardless of the ordinal number of the enumeration.
         * While a State.ordinal() does mostly the same, it is prone to changing the order based on how the
         * programmer  sets the enum. If more states are added the "order" should be preserved. For example, if
         * there is a state inserted between CLOSED and HALF_OPEN (say FIXED_OPEN) then the order of HALF_OPEN remains
         * at 2 and the new state takes 3 regardless of its order in the enum.
         *
         * @param order
         * @param allowPublish
         */
        private State(int order, boolean allowPublish){
            this.order = order;
            this.allowPublish = allowPublish;
        }

        public int getOrder(){
            return order;
        }
    }

    /**
     * State transitions of the CircuitBreaker state machine.
     */
    enum StateTransition {
        CLOSED_TO_OPEN(State.CLOSED, State.OPEN),
        CLOSED_TO_DISABLED(State.CLOSED, State.DISABLED),
        CLOSED_TO_FORCED_OPEN(State.CLOSED, State.FORCED_OPEN),
        HALF_OPEN_TO_CLOSED(State.HALF_OPEN, State.CLOSED),
        HALF_OPEN_TO_OPEN(State.HALF_OPEN, State.OPEN),
        HALF_OPEN_TO_DISABLED(State.HALF_OPEN, State.DISABLED),
        HALF_OPEN_TO_FORCED_OPEN(State.HALF_OPEN, State.FORCED_OPEN),
        OPEN_TO_CLOSED(State.OPEN, State.CLOSED),
        OPEN_TO_HALF_OPEN(State.OPEN, State.HALF_OPEN),
        OPEN_TO_DISABLED(State.OPEN, State.DISABLED),
        OPEN_TO_FORCED_OPEN(State.OPEN, State.FORCED_OPEN),
        FORCED_OPEN_TO_CLOSED(State.FORCED_OPEN, State.CLOSED),
        FORCED_OPEN_TO_OPEN(State.FORCED_OPEN, State.OPEN),
        FORCED_OPEN_TO_DISABLED(State.FORCED_OPEN, State.DISABLED),
        FORCED_OPEN_TO_HALF_OPEN(State.FORCED_OPEN, State.HALF_OPEN),
        DISABLED_TO_CLOSED(State.DISABLED, State.CLOSED),
        DISABLED_TO_OPEN(State.DISABLED, State.OPEN),
        DISABLED_TO_FORCED_OPEN(State.DISABLED, State.FORCED_OPEN),
        DISABLED_TO_HALF_OPEN(State.DISABLED, State.HALF_OPEN);

        private final State fromState;

        private final State toState;

        private static final Map<Tuple2<State, State>, StateTransition> STATE_TRANSITION_MAP =
                Arrays
                        .stream(StateTransition.values())
                        .collect(Collectors.toMap(v -> Tuple.of(v.fromState, v.toState), Function.identity()));

        private boolean matches(State fromState, State toState) {
            return this.fromState == fromState && this.toState == toState;
        }

        public static StateTransition transitionBetween(State fromState, State toState){
            final StateTransition stateTransition = STATE_TRANSITION_MAP.get(Tuple.of(fromState, toState));
            if(stateTransition == null) {
                throw new IllegalStateException(
                        String.format("Illegal state transition from %s to %s", fromState.toString(), toState.toString()));
            }
            return stateTransition;
        }

        StateTransition(State fromState, State toState) {
            this.fromState = fromState;
            this.toState = toState;
        }

        public State getFromState() {
            return fromState;
        }

        public State getToState() {
            return toState;
        }

        @Override
        public String toString(){
            return String.format("State transition from %s to %s", fromState, toState);
        }
    }

    /**
     * An EventPublisher can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<CircuitBreakerEvent> {

        EventPublisher onSuccess(EventConsumer<CircuitBreakerOnSuccessEvent> eventConsumer);

        EventPublisher onError(EventConsumer<CircuitBreakerOnErrorEvent> eventConsumer);

        EventPublisher onStateTransition(EventConsumer<CircuitBreakerOnStateTransitionEvent> eventConsumer);

        EventPublisher onReset(EventConsumer<CircuitBreakerOnResetEvent> eventConsumer);

        EventPublisher onIgnoredError(EventConsumer<CircuitBreakerOnIgnoredErrorEvent> eventConsumer);

        EventPublisher onCallNotPermitted(EventConsumer<CircuitBreakerOnCallNotPermittedEvent> eventConsumer);
        }

    interface Metrics {

        /**
         * Returns the failure rate in percentage. If the number of measured calls is below the minimum number of measured calls,
         * it returns -1.
         *
         * @return the failure rate in percentage
         */
        float getFailureRate();

        /**
         * Returns the current number of buffered calls.
         *
         * @return he current number of buffered calls
         */
        int getNumberOfBufferedCalls();

        /**
         * Returns the current number of failed calls.
         *
         * @return the current number of failed calls
         */
        int getNumberOfFailedCalls();

        /**
         * Returns the current number of not permitted calls, when the state is OPEN.
         *
         * The number of denied calls is always 0, when the CircuitBreaker state is CLOSED or HALF_OPEN.
         * The number of denied calls is only increased when the CircuitBreaker state is OPEN.
         *
         * @return the current number of not permitted calls
         */
        long getNumberOfNotPermittedCalls();

        /**
         * Returns the maximum number of buffered calls.
         *
         * @return the maximum number of buffered calls
         */
        int getMaxNumberOfBufferedCalls();

        /**
         * Returns the current number of successful calls.
         *
         * @return the current number of successful calls
         */
        int getNumberOfSuccessfulCalls();
    }

    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier the original supplier
     * @param <T> the type of results supplied by this supplier
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(CircuitBreaker circuitBreaker, CheckedFunction0<T> supplier){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try {
                T returnValue = supplier.apply();

                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
                return returnValue;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier the original supplier
     * @param <T> the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(
        CircuitBreaker circuitBreaker,
        Supplier<CompletionStage<T>> supplier
    ) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();

            if (!circuitBreaker.isCallPermitted()) {
                promise.completeExceptionally(
                        new CircuitBreakerOpenException(
                                String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));

            } else {
                final long start = System.nanoTime();
                supplier.get().whenComplete((result, throwable) -> {
                    long durationInNanos = System.nanoTime() - start;
                    if (result != null) {
                        circuitBreaker.onSuccess(durationInNanos);
                        promise.complete(result);
                    } else if (throwable instanceof Exception) {
                        circuitBreaker.onError(durationInNanos, throwable);
                        promise.completeExceptionally(throwable);
                    } else{
                        // Do not handle java.lang.Error
                        promise.completeExceptionally(throwable);
                    }
                });
            }

            return promise;
        };
    }

    /**
     * Returns a runnable which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param runnable the original runnable
     *
     * @return a runnable which is decorated by a CircuitBreaker.
     */
    static CheckedRunnable decorateCheckedRunnable(CircuitBreaker circuitBreaker, CheckedRunnable runnable){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try{
                runnable.run();
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
            } catch (Exception exception){
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a callable which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param callable the original Callable
     * @param <T> the result type of callable
     *
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Callable<T> decorateCallable(CircuitBreaker circuitBreaker, Callable<T> callable){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try {
                T returnValue = callable.call();
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
                return returnValue;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier the original supplier
     * @param <T> the type of results supplied by this supplier
     *
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Supplier<T> decorateSupplier(CircuitBreaker circuitBreaker, Supplier<T> supplier){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try {
                T returnValue = supplier.get();
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
                return returnValue;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a CircuitBreaker.

     * @param circuitBreaker the CircuitBreaker
     * @param consumer the original consumer
     * @param <T> the type of the input to the consumer
     *
     * @return a consumer which is decorated by a CircuitBreaker.
     */
    static <T> Consumer<T> decorateConsumer(CircuitBreaker circuitBreaker, Consumer<T> consumer){
        return (t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try {
                consumer.accept(t);
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a CircuitBreaker.

     * @param circuitBreaker the CircuitBreaker
     * @param consumer the original consumer
     * @param <T> the type of the input to the consumer
     *
     * @return a consumer which is decorated by a CircuitBreaker.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(CircuitBreaker circuitBreaker, CheckedConsumer<T> consumer){
        return (t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try {
                consumer.accept(t);
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param runnable the original runnable
     *
     * @return a runnable which is decorated by a CircuitBreaker.
     */
    static Runnable decorateRunnable(CircuitBreaker circuitBreaker, Runnable runnable){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try{
                runnable.run();
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
            } catch (Exception exception){
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a function which is decorated by a CircuitBreaker.

     * @param circuitBreaker the CircuitBreaker
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return a function which is decorated by a CircuitBreaker.
     */
    static <T, R> Function<T, R> decorateFunction(CircuitBreaker circuitBreaker, Function<T, R> function){
        return (T t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try{
                R returnValue = function.apply(t);
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
                return returnValue;
            } catch (Exception exception){
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a function which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return a function which is decorated by a CircuitBreaker.
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(CircuitBreaker circuitBreaker, CheckedFunction1<T, R> function){
        return (T t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            long start = System.nanoTime();
            try{
                R returnValue = function.apply(t);
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos);
                return returnValue;
            } catch (Exception exception){
                // Do not handle java.lang.Error
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, exception);
                throw exception;
            }
        };
    }

    /**
     * Creates a CircuitBreaker with a default CircuitBreaker configuration.
     *
     * @param name the name of the CircuitBreaker
     *
     * @return a CircuitBreaker with a default CircuitBreaker configuration.
     */
    static CircuitBreaker ofDefaults(String name){
        return new CircuitBreakerStateMachine(name);
    }

    /**
     * Creates a CircuitBreaker with a custom CircuitBreaker configuration.
     *
     * @param name the name of the CircuitBreaker
     * @param circuitBreakerConfig a custom CircuitBreaker configuration
     *
     * @return a CircuitBreaker with a custom CircuitBreaker configuration.
     */
    static CircuitBreaker of(String name, CircuitBreakerConfig circuitBreakerConfig){
        return new CircuitBreakerStateMachine(name, circuitBreakerConfig);
    }

    /**
     * Creates a CircuitBreaker with a custom CircuitBreaker configuration.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfigSupplier a supplier of a custom CircuitBreaker configuration
     *
     * @return a CircuitBreaker with a custom CircuitBreaker configuration.
     */
    static CircuitBreaker of(String name, Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier){
        return new CircuitBreakerStateMachine(name, circuitBreakerConfigSupplier);
    }
}
