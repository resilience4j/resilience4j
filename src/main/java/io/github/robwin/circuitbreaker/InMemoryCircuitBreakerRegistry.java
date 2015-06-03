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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Backend circuitBreaker manager.
 * <p/>
 * Constructs backend monitors according to configuration values.
 */
public class InMemoryCircuitBreakerRegistry implements CircuitBreakerRegistry {

    private final CircuitBreakerConfig defaultCircuitBreakerConfig;

    /**
     * The monitors, indexed by name of the backend.
     */
    private final ConcurrentMap<String, CircuitBreaker> monitors;

    /**
     * The constructor with default circuitBreaker properties.
     */
    public InMemoryCircuitBreakerRegistry() {
        this.defaultCircuitBreakerConfig = new CircuitBreakerConfig.Builder().build();
        this.monitors = new ConcurrentHashMap<>();
    }

    /**
     * The constructor with custom default circuitBreaker properties.
     *
     * @param defaultCircuitBreakerConfig The BackendMonitor service properties.
     */
    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultCircuitBreakerConfig) {
        this.defaultCircuitBreakerConfig = Objects.requireNonNull(defaultCircuitBreakerConfig, "CircuitBreakerConfig must not be null");
        this.monitors = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name) {
        return monitors.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> new DefaultCircuitBreaker(name,
                defaultCircuitBreakerConfig));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig customCircuitBreakerConfig) {
        return monitors.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> new DefaultCircuitBreaker(name,
                customCircuitBreakerConfig));
    }

    /**
     * Resets the circuitBreaker states.
     */
    public void resetMonitorStates() {
        monitors.values().forEach(CircuitBreaker::recordSuccess);
    }
}
