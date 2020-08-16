/*
 * Copyright 2017 Dan Maas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.*;
import java.util.function.Supplier;

/**
 * Backend retry manager. Constructs backend retries according to configuration values.
 */
public final class InMemoryRetryRegistry extends AbstractRegistry<Retry, RetryConfig> implements
    RetryRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryRetryRegistry() {
        this(RetryConfig.ofDefaults());
    }

    /*public InMemoryRetryRegistry(Map<String, String> tags) {
        this(RetryConfig.ofDefaults(), tags);
    }*/

    public InMemoryRetryRegistry(Map<String, RetryConfig> configs) {
        this(configs, Collections.emptyMap());
    }

    public InMemoryRetryRegistry(Map<String, RetryConfig> configs, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RetryConfig.ofDefaults()), tags);
        this.configurations.putAll(configs);
    }

    public InMemoryRetryRegistry(Map<String, RetryConfig> configs,
        RegistryEventConsumer<Retry> registryEventConsumer) {
        this(configs, registryEventConsumer, Collections.emptyMap());
    }

    public InMemoryRetryRegistry(Map<String, RetryConfig> configs,
        RegistryEventConsumer<Retry> registryEventConsumer, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RetryConfig.ofDefaults()), registryEventConsumer,
            tags);
        this.configurations.putAll(configs);
    }

    public InMemoryRetryRegistry(Map<String, RetryConfig> configs,
        List<RegistryEventConsumer<Retry>> registryEventConsumers) {
        this(configs, registryEventConsumers, Collections.emptyMap());
    }

    public InMemoryRetryRegistry(Map<String, RetryConfig> configs,
        List<RegistryEventConsumer<Retry>> registryEventConsumers, Map<String, String> tags) {
        this(configs.getOrDefault(DEFAULT_CONFIG, RetryConfig.ofDefaults()),
            registryEventConsumers, tags);
        this.configurations.putAll(configs);
    }

    public InMemoryRetryRegistry(Map<String, RetryConfig> configs,
                                          List<RegistryEventConsumer<Retry>> registryEventConsumers,
                                          Map<String, String> tags, RegistryStore<Retry> registryStore) {
        super(configs.getOrDefault(DEFAULT_CONFIG, RetryConfig.ofDefaults()),
            registryEventConsumers, Optional.ofNullable(tags).orElse(Collections.emptyMap()),
            Optional.ofNullable(registryStore).orElse(new InMemoryRegistryStore<>()));
        this.configurations.putAll(configs);
    }

    /**
     * The constructor with custom default config.
     *
     * @param defaultConfig The default config.
     */
    public InMemoryRetryRegistry(RetryConfig defaultConfig) {
        this(defaultConfig, Collections.emptyMap());
    }

    public InMemoryRetryRegistry(RetryConfig defaultConfig, Map<String, String> tags) {
        super(defaultConfig, tags);
    }

    public InMemoryRetryRegistry(RetryConfig defaultConfig,
        RegistryEventConsumer<Retry> registryEventConsumer) {
        this(defaultConfig, registryEventConsumer, Collections.emptyMap());
    }

    public InMemoryRetryRegistry(RetryConfig defaultConfig,
        RegistryEventConsumer<Retry> registryEventConsumer, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumer, tags);
    }

    public InMemoryRetryRegistry(RetryConfig defaultConfig,
        List<RegistryEventConsumer<Retry>> registryEventConsumers) {
        this(defaultConfig, registryEventConsumers, Collections.emptyMap());
    }

    public InMemoryRetryRegistry(RetryConfig defaultConfig,
        List<RegistryEventConsumer<Retry>> registryEventConsumers, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Retry> getAllRetries() {
        return new HashSet<>(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry retry(String name) {
        return retry(name, getDefaultConfig());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry retry(String name, Map<String, String> tags) {
        return retry(name, getDefaultConfig(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry retry(String name, RetryConfig config) {
        return retry(name, config, Collections.emptyMap());
    }

    @Override
    public Retry retry(String name, RetryConfig config, Map<String, String> tags) {
        return computeIfAbsent(name, () -> Retry
            .of(name, Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry retry(String name, Supplier<RetryConfig> retryConfigSupplier) {
        return retry(name, retryConfigSupplier, Collections.emptyMap());
    }

    @Override
    public Retry retry(String name, Supplier<RetryConfig> retryConfigSupplier, Map<String, String> tags) {
        return computeIfAbsent(name, () -> Retry.of(name, Objects.requireNonNull(
            Objects.requireNonNull(retryConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry retry(String name, String configName) {
        return retry(name, configName, Collections.emptyMap());
    }

    @Override
    public Retry retry(String name, String configName, Map<String, String> tags) {
        return computeIfAbsent(name, () -> Retry.of(name, getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName)), getAllTags(tags)));
    }
}
