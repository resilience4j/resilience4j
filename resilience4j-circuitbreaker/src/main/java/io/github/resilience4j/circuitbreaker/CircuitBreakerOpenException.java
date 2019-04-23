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
package io.github.resilience4j.circuitbreaker;

/**
 * A {@link CircuitBreakerOpenException} signals that the CircuitBreaker is OPEN.
 * @deprecated use {@link CallNotPermittedException} instead
 */
@Deprecated
public class CircuitBreakerOpenException extends RuntimeException {

    /**
     * The constructor with a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker.
     */
    public CircuitBreakerOpenException(CircuitBreaker circuitBreaker) {
        super(String.format("CircuitBreaker '%s' is %s and does not permit further calls", circuitBreaker.getName(), circuitBreaker.getState()));
    }

    /**
     * The constructor with a message.
     *
     * @param message The message.
     */
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}


