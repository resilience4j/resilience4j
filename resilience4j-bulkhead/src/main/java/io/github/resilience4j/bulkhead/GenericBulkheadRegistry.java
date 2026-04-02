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
package io.github.resilience4j.bulkhead;


import io.github.resilience4j.bulkhead.internal.InMemoryGenericBulkheadRegistry;
import io.github.resilience4j.bulkhead.internal.InMemoryThreadPoolBulkheadRegistry;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * The {@link GenericBulkheadRegistry} is a factory to create ThreadPoolBulkhead instances which
 * stores all bulkhead instances in a registry.
 */
public interface GenericBulkheadRegistry extends
    Registry<GenericBulkhead, GenericBulkheadConfig>, AutoCloseable {

    /**
     * Creates a BulkheadRegistry with a custom Bulkhead configuration.
     *
     * @param bulkheadConfig a custom ThreadPoolBulkhead configuration
     * @return a ThreadPoolBulkheadRegistry instance backed by a custom ThreadPoolBulkhead
     * configuration
     */
    static GenericBulkheadRegistry of(GenericBulkheadConfig bulkheadConfig, Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new InMemoryGenericBulkheadRegistry(bulkheadConfig, executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a custom default ThreadPoolBulkhead configuration
     * and a ThreadPoolBulkhead registry event consumer.
     *
     * @param bulkheadConfig        a custom default ThreadPoolBulkhead configuration.
     * @param registryEventConsumer a ThreadPoolBulkhead registry event consumer.
     * @return a ThreadPoolBulkheadRegistry with a custom ThreadPoolBulkhead configuration and a
     * ThreadPoolBulkhead registry event consumer.
     */
    static GenericBulkheadRegistry of(GenericBulkheadConfig bulkheadConfig,
                                      RegistryEventConsumer<GenericBulkhead> registryEventConsumer, Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new InMemoryGenericBulkheadRegistry(bulkheadConfig, registryEventConsumer, executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a custom default ThreadPoolBulkhead configuration
     * and a list of ThreadPoolBulkhead registry event consumers.
     *
     * @param bulkheadConfig         a custom default ThreadPoolBulkhead configuration.
     * @param registryEventConsumers a list of ThreadPoolBulkhead registry event consumers.
     * @return a ThreadPoolBulkheadRegistry with a custom ThreadPoolBulkhead configuration and a
     * list of ThreadPoolBulkhead registry event consumers.
     */
    static GenericBulkheadRegistry of(GenericBulkheadConfig bulkheadConfig,
                                      List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers,
                                      Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new InMemoryGenericBulkheadRegistry(bulkheadConfig, registryEventConsumers, executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a default ThreadPoolBulkhead configuration
     *
     * @return a ThreadPoolBulkheadRegistry instance backed by a default ThreadPoolBulkhead
     * configuration
     */
    static GenericBulkheadRegistry ofDefaults(Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return ofDefaults(emptyMap(), executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a default ThreadPoolBulkhead configuration
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param tags default tags to add to the registry
     * @return a ThreadPoolBulkheadRegistry instance backed by a default ThreadPoolBulkhead
     * configuration
     */
    static GenericBulkheadRegistry ofDefaults(Map<String, String> tags,
                                              Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new InMemoryGenericBulkheadRegistry(GenericBulkheadConfig.ofDefaults(), tags, executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations.
     *
     * @param configs a Map of shared Bulkhead configurations
     * @return a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations.
     */
    static GenericBulkheadRegistry of(Map<String, GenericBulkheadConfig> configs, Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return of(configs, emptyMap(), executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared Bulkhead configurations
     * @param tags    default tags to add to the registry
     * @return a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations.
     */
    static GenericBulkheadRegistry of(Map<String, GenericBulkheadConfig> configs, Map<String, String> tags,
                                      Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new InMemoryGenericBulkheadRegistry(configs, tags, executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a ThreadPoolBulkhead registry event consumer.
     *
     * @param configs               a Map of shared ThreadPoolBulkhead configurations.
     * @param registryEventConsumer a ThreadPoolBulkhead registry event consumer.
     * @return a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a ThreadPoolBulkhead registry event consumer.
     */
    static GenericBulkheadRegistry of(Map<String, GenericBulkheadConfig> configs,
                                      RegistryEventConsumer<GenericBulkhead> registryEventConsumer,
                                      Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return of(configs, registryEventConsumer, emptyMap(), executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a ThreadPoolBulkhead registry event consumer.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs               a Map of shared ThreadPoolBulkhead configurations.
     * @param registryEventConsumer a ThreadPoolBulkhead registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a ThreadPoolBulkhead registry event consumer.
     */
    static GenericBulkheadRegistry of(Map<String, GenericBulkheadConfig> configs,
                                      RegistryEventConsumer<GenericBulkhead> registryEventConsumer, Map<String, String> tags,
                                      Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new InMemoryGenericBulkheadRegistry(configs, registryEventConsumer, tags, executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a list of ThreadPoolBulkhead registry event consumers.
     *
     * @param configs                a Map of shared ThreadPoolBulkhead configurations.
     * @param registryEventConsumers a list of ThreadPoolBulkhead registry event consumers.
     * @return a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a list of ThreadPoolBulkhead registry event consumers.
     */
    static GenericBulkheadRegistry of(Map<String, GenericBulkheadConfig> configs,
                                      List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers,
                                      Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return of(configs, registryEventConsumers, emptyMap(), executorFunction);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a list of ThreadPoolBulkhead registry event consumers.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs                a Map of shared ThreadPoolBulkhead configurations.
     * @param registryEventConsumers a list of ThreadPoolBulkhead registry event consumers.
     * @param tags                   Tags to add to the ThreadPoolBulkhead
     * @return a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations
     * and a list of ThreadPoolBulkhead registry event consumers.
     */
    static GenericBulkheadRegistry of(Map<String, GenericBulkheadConfig> configs,
                                      List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers, Map<String, String> tags,
                                      Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new InMemoryGenericBulkheadRegistry(configs, registryEventConsumers, tags, executorFunction);
    }

    /**
     * Returns all managed {@link GenericBulkhead} instances.
     *
     * @return all managed {@link GenericBulkhead} instances.
     */
    Set<GenericBulkhead> getAllBulkheads();

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one with default
     * configuration.
     *
     * @param name the name of the ThreadPoolBulkhead
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name);

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one with default
     * configuration.
     *
     * @param name the name of the ThreadPoolBulkhead
     * @param tags Tags to add to the ThreadPoolBulkhead
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name, Map<String, String> tags);

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one with a custom
     * ThreadPoolBulkhead configuration.
     *
     * @param name   the name of the ThreadPoolBulkhead
     * @param config a custom ThreadPoolBulkheadConfig configuration
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name, GenericBulkheadConfig config);

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one with a custom
     * ThreadPoolBulkhead configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name   the name of the ThreadPoolBulkhead
     * @param config a custom ThreadPoolBulkheadConfig configuration
     * @param tags   tags to add to the ThreadPoolBulkhead
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name, GenericBulkheadConfig config, Map<String, String> tags);

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one with a custom
     * ThreadPoolBulkhead configuration.
     *
     * @param name                   the name of the ThreadPoolBulkhead
     * @param bulkheadConfigSupplier a custom ThreadPoolBulkhead configuration supplier
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name,
        Supplier<GenericBulkheadConfig> bulkheadConfigSupplier);

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one with a custom
     * ThreadPoolBulkhead configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                   the name of the ThreadPoolBulkhead
     * @param bulkheadConfigSupplier a custom ThreadPoolBulkhead configuration supplier
     * @param tags                   tags to add to the ThreadPoolBulkhead
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name,
        Supplier<GenericBulkheadConfig> bulkheadConfigSupplier, Map<String, String> tags);

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the ThreadPoolBulkhead
     * @param configName the name of the shared configuration
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name, String configName);

    /**
     * Returns a managed {@link GenericBulkhead} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the ThreadPoolBulkhead
     * @param configName the name of the shared configuration
     * @param tags       tags to add to the ThreadPoolBulkhead
     * @return The {@link GenericBulkhead}
     */
    GenericBulkhead bulkhead(String name, String configName, Map<String, String> tags);

    /**
     * Returns a builder to create a custom ThreadPoolBulkheadRegistry.
     *
     * @return a {@link GenericBulkheadRegistry.Builder}
     */
    static Builder custom(Function<GenericBulkheadConfig, ScheduledExecutorService> executorFunction) {
        return new Builder(executorFunction);
    }

    class Builder {

        private static final String DEFAULT_CONFIG = "default";
        private RegistryStore<GenericBulkhead> registryStore;
        private Map<String, GenericBulkheadConfig> threadPoolBulkheadConfigsMap;
        private List<RegistryEventConsumer<GenericBulkhead>> registryEventConsumers;
        private Map<String, String> tags;
        private Function<GenericBulkheadConfig, ScheduledExecutorService> executorServiceFunction;


        public Builder(@NonNull Function<GenericBulkheadConfig, ScheduledExecutorService> executorServiceFunction) {
            this.threadPoolBulkheadConfigsMap = new java.util.HashMap<>();
            this.registryEventConsumers = new ArrayList<>();
            this.executorServiceFunction = executorServiceFunction;
        }

        public Builder withRegistryStore(RegistryStore<GenericBulkhead> registryStore) {
            this.registryStore = registryStore;
            return this;
        }

        /**
         * Configures a ThreadPoolBulkheadRegistry with a custom default ThreadPoolBulkhead configuration.
         *
         * @param threadPoolBulkheadConfig a custom default ThreadPoolBulkhead configuration
         * @return a {@link GenericBulkheadRegistry.Builder}
         */
        public Builder withThreadPoolBulkheadConfig(GenericBulkheadConfig threadPoolBulkheadConfig) {
            threadPoolBulkheadConfigsMap.put(DEFAULT_CONFIG, threadPoolBulkheadConfig);
            return this;
        }

        /**
         * Configures a ThreadPoolBulkheadRegistry with a custom ThreadPoolBulkhead configuration.
         *
         * @param configName configName for a custom shared ThreadPoolBulkhead configuration
         * @param configuration a custom shared ThreadPoolBulkhead configuration
         * @return a {@link GenericBulkheadRegistry.Builder}
         * @throws IllegalArgumentException if {@code configName.equals("default")}
         */
        public Builder addThreadPoolBulkheadConfig(String configName, ThreadPoolBulkheadConfig configuration) {
            if (configName.equals(DEFAULT_CONFIG)) {
                throw new IllegalArgumentException(
                    "You cannot add another configuration with name 'default' as it is preserved for default configuration");
            }
            threadPoolBulkheadConfigsMap.put(configName, configuration);
            return this;
        }

        /**
         * Configures a ThreadPoolBulkheadRegistry with a ThreadPoolBulkhead registry event consumer.
         *
         * @param registryEventConsumer a ThreadPoolBulkhead registry event consumer.
         * @return a {@link GenericBulkheadRegistry.Builder}
         */
        public Builder addRegistryEventConsumer(RegistryEventConsumer<GenericBulkhead> registryEventConsumer) {
            this.registryEventConsumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Configures a ThreadPoolBulkheadRegistry with Tags.
         * <p>
         * Tags added to the registry will be added to every instance created by this registry.
         *
         * @param tags default tags to add to the registry.
         * @return a {@link GenericBulkheadRegistry.Builder}
         */
        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds a ThreadPoolBulkheadRegistry
         *
         * @return the ThreadPoolBulkheadRegistry
         */
        public GenericBulkheadRegistry build() {
            return new InMemoryGenericBulkheadRegistry(threadPoolBulkheadConfigsMap, registryEventConsumers, tags,
                registryStore, executorServiceFunction);
        }
    }
}
