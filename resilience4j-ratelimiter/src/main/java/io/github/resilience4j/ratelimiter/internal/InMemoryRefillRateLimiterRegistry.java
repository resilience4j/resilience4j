/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.*;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Backend RateLimiter manager. Constructs backend RateLimiters according to configuration values.
 */
public class InMemoryRefillRateLimiterRegistry  extends
    AbstractRegistry<RateLimiter, RefillRateLimiterConfig> implements RefillRateLimiterRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryRefillRateLimiterRegistry() {
        this(RefillRateLimiterConfig.ofDefaults());
    }

    public InMemoryRefillRateLimiterRegistry(io.vavr.collection.Map<String, String> tags) {
        this(RefillRateLimiterConfig.ofDefaults(), tags);
    }

    public InMemoryRefillRateLimiterRegistry(Map<String, RefillRateLimiterConfig> configs) {
        this(configs, HashMap.empty());
    }

    public InMemoryRefillRateLimiterRegistry(Map<String, RefillRateLimiterConfig> configs,
                                       io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RefillRateLimiterConfig.ofDefaults()), tags);
        this.configurations.putAll(configs);
    }

    public InMemoryRefillRateLimiterRegistry(Map<String, RefillRateLimiterConfig> configs,
                                       RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        this(configs, registryEventConsumer, HashMap.empty());
    }

    public InMemoryRefillRateLimiterRegistry(Map<String, RefillRateLimiterConfig> configs,
                                       RegistryEventConsumer<RateLimiter> registryEventConsumer,
                                       io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RefillRateLimiterConfig.ofDefaults()),
            registryEventConsumer, tags);
        this.configurations.putAll(configs);
    }

    public InMemoryRefillRateLimiterRegistry(Map<String, RefillRateLimiterConfig> configs,
                                       List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        this(configs, registryEventConsumers, HashMap.empty());
    }

    public InMemoryRefillRateLimiterRegistry(Map<String, RefillRateLimiterConfig> configs,
                                       List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                       io.vavr.collection.Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RefillRateLimiterConfig.ofDefaults()),
            registryEventConsumers, tags);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public InMemoryRefillRateLimiterRegistry(RefillRateLimiterConfig defaultConfig) {
        super(defaultConfig);
    }

    public InMemoryRefillRateLimiterRegistry(RefillRateLimiterConfig defaultConfig,
                                       io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, tags);
    }

    public InMemoryRefillRateLimiterRegistry(RefillRateLimiterConfig defaultConfig,
                                       RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    public InMemoryRefillRateLimiterRegistry(RefillRateLimiterConfig defaultConfig,
                                       RegistryEventConsumer<RateLimiter> registryEventConsumer,
                                       io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    public InMemoryRefillRateLimiterRegistry(RefillRateLimiterConfig defaultConfig,
                                       List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    public InMemoryRefillRateLimiterRegistry(RefillRateLimiterConfig defaultConfig,
                                       List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                       io.vavr.collection.Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    public InMemoryRefillRateLimiterRegistry(Map<String, RefillRateLimiterConfig> configs,
                                       List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                       io.vavr.collection.Map<String, String> tags, RegistryStore<RateLimiter> registryStore) {
        super(configs.getOrDefault(DEFAULT_CONFIG, RefillRateLimiterConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(HashMap.empty()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        this.configurations.putAll(configs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Seq<RateLimiter> getAllRateLimiters() {
        return Array.ofAll(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name) {
        return rateLimiter(name, getDefaultConfig());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, io.vavr.collection.Map<String, String> tags) {
        return rateLimiter(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name, final RefillRateLimiterConfig config) {
        return rateLimiter(name, config, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, RefillRateLimiterConfig config,
                                   io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> new RefillRateLimiter(name,
            Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name,
                                   final Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier) {
        return rateLimiter(name, rateLimiterConfigSupplier, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name,
                                   Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier,
                                   io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> new RefillRateLimiter(name, Objects.requireNonNull(
            Objects.requireNonNull(rateLimiterConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, String configName) {
        return rateLimiter(name, configName, HashMap.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, String configName,
                                   io.vavr.collection.Map<String, String> tags) {
        return computeIfAbsent(name, () -> RateLimiter.of(name, getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName)), getAllTags(tags)));
    }

}
