/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.HedgeRegistry;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Backend Hedge manager. Constructs backend Hedges according to configuration values.
 */
public class InMemoryHedgeRegistry extends
    AbstractRegistry<Hedge, HedgeConfig> implements HedgeRegistry {

    /**
     * Constructor
     *
     * @param defaultConfig          the default config to use for new Hedges
     * @param registryEventConsumers initialized consumers for hedges
     * @param tags                   a map of tags for the registry
     */
    public InMemoryHedgeRegistry(HedgeConfig defaultConfig,
                                 List<RegistryEventConsumer<Hedge>> registryEventConsumers,
                                 Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);

    }

    public static class Builder {
        private final Map<String, String> tags = new HashMap<>();
        private final Map<String, HedgeConfig> configs = new HashMap<>();
        private HedgeConfig defaultConfig = HedgeConfig.ofDefaults();
        private final List<RegistryEventConsumer<Hedge>> consumers = new ArrayList<>();

        public Builder withTags(Map<String, String> tags) {
            this.tags.putAll(tags);
            return this;
        }

        public Builder withConfigs(Map<String, HedgeConfig> configs) {
            this.configs.putAll(configs);
            return this;
        }

        public Builder withDefaultConfig(HedgeConfig config) {
            if (config == null) {
                throw new NullPointerException(CONFIG_MUST_NOT_BE_NULL);
            } else {
                this.defaultConfig = config;
                return this;
            }
        }

        public Builder withConsumers(List<RegistryEventConsumer<Hedge>> registryEventConsumers) {
            this.consumers.addAll(registryEventConsumers);
            return this;
        }

        public Builder withConsumer(RegistryEventConsumer<Hedge> registryEventConsumer) {
            this.consumers.add(registryEventConsumer);
            return this;
        }

        public HedgeRegistry build() {
            configs.remove("default");
            HedgeRegistry registry = new InMemoryHedgeRegistry(defaultConfig, consumers, tags);
            configs.forEach(registry::addConfiguration);
            return registry;
        }
    }

    @Override
    public Stream<Hedge> getAllHedges() {
        return entryMap.values().stream();
    }

    @Override
    public Hedge hedge(final String name) {
        return hedge(name, getDefaultConfig(), emptyMap());
    }

    @Override
    public Hedge hedge(String name,
                       Map<String, String> tags) {
        return hedge(name, getDefaultConfig(), tags);
    }

    @Override
    public Hedge hedge(final String name, final HedgeConfig config) {
        return hedge(name, config, emptyMap());
    }

    @Override
    public Hedge hedge(String name,
                       HedgeConfig hedgeConfig,
                       Map<String, String> tags) {
        return computeIfAbsent(name, () -> Hedge.of(name,
            Objects.requireNonNull(hedgeConfig, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    @Override
    public Hedge hedge(final String name,
                       final Supplier<HedgeConfig> hedgeConfigSupplier) {
        return hedge(name, hedgeConfigSupplier, emptyMap());
    }

    @Override
    public Hedge hedge(String name,
                       Supplier<HedgeConfig> hedgeConfigSupplier,
                       Map<String, String> tags) {
        return computeIfAbsent(name, () -> Hedge.of(name, Objects.requireNonNull(
            Objects.requireNonNull(hedgeConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
    }

    @Override
    public Hedge hedge(String name, String configName) {
        return hedge(name, configName, emptyMap());
    }

    @Override
    public Hedge hedge(String name, String configName,
                       Map<String, String> tags) {
        HedgeConfig config = getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName));
        return hedge(name, config, tags);
    }
}
