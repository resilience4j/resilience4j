/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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


import io.github.resilience4j.bulkhead.internal.InMemoryBulkheadRegistry;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The {@link BulkheadRegistry} is a factory to create Bulkhead instances which stores all bulkhead
 * instances in a registry.
 */
public interface BulkheadRegistry extends Registry<Bulkhead, BulkheadConfig> {

    /**
     * Creates a BulkheadRegistry with a custom Bulkhead configuration.
     *
     * @param bulkheadConfig a custom Bulkhead configuration
     * @return a BulkheadRegistry instance backed by a custom Bulkhead configuration
     */
    static BulkheadRegistry of(BulkheadConfig bulkheadConfig) {
        return new InMemoryBulkheadRegistry(bulkheadConfig);
    }

    /**
     * Creates a BulkheadRegistry with a custom Bulkhead configuration.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param bulkheadConfig a custom Bulkhead configuration
     * @param tags           default tags to add to the registry
     * @return a BulkheadRegistry instance backed by a custom Bulkhead configuration
     */
    static BulkheadRegistry of(BulkheadConfig bulkheadConfig, Map<String, String> tags) {
        return new InMemoryBulkheadRegistry(bulkheadConfig, tags);
    }

    /**
     * Creates a BulkheadRegistry with a custom default Bulkhead configuration and a Bulkhead
     * registry event consumer.
     *
     * @param bulkheadConfig        a custom default Bulkhead configuration.
     * @param registryEventConsumer a Bulkhead registry event consumer.
     * @return a BulkheadRegistry with a custom Bulkhead configuration and a Bulkhead registry event
     * consumer.
     */
    static BulkheadRegistry of(BulkheadConfig bulkheadConfig,
        RegistryEventConsumer<Bulkhead> registryEventConsumer) {
        return new InMemoryBulkheadRegistry(bulkheadConfig, registryEventConsumer);
    }

    /**
     * Creates a BulkheadRegistry with a custom default Bulkhead configuration and a list of
     * Bulkhead registry event consumers.
     *
     * @param bulkheadConfig         a custom default Bulkhead configuration.
     * @param registryEventConsumers a list of Bulkhead registry event consumers.
     * @return a BulkheadRegistry with a custom Bulkhead configuration and a list of Bulkhead
     * registry event consumers.
     */
    static BulkheadRegistry of(BulkheadConfig bulkheadConfig,
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers) {
        return new InMemoryBulkheadRegistry(bulkheadConfig, registryEventConsumers);
    }

    /**
     * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations.
     *
     * @param configs a Map of shared Bulkhead configurations
     * @return a RetryRegistry with a Map of shared Bulkhead configurations.
     */
    static BulkheadRegistry of(Map<String, BulkheadConfig> configs) {
        return new InMemoryBulkheadRegistry(configs);
    }

    /**
     * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared Bulkhead configurations
     * @param tags    default tags to add to the registry
     * @return a RetryRegistry with a Map of shared Bulkhead configurations.
     */
    static BulkheadRegistry of(Map<String, BulkheadConfig> configs, Map<String, String> tags) {
        return new InMemoryBulkheadRegistry(configs, tags);
    }

    /**
     * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations and a Bulkhead
     * registry event consumer.
     *
     * @param configs               a Map of shared Bulkhead configurations.
     * @param registryEventConsumer a Bulkhead registry event consumer.
     * @return a BulkheadRegistry with a Map of shared Bulkhead configurations and a Bulkhead
     * registry event consumer.
     */
    static BulkheadRegistry of(Map<String, BulkheadConfig> configs,
        RegistryEventConsumer<Bulkhead> registryEventConsumer) {
        return new InMemoryBulkheadRegistry(configs, registryEventConsumer);
    }

    /**
     * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations and a Bulkhead
     * registry event consumer.
     *
     * @param configs               a Map of shared Bulkhead configurations.
     * @param registryEventConsumer a Bulkhead registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a BulkheadRegistry with a Map of shared Bulkhead configurations and a Bulkhead
     * registry event consumer.
     */
    static BulkheadRegistry of(Map<String, BulkheadConfig> configs,
        RegistryEventConsumer<Bulkhead> registryEventConsumer, Map<String, String> tags) {
        return new InMemoryBulkheadRegistry(configs, registryEventConsumer, tags);
    }

    /**
     * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations and a list of
     * Bulkhead registry event consumers.
     *
     * @param configs                a Map of shared Bulkhead configurations.
     * @param registryEventConsumers a list of Bulkhead registry event consumers.
     * @return a BulkheadRegistry with a Map of shared Bulkhead configurations and a list of
     * Bulkhead registry event consumers.
     */
    static BulkheadRegistry of(Map<String, BulkheadConfig> configs,
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers) {
        return new InMemoryBulkheadRegistry(configs, registryEventConsumers);
    }

    /**
     * Creates a BulkheadRegistry with a default Bulkhead configuration
     *
     * @return a BulkheadRegistry instance backed by a default Bulkhead configuration
     */
    static BulkheadRegistry ofDefaults() {
        return new InMemoryBulkheadRegistry(BulkheadConfig.ofDefaults());
    }

    /**
     * Returns all managed {@link Bulkhead} instances.
     *
     * @return all managed {@link Bulkhead} instances.
     */
    Set<Bulkhead> getAllBulkheads();

    /**
     * Returns a managed {@link Bulkhead} or creates a new one with default configuration.
     *
     * @param name the name of the Bulkhead
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name);

    /**
     * Returns a managed {@link Bulkhead} or creates a new one with default configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the Bulkhead
     * @param tags tags to add to the bulkhead
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name, Map<String, String> tags);

    /**
     * Returns a managed {@link Bulkhead} or creates a new one with a custom BulkheadConfig
     * configuration.
     *
     * @param name   the name of the Bulkhead
     * @param config a custom Bulkhead configuration
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name, BulkheadConfig config);

    /**
     * Returns a managed {@link Bulkhead} or creates a new one with a custom BulkheadConfig
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name   the name of the Bulkhead
     * @param config a custom Bulkhead configuration
     * @param tags   tags added to the bulkhead
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name, BulkheadConfig config, Map<String, String> tags);

    /**
     * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead
     * configuration.
     *
     * @param name                   the name of the Bulkhead
     * @param bulkheadConfigSupplier a custom Bulkhead configuration supplier
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier);

    /**
     * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                   the name of the Bulkhead
     * @param bulkheadConfigSupplier a custom Bulkhead configuration supplier
     * @param tags                   tags to add to the Bulkhead
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier, Map<String, String> tags);

    /**
     * Returns a managed {@link Bulkhead} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the Bulkhead
     * @param configName the name of the shared configuration
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name, String configName);

    /**
     * Returns a managed {@link Bulkhead} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the Bulkhead
     * @param configName the name of the shared configuration
     * @param tags       tags to add to the Bulkhead
     * @return The {@link Bulkhead}
     */
    Bulkhead bulkhead(String name, String configName, Map<String, String> tags);

    /**
     * Returns a builder to create a custom BulkheadRegistry.
     *
     * @return a {@link BulkheadRegistry.Builder}
     */
    static Builder custom() {
        return new Builder();
    }

    class Builder {

        private static final String DEFAULT_CONFIG = "default";
        private RegistryStore<Bulkhead> registryStore;
        private Map<String, BulkheadConfig> bulkheadConfigsMap;
        private List<RegistryEventConsumer<Bulkhead>> registryEventConsumers;
        private Map<String, String> tags;

        public Builder() {
            this.bulkheadConfigsMap = new java.util.HashMap<>();
            this.registryEventConsumers = new ArrayList<>();
        }

        public Builder withRegistryStore(RegistryStore<Bulkhead> registryStore) {
            this.registryStore = registryStore;
            return this;
        }

        /**
         * Configures a BulkheadRegistry with a custom default Bulkhead configuration.
         *
         * @param bulkheadConfig a custom default Bulkhead configuration
         * @return a {@link BulkheadRegistry.Builder}
         */
        public Builder withBulkheadConfig(BulkheadConfig bulkheadConfig) {
            bulkheadConfigsMap.put(DEFAULT_CONFIG, bulkheadConfig);
            return this;
        }

        /**
         * Configures a BulkheadRegistry with a custom Bulkhead configuration.
         *
         * @param configName configName for a custom shared Bulkhead configuration
         * @param configuration a custom shared Bulkhead configuration
         * @return a {@link BulkheadRegistry.Builder}
         * @throws IllegalArgumentException if {@code configName.equals("default")}
         */
        public Builder addBulkheadConfig(String configName, BulkheadConfig configuration) {
            if (configName.equals(DEFAULT_CONFIG)) {
                throw new IllegalArgumentException(
                    "You cannot add another configuration with name 'default' as it is preserved for default configuration");
            }
            bulkheadConfigsMap.put(configName, configuration);
            return this;
        }

        /**
         * Configures a BulkheadRegistry with a Bulkhead registry event consumer.
         *
         * @param registryEventConsumer a Bulkhead registry event consumer.
         * @return a {@link BulkheadRegistry.Builder}
         */
        public Builder addRegistryEventConsumer(RegistryEventConsumer<Bulkhead> registryEventConsumer) {
            this.registryEventConsumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Configures a BulkheadRegistry with Tags.
         * <p>
         * Tags added to the registry will be added to every instance created by this registry.
         *
         * @param tags default tags to add to the registry.
         * @return a {@link BulkheadRegistry.Builder}
         */
        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds a BulkheadRegistry
         *
         * @return the BulkheadRegistry
         */
        public BulkheadRegistry build() {
            return new InMemoryBulkheadRegistry(bulkheadConfigsMap, registryEventConsumers, tags,
                registryStore);
        }
    }
}
