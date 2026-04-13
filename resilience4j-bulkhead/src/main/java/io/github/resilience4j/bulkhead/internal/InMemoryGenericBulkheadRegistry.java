/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech, Mahmoud Romeh
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

import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * Thread pool Bulkhead instance manager; Constructs/returns thread pool bulkhead instances.
 */
public final class InMemoryGenericBulkheadRegistry extends
    AbstractRegistry<GenericBulkhead, GenericBulkheadConfig> implements
    GenericBulkheadRegistry {

    private final Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService;

    /**
     * The constructor with default configuration.
     *
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        this(emptyMap(), scheduledExecutorService);
    }

    /**
     * The constructor with a map of configurations.
     *
     * @param configs a map of configurations
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(Map<String, GenericBulkheadConfig> configs, Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        this(configs, emptyMap(), scheduledExecutorService);
    }

    /**
     * The constructor with a map of configurations and a map of tags.
     *
     * @param configs a map of configurations
     * @param tags a map of tags
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(Map<String, GenericBulkheadConfig> configs, Map<String, String> tags, Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        this(configs.getOrDefault(DEFAULT_CONFIG, GenericBulkheadConfig.ofDefaults()), tags, scheduledExecutorService);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with a map of configurations and a registry event consumer.
     *
     * @param configs a map of configurations
     * @param registryEventConsumer a registry event consumer
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
        Map<String, GenericBulkheadConfig> configs,
        RegistryEventConsumer<GenericBulkhead> registryEventConsumer, Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        this(configs, registryEventConsumer, emptyMap(), scheduledExecutorService);
    }

    /**
     * The constructor with a map of configurations, a registry event consumer, and a map of tags.
     *
     * @param configs a map of configurations
     * @param registryEventConsumer a registry event consumer
     * @param tags a map of tags
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
        Map<String, GenericBulkheadConfig> configs,
        RegistryEventConsumer<GenericBulkhead> registryEventConsumer, Map<String, String> tags,
        Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        this(configs.getOrDefault(DEFAULT_CONFIG, GenericBulkheadConfig.ofDefaults()),
            registryEventConsumer, tags, scheduledExecutorService);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with a map of configurations and a list of registry event consumers.
     *
     * @param configs a map of configurations
     * @param registryEventConsumers a list of registry event consumers
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
        Map<String, GenericBulkheadConfig> configs,
        List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers,
        Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        this(configs, registryEventConsumers, emptyMap(), scheduledExecutorService);
    }

    /**
     * The constructor with a map of configurations, a list of registry event consumers, and a map of tags.
     *
     * @param configs a map of configurations
     * @param registryEventConsumers a list of registry event consumers
     * @param tags a map of tags
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
        Map<String, GenericBulkheadConfig> configs,
        List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers, Map<String, String> tags,
        Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        this(configs.getOrDefault(DEFAULT_CONFIG, GenericBulkheadConfig.ofDefaults()),
            registryEventConsumers, tags, scheduledExecutorService);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(GenericBulkheadConfig defaultConfig, Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        super(defaultConfig);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * The constructor with a default configuration and a map of tags.
     *
     * @param defaultConfig the default configuration
     * @param tags a map of tags
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(GenericBulkheadConfig defaultConfig, Map<String, String> tags, Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        super(defaultConfig, tags);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * The constructor with a default configuration and a registry event consumer.
     *
     * @param defaultConfig the default configuration
     * @param registryEventConsumer a registry event consumer
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
            GenericBulkheadConfig defaultConfig,
        RegistryEventConsumer<GenericBulkhead> registryEventConsumer, Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        super(defaultConfig, registryEventConsumer);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * The constructor with a default configuration, a registry event consumer, and a map of tags.
     *
     * @param defaultConfig the default configuration
     * @param registryEventConsumer a registry event consumer
     * @param tags a map of tags
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
        GenericBulkheadConfig defaultConfig,
        RegistryEventConsumer<GenericBulkhead> registryEventConsumer, Map<String, String> tags,
        Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        super(defaultConfig, registryEventConsumer, tags);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * The constructor with a default configuration and a list of registry event consumers.
     *
     * @param defaultConfig the default configuration
     * @param registryEventConsumers a list of registry event consumers
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
        GenericBulkheadConfig defaultConfig,
        List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers,
        Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        super(defaultConfig, registryEventConsumers);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * The constructor with a default configuration, a list of registry event consumers, and a map of tags.
     *
     * @param defaultConfig the default configuration
     * @param registryEventConsumers a list of registry event consumers
     * @param tags a map of tags
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(
        GenericBulkheadConfig defaultConfig,
        List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers, Map<String, String> tags,
        Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        super(defaultConfig, registryEventConsumers, tags);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * The constructor with a map of configurations, a list of registry event consumers, a map of tags, a registry store, and an executor service function.
     *
     * @param configs a map of configurations
     * @param registryEventConsumers a list of registry event consumers
     * @param tags a map of tags
     * @param registryStore the registry store
     * @param scheduledExecutorService function to create the scheduled executor service
     */
    public InMemoryGenericBulkheadRegistry(Map<String, GenericBulkheadConfig> configs,
                                           List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers,
                                           Map<String, String> tags, RegistryStore<GenericBulkhead> registryStore,
                                           Function<GenericBulkheadConfig, ScheduledExecutorService> scheduledExecutorService) {
        super(configs.getOrDefault(DEFAULT_CONFIG, ThreadPoolBulkheadConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(emptyMap()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        this.configurations.putAll(configs);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<GenericBulkhead> getAllBulkheads() {
        return new HashSet<>(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name) {
        return bulkhead(name, emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name, Map<String, String> tags) {
        return bulkhead(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name, GenericBulkheadConfig config) {
        return bulkhead(name, config, emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name, GenericBulkheadConfig config, Map<String, String> tags) {
        return computeIfAbsent(name, () -> {
            GenericBulkheadConfig bulkheadConfig = Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL);
            return GenericBulkhead
                .of(name, getAllTags(tags), scheduledExecutorService.apply(bulkheadConfig), bulkheadConfig);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name,
        Supplier<GenericBulkheadConfig> bulkheadConfigSupplier) {
        return bulkhead(name, bulkheadConfigSupplier, emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name,
        Supplier<GenericBulkheadConfig> bulkheadConfigSupplier, Map<String, String> tags) {
        return computeIfAbsent(name, () -> {
            GenericBulkheadConfig bulkheadConfig = Objects.requireNonNull(
                    Objects.requireNonNull(bulkheadConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
                    CONFIG_MUST_NOT_BE_NULL);
            return GenericBulkhead.of(name, getAllTags(tags), scheduledExecutorService.apply(bulkheadConfig), bulkheadConfig);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name, String configName) {
        return bulkhead(name, configName, emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBulkhead bulkhead(String name, String configName, Map<String, String> tags) {
        return computeIfAbsent(name, () -> {
            GenericBulkheadConfig bulkheadConfig = getConfiguration(configName)
                    .orElseThrow(() -> new ConfigurationNotFoundException(configName));
            return GenericBulkhead.of(name, getAllTags(tags), scheduledExecutorService.apply(bulkheadConfig), bulkheadConfig);
        });
    }

    @Override
    public void close() throws Exception {
        for (GenericBulkhead bulkhead : getAllBulkheads()) {
            bulkhead.close();
        }
    }
}
