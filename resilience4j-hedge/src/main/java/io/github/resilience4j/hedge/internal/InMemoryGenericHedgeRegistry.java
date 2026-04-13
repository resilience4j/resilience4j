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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Backend Hedge manager. Constructs backend Hedges according to configuration values.
 */
public class InMemoryGenericHedgeRegistry extends
    AbstractRegistry<GenericHedge, SimpleHedgeConfig> implements GenericHedgeRegistry<GenericHedge, SimpleHedgeConfig> {

    private final Function<SimpleHedgeConfig, ScheduledExecutorService> executorFunction;
    /**
     * Constructor
     *
     * @param defaultConfig          the default config to use for new Hedges
     * @param registryEventConsumers initialized consumers for hedges
     * @param tags                   a map of tags for the registry
     * @param executorFunction       function that provides an executor service based on config
     */
    public InMemoryGenericHedgeRegistry(SimpleHedgeConfig defaultConfig,
                                        List<RegistryEventConsumer<GenericHedge>> registryEventConsumers,
                                        Map<String, String> tags,
                                        @NonNull Function<SimpleHedgeConfig, ScheduledExecutorService> executorFunction) {
        super(defaultConfig, registryEventConsumers, tags);
        this.executorFunction = executorFunction;
    }

    /**
     * Builder for an InMemoryGenericHedgeRegistry.
     */
    public static class Builder {
        private final Map<String, String> tags = new HashMap<>();
        private final Map<String, SimpleHedgeConfig> configs = new HashMap<>();
        private SimpleHedgeConfig defaultConfig = SimpleHedgeConfig.ofDefaults();
        private final List<RegistryEventConsumer<GenericHedge>> consumers = new ArrayList<>();
        private final Function<SimpleHedgeConfig, ScheduledExecutorService> executorServiceFunction;

        /**
         * Constructor for the Builder.
         *
         * @param executorFunction function to provide executor service
         */
        public Builder(@NonNull Function<SimpleHedgeConfig, ScheduledExecutorService> executorFunction) {
            this.executorServiceFunction = executorFunction;
        }

        /**
         * Adds tags to the registry.
         *
         * @param tags the tags
         * @return the Builder
         */
        public Builder withTags(Map<String, String> tags) {
            this.tags.putAll(tags);
            return this;
        }

        /**
         * Adds configurations to the registry.
         *
         * @param configs the configurations
         * @return the Builder
         */
        public Builder withConfigs(Map<String, SimpleHedgeConfig> configs) {
            this.configs.putAll(configs);
            return this;
        }

        /**
         * Sets the default configuration.
         *
         * @param config the default configuration
         * @return the Builder
         * @throws NullPointerException if the config is null
         */
        public Builder withDefaultConfig(SimpleHedgeConfig config) {
            if (config == null) {
                throw new NullPointerException(CONFIG_MUST_NOT_BE_NULL);
            } else {
                this.defaultConfig = config;
                return this;
            }
        }

        /**
         * Adds consumers to the registry.
         *
         * @param registryEventConsumers the consumers
         * @return the Builder
         */
        public Builder withConsumers(List<RegistryEventConsumer<GenericHedge>> registryEventConsumers) {
            this.consumers.addAll(registryEventConsumers);
            return this;
        }

        /**
         * Adds a single consumer to the registry.
         *
         * @param registryEventConsumer the consumer
         * @return the Builder
         */
        public Builder withConsumer(RegistryEventConsumer<GenericHedge> registryEventConsumer) {
            this.consumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Builds the registry.
         *
         * @return the newly constructed GenericHedgeRegistry
         */
        public GenericHedgeRegistry<? super GenericHedge, ? super SimpleHedgeConfig> build() {
            configs.remove("default");
            GenericHedgeRegistry<? super GenericHedge, ? super SimpleHedgeConfig> registry = new InMemoryGenericHedgeRegistry(defaultConfig, consumers, tags, executorServiceFunction);
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
        return computeIfAbsent(name, () -> {
            SimpleHedgeConfig simpleHedgeConfig = Objects.requireNonNull(hedgeConfig, CONFIG_MUST_NOT_BE_NULL);
            return GenericHedge.of(name,
                    simpleHedgeConfig, getAllTags(tags), executorFunction.apply(simpleHedgeConfig));
        });
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
        return computeIfAbsent(name, () -> {
            SimpleHedgeConfig simpleHedgeConfig = Objects.requireNonNull(
                    Objects.requireNonNull(hedgeConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(),
                    CONFIG_MUST_NOT_BE_NULL);
            return GenericHedge.of(name, simpleHedgeConfig, getAllTags(tags), executorFunction.apply(simpleHedgeConfig));
        });
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
