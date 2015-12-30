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
package io.github.robwin.circuitbreaker.internal;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Backend circuitBreaker manager.
 * Constructs backend circuitBreakers according to configuration values.
 */
public final class InMemoryCircuitBreakerRegistry implements CircuitBreakerRegistry {

    private final CircuitBreakerConfig defaultCircuitBreakerConfig;

    /**
     * The circuitBreakers, indexed by name of the backend.
     */
    private final ConcurrentMap<String, CircuitBreaker> circuitBreakers;

    /**
     * The constructor with default circuitBreaker properties.
     */
    public InMemoryCircuitBreakerRegistry() {
        this.defaultCircuitBreakerConfig = new CircuitBreakerConfig.Builder().build();
        this.circuitBreakers = new ConcurrentHashMap<>();
    }

    /**
     * The constructor with custom default circuitBreaker properties.
     *
     * @param defaultCircuitBreakerConfig The BackendMonitor service properties.
     */
    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultCircuitBreakerConfig) {
        this.defaultCircuitBreakerConfig = Objects.requireNonNull(defaultCircuitBreakerConfig, "CircuitBreakerConfig must not be null");
        this.circuitBreakers = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> new CircuitBreakerStateMachine(name,
                defaultCircuitBreakerConfig));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig customCircuitBreakerConfig) {
        return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> new CircuitBreakerStateMachine(name,
                customCircuitBreakerConfig));
    }

    /**
     * Reset the circuitBreaker states.
     */
    public void resetMonitorStates() {
        circuitBreakers.values().forEach(CircuitBreaker::recordSuccess);
    }
}
