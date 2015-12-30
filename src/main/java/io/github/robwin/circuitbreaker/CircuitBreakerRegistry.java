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


import io.github.robwin.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;

/**
 * Manages all circuitBreaker instances.
 */
public interface CircuitBreakerRegistry {

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with the default configuration.
     *
     * @param name the name of the CircuitBreaker
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with a custom configuration.
     *
     * @param name      the name of the CircuitBreaker
     * @param circuitBreakerConfig  the CircuitBreaker configuration
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig circuitBreakerConfig);

    static CircuitBreakerRegistry of(CircuitBreakerConfig defaultCircuitBreakerConfig){
        return new InMemoryCircuitBreakerRegistry(defaultCircuitBreakerConfig);
    }

    static CircuitBreakerRegistry ofDefaults(){
        return new InMemoryCircuitBreakerRegistry();
    }
}
