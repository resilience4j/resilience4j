/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * Backend RateLimiter manager. Constructs backend RateLimiters according to configuration values.
 */
public class InMemoryRateLimiterRegistry extends
    AbstractRegistry<RateLimiter, RateLimiterConfig> implements RateLimiterRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryRateLimiterRegistry() {
        this(RateLimiterConfig.ofDefaults());
    }

    public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs) {
        this(configs, emptyMap());
    }

    public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()), tags);
        this.configurations.putAll(configs);
    }

    public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs,
        RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        this(configs, registryEventConsumer, emptyMap());
    }

    public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs,
        RegistryEventConsumer<RateLimiter> registryEventConsumer, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()),
            registryEventConsumer, tags);
        this.configurations.putAll(configs);
    }

    public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs,
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        this(configs, registryEventConsumers, emptyMap());
    }

    public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs,
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()),
            registryEventConsumers, tags);
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig) {
        super(defaultConfig);
    }

    public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig, Map<String, String> tags) {
        super(defaultConfig, tags);
    }

    public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig,
        RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig,
        RegistryEventConsumer<RateLimiter> registryEventConsumer, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig,
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig,
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs,
                                          List<RegistryEventConsumer<RateLimiter>> registryEventConsumers,
                                          Map<String, String> tags, RegistryStore<RateLimiter> registryStore) {
        super(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(emptyMap()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        this.configurations.putAll(configs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RateLimiter> getAllRateLimiters() {
        return new HashSet<>(entryMap.values());
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
    public RateLimiter rateLimiter(String name, Map<String, String> tags) {
        return rateLimiter(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name, final RateLimiterConfig config) {
        return rateLimiter(name, config, emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, RateLimiterConfig config, Map<String, String> tags) {
        return computeIfAbsent(name, () -> new AtomicRateLimiter(name,
            Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(final String name,
        final Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
        return rateLimiter(name, rateLimiterConfigSupplier, emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name,
        Supplier<RateLimiterConfig> rateLimiterConfigSupplier, Map<String, String> tags) {
        return computeIfAbsent(name, () -> new AtomicRateLimiter(name, Objects.requireNonNull(
            Objects.requireNonNull(rateLimiterConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, String configName) {
        return rateLimiter(name, configName, emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiter rateLimiter(String name, String configName, Map<String, String> tags) {
        return computeIfAbsent(name, () -> RateLimiter.of(name, getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName)), getAllTags(tags)));
    }
}
