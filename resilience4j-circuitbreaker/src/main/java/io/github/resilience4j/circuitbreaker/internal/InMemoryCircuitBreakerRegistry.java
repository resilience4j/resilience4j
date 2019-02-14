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
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
     * The list of consumer functions to execute after a circuit breaker is created.
     */
    private final List<BiConsumer<CircuitBreaker, CircuitBreakerConfig>> postCreationConsumers;

    /**
     * The constructor with default circuitBreaker properties.
     */
    public InMemoryCircuitBreakerRegistry() {
        this.defaultCircuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        this.circuitBreakers = new ConcurrentHashMap<>();
        this.postCreationConsumers = new ArrayList<>();
    }

    /**
     * The constructor with custom default circuitBreaker properties.
     *
     * @param defaultCircuitBreakerConfig The BackendMonitor service properties.
     */
    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultCircuitBreakerConfig) {
        this.defaultCircuitBreakerConfig = Objects.requireNonNull(defaultCircuitBreakerConfig, "CircuitBreakerConfig must not be null");
        this.circuitBreakers = new ConcurrentHashMap<>();
        this.postCreationConsumers = new ArrayList<>();
    }

    @Override
    public Seq<CircuitBreaker> getAllCircuitBreakers() {
        return Array.ofAll(circuitBreakers.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), 
        		(k) -> postCreateCircuitBreaker(CircuitBreaker.of(name, defaultCircuitBreakerConfig), defaultCircuitBreakerConfig));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig customCircuitBreakerConfig) {
        return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), 
        		(k) ->  postCreateCircuitBreaker(CircuitBreaker.of(name, customCircuitBreakerConfig), customCircuitBreakerConfig));
    }

    @Override
    public CircuitBreaker circuitBreaker(String name, Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
        return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), 
        		(k) -> {
        			CircuitBreakerConfig config = circuitBreakerConfigSupplier.get();
        			return postCreateCircuitBreaker(CircuitBreaker.of(name, config), config);
        		});
    }
    
    private CircuitBreaker postCreateCircuitBreaker(CircuitBreaker createdCircuitBreaker, CircuitBreakerConfig circuitBreakerConfig) {
        if(postCreationConsumers != null) {
            postCreationConsumers.forEach(consumer -> {
                consumer.accept(createdCircuitBreaker, circuitBreakerConfig);
            });
        }
        return createdCircuitBreaker;
    }

    @Override
    public void registerPostCreationConsumer(BiConsumer<CircuitBreaker, CircuitBreakerConfig> postCreationConsumer) {
        postCreationConsumers.add(postCreationConsumer);
    }
}
