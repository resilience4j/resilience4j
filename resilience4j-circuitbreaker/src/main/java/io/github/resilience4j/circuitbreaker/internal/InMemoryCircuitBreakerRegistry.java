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
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * Backend circuitBreaker manager. Constructs backend circuitBreakers according to configuration
 * values.
 */
public final class InMemoryCircuitBreakerRegistry extends
    AbstractRegistry<CircuitBreaker, CircuitBreakerConfig> implements CircuitBreakerRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryCircuitBreakerRegistry() {
        this(emptyMap());
    }

    public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs) {
        this(configs, emptyMap());
    }

    public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
        Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()), tags);
        this.configurations.putAll(configs);
    }

    public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
        this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
            registryEventConsumer);
        this.configurations.putAll(configs);
    }

    public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
            registryEventConsumer, tags);
        this.configurations.putAll(configs);
    }

    public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
                                          List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers,
                                          Map<String, String> tags, RegistryStore<CircuitBreaker> registryStore) {
        super(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(emptyMap()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        this.configurations.putAll(configs);
    }

    public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs,
        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
        this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()),
            registryEventConsumers);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig) {
        super(defaultConfig);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     * @param tags          The tags to add to the CircuitBreaker
     */
    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
        Map<String, String> tags) {
        super(defaultConfig, tags);
    }

    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer,
        Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<CircuitBreaker> getAllCircuitBreakers() {
        return new HashSet<>(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name) {
        return circuitBreaker(name, getDefaultConfig());
    }

    @Override
    public CircuitBreaker circuitBreaker(String name, Map<String, String> tags) {
        return circuitBreaker(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config) {
        return circuitBreaker(name, config, emptyMap());
    }

    @Override
    public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config,
        Map<String, String> tags) {
        return computeIfAbsent(name, () -> CircuitBreaker
            .of(name, Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name, String configName) {
        return circuitBreaker(name, configName, emptyMap());
    }

    @Override
    public CircuitBreaker circuitBreaker(String name, String configName, Map<String, String> tags) {
        return computeIfAbsent(name, () -> CircuitBreaker.of(name, getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName)), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircuitBreaker circuitBreaker(String name,
        Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
        return circuitBreaker(name, circuitBreakerConfigSupplier, emptyMap());
    }

    @Override
    public CircuitBreaker circuitBreaker(String name,
        Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier, Map<String, String> tags) {
        return computeIfAbsent(name, () -> CircuitBreaker.of(name, Objects.requireNonNull(
            Objects.requireNonNull(circuitBreakerConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }
}
