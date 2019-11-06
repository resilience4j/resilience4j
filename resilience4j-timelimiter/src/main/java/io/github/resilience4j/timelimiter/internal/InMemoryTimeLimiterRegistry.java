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
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Backend TimeLimiter manager. Constructs backend TimeLimiters according to configuration values.
 */
public class InMemoryTimeLimiterRegistry extends
    AbstractRegistry<TimeLimiter, TimeLimiterConfig> implements TimeLimiterRegistry {

    /**
     * The constructor with default default.
     */
    public InMemoryTimeLimiterRegistry() {
        this(TimeLimiterConfig.ofDefaults());
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()));
        this.configurations.putAll(configs);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()),
            registryEventConsumer);
        this.configurations.putAll(configs);
    }

    public InMemoryTimeLimiterRegistry(Map<String, TimeLimiterConfig> configs,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers) {
        this(configs.getOrDefault(DEFAULT_CONFIG, TimeLimiterConfig.ofDefaults()),
            registryEventConsumers);
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

    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer) {
        super(defaultConfig, registryEventConsumer);
    }

    public InMemoryTimeLimiterRegistry(TimeLimiterConfig defaultConfig,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers) {
        super(defaultConfig, registryEventConsumers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Seq<TimeLimiter> getAllTimeLimiters() {
        return Array.ofAll(entryMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(final String name) {
        return timeLimiter(name, getDefaultConfig());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(final String name, final TimeLimiterConfig config) {
        return computeIfAbsent(name, () -> new TimeLimiterImpl(name,
            Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(final String name,
        final Supplier<TimeLimiterConfig> timeLimiterConfigSupplier) {
        return computeIfAbsent(name, () -> {
            TimeLimiterConfig config = Objects
                .requireNonNull(timeLimiterConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get();
            return new TimeLimiterImpl(name,
                Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeLimiter timeLimiter(String name, String configName) {
        return computeIfAbsent(name, () -> {
            TimeLimiterConfig config = getConfiguration(configName)
                .orElseThrow(() -> new ConfigurationNotFoundException(configName));
            return TimeLimiter.of(name, config);
        });
    }
}
