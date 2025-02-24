/*
 *
 *  Copyright 2019 authors
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
package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * Backend TimeLimiter manager. Constructs backend TimeLimiters according to configuration values.
 */
public class InMemoryTimeLimiterRegistry extends
    AbstractRegistry<TimeLimiter, TimeLimiterConfig> implements TimeLimiterRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryTimeLimiterRegistry() {
        this(TimeLimiterConfig.ofDefaults(), emptyMap());
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()));
        this.configurations.putAll(configs);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()), tags);
        this.configurations.putAll(configs);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()),
            registryEventConsumer);
        this.configurations.putAll(configs);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()), registryEventConsumer,
            tags);
        this.configurations.putAll(configs);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()),
            registryEventConsumers);
        this.configurations.putAll(configs);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()),
            registryEventConsumers, tags);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig) {
        super(defaultConfig);
    }

    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig, Map<String, String> tags) {
        super(defaultConfig, tags);
    }

    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs,
                                       List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers,
                                       Map<String, String> tags,
                                       RegistryStore<TimeLimiter> registryStore) {
        super(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()),
                registryEventConsumers, Optional.ofNullable(tags).orElse(emptyMap()),
                Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        this.configurations.putAll(configs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TimeLimiter> getAllTimeLimiters() {
        return new HashSet<>(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(final String name) {
        return timeLimiter(name, getDefaultConfig(), emptyMap());
    }

    @Override
    public TimeLimiter timeLimiter(String name, Map<String, String> tags) {
        return timeLimiter(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(final String name, final TimeLimiterConfig config) {
        return timeLimiter(name, config, emptyMap());
    }

    @Override
    public TimeLimiter timeLimiter(String name,
        TimeLimiterConfig timeLimiterConfig, Map<String, String> tags) {
        return computeIfAbsent(name, () -> TimeLimiter.of(name,
            Objects.requireNonNull(timeLimiterConfig, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(final String name,
        final Supplier<TimeLimiterConfig> timeLimiterConfigSupplier) {
        return timeLimiter(name, timeLimiterConfigSupplier, emptyMap());
    }

    @Override
    public TimeLimiter timeLimiter(String name,
        Supplier<TimeLimiterConfig> timeLimiterConfigSupplier, Map<String, String> tags) {
        return computeIfAbsent(name, () -> TimeLimiter.of(name, Objects.requireNonNull(
            Objects.requireNonNull(timeLimiterConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(String name, String configName) {
        return timeLimiter(name, configName, emptyMap());
    }

    @Override
    public TimeLimiter timeLimiter(String name, String configName, Map<String, String> tags) {
        TimeLimiterConfig config = getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName));
        return timeLimiter(name, config, tags);
    }
}
