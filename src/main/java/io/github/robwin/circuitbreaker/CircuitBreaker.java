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

import javaslang.control.Try;

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
     * Records a backend failure.
     * This must be called if a call to a backend fails
     *
     * @param throwable The throwable which must be recorded
     */
    void recordFailure(Throwable throwable);

     /**
      * Records success of a call to a backend.
      * This must be called after a successful call.
      */
    void recordSuccess();

    /**
     * Get the name of the CircuitBreaker
     *
     * @return the name of the CircuitBreaker
     */
    String getName();

    /**
     * Get the state of the CircuitBreaker
     *
     * @return the state of the CircuitBreaker
     */
    State getState();

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
        /** A HALF_CLOSED breaker has completed its wait interval
         and will allow requests */
        HALF_CLOSED
    }

    /**
     * State transitions of the CircuitBreaker state machine.
     */
    enum StateTransition {
        CLOSED_TO_OPEN(State.CLOSED, State.OPEN),
        HALF_CLOSED_TO_CLOSED(State.HALF_CLOSED, State.CLOSED),
        HALF_CLOSED_TO_OPEN(State.HALF_CLOSED, State.OPEN),
        OPEN_TO_CLOSED(State.OPEN, State.CLOSED),
        OPEN_TO_HALF_CLOSED(State.OPEN, State.HALF_CLOSED);

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

    /**
     * Creates a supplier which is secured by a CircuitBreaker.
     *
     * @param supplier the original supplier
     * @param circuitBreaker the CircuitBreaker
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Try.CheckedSupplier<T> supplier, CircuitBreaker circuitBreaker){
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
     * @return a runnable which is secured by a CircuitBreaker.
     */
    static Try.CheckedRunnable decorateCheckedRunnable(Try.CheckedRunnable runnable, CircuitBreaker circuitBreaker){
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
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <T> Supplier<T> decorateSupplier(Supplier<T> supplier, CircuitBreaker circuitBreaker){
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
     * @return a runnable which is secured by a CircuitBreaker.
     */
    static Runnable decorateRunnable(Runnable runnable, CircuitBreaker circuitBreaker){
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
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T, R> Function<T, R> decorateFunction(Function<T, R> function, CircuitBreaker circuitBreaker){
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
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Try.CheckedFunction<T, R> function, CircuitBreaker circuitBreaker){
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
