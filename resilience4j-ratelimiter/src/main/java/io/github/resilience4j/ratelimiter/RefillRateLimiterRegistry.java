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
package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.internal.InMemoryRefillRateLimiterRegistry;
import io.vavr.collection.Seq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages all RefillRateLimiter instances.
 */
public interface RefillRateLimiterRegistry extends Registry<RateLimiter, RefillRateLimiterConfig> {

    /**
     * Creates a RefillRateLimiterRegistry with a custom RefillRateLimiter configuration.
     *
     * @param defaultRateLimiterConfig a custom RefillRateLimiter configuration
     * @return a RefillRateLimiterRegistry instance backed by a custom RateLimiter configuration
     */
    static RefillRateLimiterRegistry of(RefillRateLimiterConfig defaultRateLimiterConfig) {
        return new InMemoryRefillRateLimiterRegistry(defaultRateLimiterConfig);
    }

    /**
     * Creates a RefillRateLimiterRegistry with a custom default RefillRateLimiter configuration and a
     * RateLimiter registry event consumer.
     *
     * @param defaultRateLimiterConfig a custom default RefillRateLimiter configuration.
     * @param registryEventConsumer    a RateLimiter registry event consumer.
     * @return a RefillRateLimiterRegistry with a custom RateLimiter configuration and a RateLimiter
     * registry event consumer.
     */
    static RefillRateLimiterRegistry of(RefillRateLimiterConfig defaultRateLimiterConfig,
                                        RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        return new InMemoryRefillRateLimiterRegistry(defaultRateLimiterConfig, registryEventConsumer);
    }

    /**
     * Creates a RefillRateLimiterRegistry with a custom default RefillRateLimiter configuration and a list of
     * RateLimiter registry event consumers.
     *
     * @param defaultRateLimiterConfig a custom default RefillRateLimiter configuration.
     * @param registryEventConsumers   a list of RateLimiter registry event consumers.
     * @return a RefillRateLimiterRegistry with a custom RateLimiter configuration and a list of
     * RateLimiter registry event consumers.
     */
    static RefillRateLimiterRegistry of(RefillRateLimiterConfig defaultRateLimiterConfig,
                                        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        return new InMemoryRefillRateLimiterRegistry(defaultRateLimiterConfig, registryEventConsumers);
    }

    /**
     * Returns a managed {@link RefillRateLimiterConfig} or creates a new one with a default RateLimiter
     * configuration.
     *
     * @return The {@link RefillRateLimiterConfig}
     */
    static RefillRateLimiterRegistry ofDefaults() {
        return new InMemoryRefillRateLimiterRegistry(RefillRateLimiterConfig.ofDefaults());
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     *
     * @param configs a Map of shared RateLimiter configurations
     * @return a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     */
    static RefillRateLimiterRegistry of(Map<String, RefillRateLimiterConfig> configs) {
        return new InMemoryRefillRateLimiterRegistry(configs);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared RateLimiter configurations
     * @param tags    default tags to add to the registry
     * @return a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     */
    static RefillRateLimiterRegistry of(Map<String, RefillRateLimiterConfig> configs,
                                        io.vavr.collection.Map<String, String> tags) {
        return new InMemoryRefillRateLimiterRegistry(configs, tags);
    }

    /**
     * Creates a RefillRateLimiterRegistry with a Map of shared RefillRateLimiter configurations and a
     * RateLimiter registry event consumer.
     *
     * @param configs               a Map of shared RefillRateLimiter configurations.
     * @param registryEventConsumer a RateLimiter registry event consumer.
     * @return a RefillRateLimiterRegistry with a Map of shared RateLimiter configurations and a
     * RateLimiter registry event consumer.
     */
    static RefillRateLimiterRegistry of(Map<String, RefillRateLimiterConfig> configs,
                                        RegistryEventConsumer<RateLimiter> registryEventConsumer) {
        return new InMemoryRefillRateLimiterRegistry(configs, registryEventConsumer);
    }

    /**
     * Creates a RefillRateLimiterRegistry with a Map of shared RefillRateLimiter configurations and a
     * RateLimiter registry event consumer.
     *
     * @param configs               a Map of shared RefillRateLimiter configurations.
     * @param registryEventConsumer a RateLimiter registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a RefillRateLimiterRegistry with a Map of shared RateLimiter configurations and a
     * RateLimiter registry event consumer.
     */
    static RefillRateLimiterRegistry of(Map<String, RefillRateLimiterConfig> configs,
                                        RegistryEventConsumer<RateLimiter> registryEventConsumer,
                                        io.vavr.collection.Map<String, String> tags) {
        return new InMemoryRefillRateLimiterRegistry(configs, registryEventConsumer, tags);
    }

    /**
     * Creates a RefillRateLimiterRegistry with a Map of shared RefillRateLimiter configurations and a list of
     * RateLimiter registry event consumers.
     *
     * @param configs                a Map of shared RefillRateLimiter configurations.
     * @param registryEventConsumers a list of RateLimiter registry event consumers.
     * @return a RefillRateLimiterRegistry with a Map of shared RateLimiter configurations and a list of
     * RateLimiter registry event consumers.
     */
    static RefillRateLimiterRegistry of(Map<String, RefillRateLimiterConfig> configs,
                                        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
        return new InMemoryRefillRateLimiterRegistry(configs, registryEventConsumers);
    }

    /**
     * Returns all managed {@link RateLimiter} instances.
     *
     * @return all managed {@link RateLimiter} instances.
     */
    Seq<RateLimiter> getAllRateLimiters();

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter
     * configuration.
     *
     * @param name the name of the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the RateLimiter
     * @param tags tags added to the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter
     * configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RefillRateLimiter configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RefillRateLimiter configuration
     * @param tags              tags added to the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom
     * RefillRateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RefillRateLimiterConfig configuration
     * @return The {@link RateLimiterConfig}
     */
    RateLimiter rateLimiter(String name, Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom
     * RefillRateLimiterConfig configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RefillRateLimiterConfig configuration
     * @param tags                      tags added to the RateLimiter
     * @return The {@link RateLimiterConfig}
     */
    RateLimiter rateLimiter(String name, Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the RateLimiter
     * @param configName the name of the shared configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, String configName);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the RateLimiter
     * @param configName the name of the shared configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, String configName,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a builder to create a custom RefillRateLimiterRegistry.
     *
     * @return a {@link RefillRateLimiterRegistry.Builder}
     */
    static RefillRateLimiterRegistry.Builder custom() {
        return new RefillRateLimiterRegistry.Builder();
    }

    class Builder {

        private static final String DEFAULT_CONFIG = "default";
        private RegistryStore registryStore;
        private Map<String, RefillRateLimiterConfig> rateLimiterConfigsMap;
        private List<RegistryEventConsumer<RateLimiter>> registryEventConsumers;
        private io.vavr.collection.Map<String, String> tags;

        public Builder() {
            this.rateLimiterConfigsMap = new java.util.HashMap<>();
            this.registryEventConsumers = new ArrayList<>();
        }

        public RefillRateLimiterRegistry.Builder withRegistryStore(RegistryStore registryStore) {
            this.registryStore = registryStore;
            return this;
        }

        /**
         * Configures a RefillRateLimiterRegistry with a custom default RefillRateLimiterConfig configuration.
         *
         * @param rateLimiterConfig a custom default RefillRateLimiter configuration
         * @return a {@link RefillRateLimiterRegistry.Builder}
         */
        public RefillRateLimiterRegistry.Builder withRateLimiterConfig(RefillRateLimiterConfig rateLimiterConfig) {
            rateLimiterConfigsMap.put(DEFAULT_CONFIG, rateLimiterConfig);
            return this;
        }

        /**
         * Configures a RefillRateLimiterRegistry with a custom RefillRateLimiterConfig configuration.
         *
         * @param configName    configName for a custom shared RateLimiter configuration
         * @param configuration a custom shared RefillRateLimiter configuration
         * @return a {@link RefillRateLimiterRegistry.Builder}
         * @throws IllegalArgumentException if {@code configName.equals("default")}
         */
        public RefillRateLimiterRegistry.Builder addRateLimiterConfig(String configName, RefillRateLimiterConfig configuration) {
            if (configName.equals(DEFAULT_CONFIG)) {
                throw new IllegalArgumentException(
                    "You cannot add another configuration with name 'default' as it is preserved for default configuration");
            }
            rateLimiterConfigsMap.put(configName, configuration);
            return this;
        }

        /**
         * Configures a RefillRateLimiterRegistry with a RateLimiter registry event consumer.
         *
         * @param registryEventConsumer a RateLimiter registry event consumer.
         * @return a {@link RefillRateLimiterRegistry.Builder}
         */
        public RefillRateLimiterRegistry.Builder addRegistryEventConsumer(RegistryEventConsumer<RateLimiter> registryEventConsumer) {
            this.registryEventConsumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Configures a RefillRateLimiterRegistry with Tags.
         * <p>
         * Tags added to the registry will be added to every instance created by this registry.
         *
         * @param tags default tags to add to the registry.
         * @return a {@link RefillRateLimiterRegistry.Builder}
         */
        public RefillRateLimiterRegistry.Builder withTags(io.vavr.collection.Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds a RefillRateLimiterRegistry
         *
         * @return the RefillRateLimiterRegistry
         */
        public RefillRateLimiterRegistry build() {
            return new InMemoryRefillRateLimiterRegistry(rateLimiterConfigsMap, registryEventConsumers, tags,
                registryStore);
        }
    }

}
