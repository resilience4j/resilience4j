/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Bulkhead instance manager; Constructs/returns bulkhead instances.
 */
public final class InMemoryBulkheadRegistry extends
    AbstractRegistry<Bulkhead, BulkheadConfig> implements BulkheadRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryBulkheadRegistry() {
        this(BulkheadConfig.ofDefaults());
    }

    public InMemoryBulkheadRegistry(io.vavr.collection.Map<String, String> tags) {
        this(BulkheadConfig.ofDefaults(), tags);
    }

    public InMemoryBulkheadRegistry(Map<String, BulkheadConfig> configs) {
        this(configs, HashMap.empty());
    }

    public InMemoryBulkheadRegistry(Map<String, BulkheadConfig> configs,
        io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, BulkheadConfig.ofDefaults()), tags);
        this.configurations.putAll(configs);
    }

    public InMemoryBulkheadRegistry(
        Map<String, BulkheadConfig> configs,
        RegistryEventConsumer<Bulkhead> registryEventConsumer) {
        this(configs, registryEventConsumer, HashMap.empty());
    }

    public InMemoryBulkheadRegistry(
        Map<String, BulkheadConfig> configs, RegistryEventConsumer<Bulkhead> registryEventConsumer,
        io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, BulkheadConfig.ofDefaults()),
            registryEventConsumer, tags);
        this.configurations.putAll(configs);
    }

    public InMemoryBulkheadRegistry(
        Map<String, BulkheadConfig> configs,
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers) {
        this(configs, registryEventConsumers, HashMap.empty());
    }

    public InMemoryBulkheadRegistry(
        Map<String, BulkheadConfig> configs,
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers,
        io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, BulkheadConfig.ofDefaults()),
            registryEventConsumers, tags);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public InMemoryBulkheadRegistry(BulkheadConfig defaultConfig) {
        super(defaultConfig);
    }

    public InMemoryBulkheadRegistry(BulkheadConfig defaultConfig,
        io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, tags);
    }

    public InMemoryBulkheadRegistry(BulkheadConfig defaultConfig,
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    public InMemoryBulkheadRegistry(BulkheadConfig defaultConfig,
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers,
        io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    public InMemoryBulkheadRegistry(BulkheadConfig defaultConfig,
        RegistryEventConsumer<Bulkhead> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    public InMemoryBulkheadRegistry(BulkheadConfig defaultConfig,
        RegistryEventConsumer<Bulkhead> registryEventConsumer,
        io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    public InMemoryBulkheadRegistry(Map<String, BulkheadConfig> configs,
                                          List<RegistryEventConsumer<Bulkhead>> registryEventConsumers,
                                          io.vavr.collection.Map<String, String> tags, RegistryStore<Bulkhead> registryStore) {
        super(configs.getOrDefault(DEFAULT_CONFIG, BulkheadConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(HashMap.empty()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        this.configurations.putAll(configs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Seq<Bulkhead> getAllBulkheads() {
        return Array.ofAll(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name) {
        return bulkhead(name, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name, io.vavr.collection.Map<String, String> tags) {
        return bulkhead(name, getDefaultConfig(), getAllTags(tags));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name, BulkheadConfig config) {
        return bulkhead(name, config, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name, BulkheadConfig config,
        io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> Bulkhead
            .of(name, Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier) {
        return bulkhead(name, bulkheadConfigSupplier, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier,
        io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> Bulkhead.of(name, Objects.requireNonNull(
            Objects.requireNonNull(bulkheadConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name, String configName) {
        return bulkhead(name, configName, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bulkhead bulkhead(String name, String configName,
        io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> Bulkhead.of(name, getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName)), getAllTags(tags)));
    }
}
