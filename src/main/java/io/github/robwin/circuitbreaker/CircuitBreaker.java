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

import java.util.function.Supplier;

import static io.github.robwin.circuitbreaker.CircuitBreakerUtils.checkIfCallIsPermitted;

/**
 * CircuitBreaker API.
 *
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
    abstract boolean isCallPermitted();

    /**
     * Records a backend failure.
     * This must be called if a call to this backend fails
     */
    void recordFailure();

     /**
      * Records success of a call to this backend.
      * This must be called after a successful call.
      */
    void recordSuccess();

    /**
     * Get the name of the CircuitBreaker
     */
    String getName();

    /**
     * Get the state of the CircuitBreaker
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
        /** A HALF_CLOSED breaker has completed its cooldown
         period and will allow one request */
        HALF_CLOSED
    }

    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Try.CheckedSupplier<T> supplier, CircuitBreaker circuitBreaker){
        return () -> {
            try {
                checkIfCallIsPermitted(circuitBreaker);
                T returnValue = supplier.get();
                circuitBreaker.recordSuccess();
                return returnValue;
            } catch (Throwable throwable) {
                circuitBreaker.recordFailure();
                throw throwable;
            }
        };
    }

    static Try.CheckedRunnable decorateCheckedRunnable(Try.CheckedRunnable runnable, CircuitBreaker circuitBreaker){
        return () -> {
            try{
                checkIfCallIsPermitted(circuitBreaker);
                runnable.run();
                circuitBreaker.recordSuccess();
            } catch (Throwable throwable){
                circuitBreaker.recordFailure();
                throw throwable;
            }
        };
    }

    static <T> Supplier<T> decorateSupplier(Supplier<T> supplier, CircuitBreaker circuitBreaker){
        return () -> {
            try {
                checkIfCallIsPermitted(circuitBreaker);
                T returnValue = supplier.get();
                circuitBreaker.recordSuccess();
                return returnValue;
            } catch (Throwable throwable) {
                circuitBreaker.recordFailure();
                throw throwable;
            }
        };
    }

    static Runnable decorateRunnable(Runnable runnable, CircuitBreaker circuitBreaker){
        return () -> {
            try{
                checkIfCallIsPermitted(circuitBreaker);
                runnable.run();
                circuitBreaker.recordSuccess();
            } catch (Throwable throwable){
                circuitBreaker.recordFailure();
                throw throwable;
            }
        };
    }
}
