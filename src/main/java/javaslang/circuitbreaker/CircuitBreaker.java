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
package javaslang.circuitbreaker;

import io.reactivex.Observable;
import javaslang.control.Try;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A CircuitBreaker manages the state of a backend system. It is notified on the result of all
 * attempts to communicate with the backend, via the {@link #recordSuccess} and {@link #recordFailure} methods.
 * Before communicating with the backend, the respective connector must obtain the permission to do so via the method
 * {@link #isCallPermitted()}.
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
     * This method must be invoked after a failed call.
     *
     * @param throwable The throwable which must be recorded
     */
    void recordFailure(Throwable throwable);

     /**
      * Records a successful call.
      * This method must be invoked after a successful call.
      */
    void recordSuccess();

    /**
     * Get the name of this CircuitBreaker
     *
     * @return the name of this CircuitBreaker
     */
    String getName();

    /**
     * Get the state of this CircuitBreaker
     *
     * @return the state of this CircuitBreaker
     */
    State getState();

    /**
     * Get the CircuitBreakerConfig of this CircuitBreaker.
     *
     * @return the CircuitBreakerConfig of this CircuitBreaker
     */
    CircuitBreakerConfig getCircuitBreakerConfig();

    /**
     * Get the Metrics of this CircuitBreaker.
     *
     * @return the Metrics of this CircuitBreaker
     */
    Metrics getMetrics();


    /**
     * Get an Observable of CircuitBreakerEvents which can be subscribed
     *
     * @return an Observable of CircuitBreakerEvents which can be subscribed
     */
    Observable<CircuitBreakerEvent> observeCircuitBreakerEvents();

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
        OPEN_TO_HALF_OPEN(State.OPEN, State.HALF_OPEN);

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
         * @return the current number of failed calls.
         */
        int getNumberOfFailedCalls();
    }

    /**
     * Creates a supplier which is secured by a CircuitBreaker.
     *
     * @param supplier the original supplier
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    @Deprecated
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Try.CheckedSupplier<T> supplier, CircuitBreaker circuitBreaker){
        return decorateCheckedSupplier(circuitBreaker, supplier);
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
            try {
                T returnValue = supplier.get();
                circuitBreaker.recordSuccess();
                return returnValue;
            } catch (Throwable throwable) {
                circuitBreaker.recordFailure(throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a runnable which is secured by a CircuitBreaker.
     *
     * @param runnable the original runnable
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a runnable which is secured by a CircuitBreaker.
     */
    @Deprecated
    static Try.CheckedRunnable decorateCheckedRunnable(Try.CheckedRunnable runnable, CircuitBreaker circuitBreaker){
        return decorateCheckedRunnable(circuitBreaker, runnable);
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
            try{
                runnable.run();
                circuitBreaker.recordSuccess();
            } catch (Throwable throwable){
                circuitBreaker.recordFailure(throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a supplier which is secured by a CircuitBreaker.
     *
     * @param supplier the original supplier
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    @Deprecated
    static <T> Supplier<T> decorateSupplier(Supplier<T> supplier, CircuitBreaker circuitBreaker){
        return decorateSupplier(circuitBreaker, supplier);
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
            try {
                T returnValue = supplier.get();
                circuitBreaker.recordSuccess();
                return returnValue;
            } catch (Throwable throwable) {
                circuitBreaker.recordFailure(throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a consumer which is secured by a CircuitBreaker.
     *
     * @param consumer the original consumer
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a consumer which is secured by a CircuitBreaker.
     */
    @Deprecated
    static <T> Consumer<T> decorateConsumer(Consumer<T> consumer, CircuitBreaker circuitBreaker){
        return decorateConsumer(circuitBreaker, consumer);
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
            try {
                consumer.accept(t);
                circuitBreaker.recordSuccess();
            } catch (Throwable throwable) {
                circuitBreaker.recordFailure(throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a runnable which is secured by a CircuitBreaker.
     *
     * @param runnable the original runnable
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a runnable which is secured by a CircuitBreaker.
     */
    @Deprecated
    static Runnable decorateRunnable(Runnable runnable, CircuitBreaker circuitBreaker){
        return decorateRunnable(circuitBreaker, runnable);
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
            try{
                runnable.run();
                circuitBreaker.recordSuccess();
            } catch (Throwable throwable){
                circuitBreaker.recordFailure(throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a function which is secured by a CircuitBreaker.
     *
     * @param function the original function
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a function which is secured by a CircuitBreaker.
     */
    @Deprecated
    static <T, R> Function<T, R> decorateFunction(Function<T, R> function, CircuitBreaker circuitBreaker){
        return decorateFunction(circuitBreaker, function);
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
            try{
                R returnValue = function.apply(t);
                circuitBreaker.recordSuccess();
                return returnValue;
            } catch (Throwable throwable){
                circuitBreaker.recordFailure(throwable);
                throw throwable;
            }
        };
    }

    /**
     * Creates a function which is secured by a CircuitBreaker.
     *
     * @param function the original function
     * @param circuitBreaker the CircuitBreaker
     *
     * @return a function which is secured by a CircuitBreaker.
     */
    @Deprecated
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Try.CheckedFunction<T, R> function, CircuitBreaker circuitBreaker){
        return decorateCheckedFunction(circuitBreaker, function);
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
            try{
                R returnValue = function.apply(t);
                circuitBreaker.recordSuccess();
                return returnValue;
            } catch (Throwable throwable){
                circuitBreaker.recordFailure(throwable);
                throw throwable;
            }
        };
    }
}
