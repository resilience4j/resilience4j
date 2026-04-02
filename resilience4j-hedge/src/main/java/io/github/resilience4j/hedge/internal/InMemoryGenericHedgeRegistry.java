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
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.hedge.*;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Backend Hedge manager. Constructs backend Hedges according to configuration values.
 */
public class InMemoryGenericHedgeRegistry extends
    AbstractRegistry<GenericHedge, SimpleHedgeConfig> implements GenericHedgeRegistry<GenericHedge, SimpleHedgeConfig> {

    private final ScheduledExecutorService defaultExecutorService;

    /**
     * Constructor
     *
     * @param defaultConfig          the default config to use for new Hedges
     * @param registryEventConsumers initialized consumers for hedges
     * @param tags                   a map of tags for the registry
     */
    public InMemoryGenericHedgeRegistry(SimpleHedgeConfig defaultConfig,
                                        List<RegistryEventConsumer<GenericHedge>> registryEventConsumers,
                                        Map<String, String> tags,
                                        @NonNull ScheduledExecutorService executorService) {
        super(defaultConfig, registryEventConsumers, tags);
        this.defaultExecutorService = executorService;
    }

    public static class Builder {
        private final Map<String, String> tags = new HashMap<>();
        private final Map<String, SimpleHedgeConfig> configs = new HashMap<>();
        private SimpleHedgeConfig defaultConfig = SimpleHedgeConfig.ofDefaults();
        private final List<RegistryEventConsumer<GenericHedge>> consumers = new ArrayList<>();
        private final ScheduledExecutorService defaultExecutorService;

        public Builder(@NonNull ScheduledExecutorService executorService) {
            this.defaultExecutorService = executorService;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags.putAll(tags);
            return this;
        }

        public Builder withConfigs(Map<String, SimpleHedgeConfig> configs) {
            this.configs.putAll(configs);
            return this;
        }

        public Builder withDefaultConfig(SimpleHedgeConfig config) {
            if (config == null) {
                throw new NullPointerException(CONFIG_MUST_NOT_BE_NULL);
            } else {
                this.defaultConfig = config;
                return this;
            }
        }

        public Builder withConsumers(List<RegistryEventConsumer<GenericHedge>> registryEventConsumers) {
            this.consumers.addAll(registryEventConsumers);
            return this;
        }

        public Builder withConsumer(RegistryEventConsumer<GenericHedge> registryEventConsumer) {
            this.consumers.add(registryEventConsumer);
            return this;
        }

        public GenericHedgeRegistry<? super GenericHedge, ? super SimpleHedgeConfig> build() {
            configs.remove("default");
            GenericHedgeRegistry<? super GenericHedge, ? super SimpleHedgeConfig> registry = new InMemoryGenericHedgeRegistry(defaultConfig, consumers, tags, defaultExecutorService);
            configs.forEach(registry::addConfiguration);
            return registry;
        }
    }

    @Override
    public Stream<GenericHedge> getAllHedges() {
        return entryMap.values().stream();
    }

    @Override
    public GenericHedge hedge(final String name) {
        return hedge(name, getDefaultConfig(), emptyMap());
    }

    @Override
    public GenericHedge hedge(String name,
                       Map<String, String> tags) {
        return hedge(name, getDefaultConfig(), tags);
    }

    @Override
    public GenericHedge hedge(final String name, final SimpleHedgeConfig config) {
        return hedge(name, config, emptyMap());
    }

    @Override
    public GenericHedge hedge(String name,
                       SimpleHedgeConfig hedgeConfig,
                       Map<String, String> tags) {
        return computeIfAbsent(name, () -> GenericHedge.of(name,
            Objects.requireNonNull(hedgeConfig, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags), defaultExecutorService));
    }

    @Override
    public GenericHedge hedge(final String name,
                       final Supplier<SimpleHedgeConfig> hedgeConfigSupplier) {
        return hedge(name, hedgeConfigSupplier, emptyMap());
    }

    @Override
    public GenericHedge hedge(String name,
                       Supplier<SimpleHedgeConfig> hedgeConfigSupplier,
                       Map<String, String> tags) {
        return computeIfAbsent(name, () -> GenericHedge.of(name, Objects.requireNonNull(
            Objects.requireNonNull(hedgeConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
            CONFIG_MUST_NOT_BE_NULL), getAllTags(tags), defaultExecutorService));
    }

    @Override
    public GenericHedge hedge(String name, String configName) {
        return hedge(name, configName, emptyMap());
    }

    @Override
    public GenericHedge hedge(String name, String configName,
                       Map<String, String> tags) {
        SimpleHedgeConfig config = getConfiguration(configName)
            .orElseThrow(() -> new ConfigurationNotFoundException(configName));
        return hedge(name, config, tags);
    }
}
