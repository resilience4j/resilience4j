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
package io.github.robwin.circuitbreaker;

import io.github.robwin.circuitbreaker.event.CircuitBreakerEvent;
import io.github.robwin.circuitbreaker.internal.*;
import io.github.robwin.circuitbreaker.utils.CircuitBreakerUtils;
import io.github.robwin.metrics.StopWatch;
import io.reactivex.Flowable;
import javaslang.control.Try;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link CircuitBreaker} manages the state of a backend system.
 * The CircuitBreaker is implemented via a finite state machine with three states: CLOSED, OPEN and HALF_OPEN.
 * The CircuitBreaker does not know anything about the backend’s state by itself, but uses the information provided by the decorators via
 * {@link CircuitBreaker#onSuccess} and {@link CircuitBreaker#onError} events.
 * Before communicating with the backend, the the permission to do so must be obtained via the method {@link CircuitBreaker#isCallPermitted()}.
 *
 * The state of the CircuitBreaker changes from CLOSED to OPEN when the failure rate is above a (configurable) threshold.
 * Then, all access to the backend is blocked for a (configurable) time duration. {@link CircuitBreaker#isCallPermitted} throws a {@link CircuitBreakerOpenException}, if the CircuitBreaker is OPEN.
 *
 * After the time duration has elapsed, the CircuitBreaker state changes from OPEN to HALF_OPEN and allows calls to see if the backend is still unavailable or has become available again.
 * If the failure rate is above the configured threshold, the state changes back to OPEN. If the failure rate is below or equal to the threshold, the state changes back to CLOSED.
 */
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
     * @param duration The elapsed time duration of the call
     * @param throwable The throwable which must be recorded
     */
    void onError(Duration duration, Throwable throwable);

     /**
      * Records a successful call.
      *
      * @param duration The elapsed time duration of the call
      * This method must be invoked when a call is successfully.
      */
    void onSuccess(Duration duration);


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
     * Returns a reactive stream of CircuitBreakerEvents.
     *
     * @return a reactive stream of CircuitBreakerEvents
     */
    Flowable<CircuitBreakerEvent> getEventStream();

    /**
     * States of the CircuitBreaker state machine.
     */
    enum State {
        /** A CLOSED breaker is operating normally and allowing
         requests through. */
        CLOSED,
        /** An OPEN breaker has tripped and will not allow requests
         through. */
        OPEN,
        /** A HALF_OPEN breaker has completed its wait interval
         and will allow requests */
        HALF_OPEN
    }

    /**
     * State transitions of the CircuitBreaker state machine.
     */
    enum StateTransition {
        CLOSED_TO_OPEN(State.CLOSED, State.OPEN),
        HALF_OPEN_TO_CLOSED(State.HALF_OPEN, State.CLOSED),
        HALF_OPEN_TO_OPEN(State.HALF_OPEN, State.OPEN),
        OPEN_TO_HALF_OPEN(State.OPEN, State.HALF_OPEN),
        FORCED_OPEN_TO_CLOSED(State.OPEN, State.CLOSED);

        State fromState;
        State toState;

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

        public static StateTransition transitionToClosedState(State fromState){
            switch (fromState) {
                case HALF_OPEN:
                    return HALF_OPEN_TO_CLOSED;
                case OPEN:
                    return FORCED_OPEN_TO_CLOSED;
                default:
                    throw new IllegalStateException(String.format("Illegal state transition from %s to %s", fromState.toString(), State.CLOSED.toString()));
            }
        }

        public static StateTransition transitionToOpenState(State fromState){
            switch (fromState) {
                case HALF_OPEN:
                    return HALF_OPEN_TO_OPEN;
                case CLOSED:
                    return CLOSED_TO_OPEN;
                default:
                    throw new IllegalStateException(String.format("Illegal state transition from %s to %s", fromState.toString(), State.OPEN.toString()));
            }
        }

        public static StateTransition transitionToHalfOpenState(State fromState){
            switch (fromState) {
                case OPEN:
                    return OPEN_TO_HALF_OPEN;
                default:
                    throw new IllegalStateException(String.format("Illegal state transition from %s to %s", fromState.toString(), State.HALF_OPEN.toString()));
            }
        }

        @Override
        public String toString(){
            return String.format("State transition from %s to %s", fromState, toState);
        }
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
         * Returns the current number of denied calls.
         * The number of denied calls is always 0, when the CircuitBreaker state is CLOSED or HALF_OPEN.
         * The number of denied calls is only increased when the CircuitBreaker state is OPEN.
         *
         * @return the current number of denied calls
         */
        long getNumberOfNotPermittedCalls();

        /**
         * Returns the maximum number of buffered calls.
         *
         * @return the maximum number of buffered calls
         */
        int getMaxNumberOfBufferedCalls();

        /**
         * Returns the maximum number of successful calls.
         *
         * @return the maximum number of successful calls
         */
        int getNumberOfSuccessfulCalls();
    }

    /**
     * Creates a supplier which is secured by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier the original supplier
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(CircuitBreaker circuitBreaker, Try.CheckedSupplier<T> supplier){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try {
                T returnValue = supplier.get();
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                return returnValue;
            } catch (Throwable throwable) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a runnable which is secured by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param runnable the original runnable

     * @return a runnable which is secured by a CircuitBreaker.
     */
    static Try.CheckedRunnable decorateCheckedRunnable(CircuitBreaker circuitBreaker, Try.CheckedRunnable runnable){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try{
                runnable.run();
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
            } catch (Throwable throwable){
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a callable which is secured by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param callable the original Callable
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <T> Callable<T> decorateCallable(CircuitBreaker circuitBreaker, Callable<T> callable){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try {
                T returnValue = callable.call();
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                return returnValue;
            } catch (Throwable throwable) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a supplier which is secured by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier the original supplier
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <T> Supplier<T> decorateSupplier(CircuitBreaker circuitBreaker, Supplier<T> supplier){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try {
                T returnValue = supplier.get();
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                return returnValue;
            } catch (Throwable throwable) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a consumer which is secured by a CircuitBreaker.

     * @param circuitBreaker the CircuitBreaker
     * @param consumer the original consumer
     *
     * @return a consumer which is secured by a CircuitBreaker.
     */
    static <T> Consumer<T> decorateConsumer(CircuitBreaker circuitBreaker, Consumer<T> consumer){
        return (t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try {
                consumer.accept(t);
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
            } catch (Throwable throwable) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a consumer which is secured by a CircuitBreaker.

     * @param circuitBreaker the CircuitBreaker
     * @param consumer the original consumer
     *
     * @return a consumer which is secured by a CircuitBreaker.
     */
    static <T> Try.CheckedConsumer<T> decorateCheckedConsumer(CircuitBreaker circuitBreaker, Try.CheckedConsumer<T> consumer){
        return (t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try {
                consumer.accept(t);
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
            } catch (Throwable throwable) {
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a runnable which is secured by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param runnable the original runnable
     *
     * @return a runnable which is secured by a CircuitBreaker.
     */
    static Runnable decorateRunnable(CircuitBreaker circuitBreaker, Runnable runnable){
        return () -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try{
                runnable.run();
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
            } catch (Throwable throwable){
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a function which is secured by a CircuitBreaker.

     * @param circuitBreaker the CircuitBreaker
     * @param function the original function
     *
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T, R> Function<T, R> decorateFunction(CircuitBreaker circuitBreaker, Function<T, R> function){
        return (T t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try{
                R returnValue = function.apply(t);
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                return returnValue;
            } catch (Throwable throwable){
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a function which is secured by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param function the original function
     *
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(CircuitBreaker circuitBreaker, Try.CheckedFunction<T, R> function){
        return (T t) -> {
            CircuitBreakerUtils.isCallPermitted(circuitBreaker);
            StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
            try{
                R returnValue = function.apply(t);
                circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                return returnValue;
            } catch (Throwable throwable){
                circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                throw throwable;
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
