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
package io.github.resilience4j.retry;


import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.internal.InMemoryRetryRegistry;

import java.util.*;
import java.util.function.Supplier;

/**
 * The {@link RetryRegistry} is a factory to create Retry instances which stores all Retry instances
 * in a registry.
 */
public interface RetryRegistry extends Registry<Retry, RetryConfig> {

    /**
     * Creates a RetryRegistry with a custom default Retry configuration.
     *
     * @param retryConfig a custom Retry configuration
     * @return a RetryRegistry with a custom Retry configuration.
     */
    static RetryRegistry of(RetryConfig retryConfig) {
        return new InMemoryRetryRegistry(retryConfig);
    }

    /**
     * Creates a RetryRegistry with a custom default Retry configuration and a Retry registry event
     * consumer.
     *
     * @param retryConfig           a custom default Retry configuration.
     * @param registryEventConsumer a Retry registry event consumer.
     * @return a RetryRegistry with a custom Retry configuration and a Retry registry event
     * consumer.
     */
    static RetryRegistry of(RetryConfig retryConfig,
        RegistryEventConsumer<Retry> registryEventConsumer) {
        return new InMemoryRetryRegistry(retryConfig, registryEventConsumer);
    }

    /**
     * Creates a RetryRegistry with a custom default Retry configuration and a list of Retry
     * registry event consumers.
     *
     * @param retryConfig            a custom default Retry configuration.
     * @param registryEventConsumers a list of Retry registry event consumers.
     * @return a RetryRegistry with a custom Retry configuration and list of Retry registry event
     * consumers.
     */
    static RetryRegistry of(RetryConfig retryConfig,
        List<RegistryEventConsumer<Retry>> registryEventConsumers) {
        return new InMemoryRetryRegistry(retryConfig, registryEventConsumers);
    }

    /**
     * Creates a RetryRegistry with a default Retry configuration.
     *
     * @return a RetryRegistry with a default Retry configuration.
     */
    static RetryRegistry ofDefaults() {
        return new InMemoryRetryRegistry();
    }

    /**
     * Creates a RetryRegistry with a Map of shared Retry configurations.
     *
     * @param configs a Map of shared Retry configurations
     * @return a RetryRegistry with a Map of shared Retry configurations.
     */
    static RetryRegistry of(Map<String, RetryConfig> configs) {
        return of(configs, Collections.emptyMap());
    }

    /**
     * Creates a RetryRegistry with a Map of shared Retry configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared Retry configurations
     * @param tags    default tags to add to the registry
     * @return a RetryRegistry with a Map of shared Retry configurations.
     */
    static RetryRegistry of(Map<String, RetryConfig> configs,
        Map<String, String> tags) {
        return new InMemoryRetryRegistry(configs, tags);
    }

    /**
     * Creates a RetryRegistry with a Map of shared Retry configurations and a Retry registry event
     * consumer.
     *
     * @param configs               a Map of shared Retry configurations.
     * @param registryEventConsumer a Retry registry event consumer.
     * @return a RetryRegistry with a Map of shared Retry configurations and a Retry registry event
     * consumer.
     */
    static RetryRegistry of(Map<String, RetryConfig> configs,
        RegistryEventConsumer<Retry> registryEventConsumer) {
        return new InMemoryRetryRegistry(configs, registryEventConsumer);
    }

    /**
     * Creates a RetryRegistry with a Map of shared Retry configurations and a Retry registry event
     * consumer.
     *
     * @param configs               a Map of shared Retry configurations.
     * @param registryEventConsumer a Retry registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a RetryRegistry with a Map of shared Retry configurations and a Retry registry event
     * consumer.
     */
    static RetryRegistry of(Map<String, RetryConfig> configs,
        RegistryEventConsumer<Retry> registryEventConsumer,
        Map<String, String> tags) {
        return new InMemoryRetryRegistry(configs, registryEventConsumer, tags);
    }

    /**
     * Creates a RetryRegistry with a Map of shared Retry configurations and a list of Retry
     * registry event consumers.
     *
     * @param configs                a Map of shared Retry configurations.
     * @param registryEventConsumers a list of Retry registry event consumers.
     * @return a RetryRegistry with a Map of shared Retry configurations and a list of Retry
     * registry event consumers.
     */
    static RetryRegistry of(Map<String, RetryConfig> configs,
        List<RegistryEventConsumer<Retry>> registryEventConsumers) {
        return new InMemoryRetryRegistry(configs, registryEventConsumers);
    }

    /**
     * Returns all managed {@link Retry} instances.
     *
     * @return all managed {@link Retry} instances.
     */
    Set<Retry> getAllRetries();

    /**
     * Returns a managed {@link Retry} or creates a new one with the default Retry configuration.
     *
     * @param name the name of the Retry
     * @return The {@link Retry}
     */
    Retry retry(String name);

    /**
     * Returns a managed {@link Retry} or creates a new one with the default Retry configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the Retry
     * @param tags tags added to the Retry
     * @return The {@link Retry}
     */
    Retry retry(String name, Map<String, String> tags);

    /**
     * Returns a managed {@link Retry} or creates a new one with a custom Retry configuration.
     *
     * @param name   the name of the Retry
     * @param config a custom Retry configuration
     * @return The {@link Retry}
     */
    Retry retry(String name, RetryConfig config);

    /**
     * Returns a managed {@link Retry} or creates a new one with a custom Retry configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name   the name of the Retry
     * @param config a custom Retry configuration
     * @param tags   tags added to the Retry
     * @return The {@link Retry}
     */
    Retry retry(String name, RetryConfig config, Map<String, String> tags);

    /**
     * Returns a managed {@link Retry} or creates a new one with a custom Retry configuration.
     *
     * @param name                the name of the Retry
     * @param retryConfigSupplier a supplier of a custom Retry configuration
     * @return The {@link Retry}
     */
    Retry retry(String name, Supplier<RetryConfig> retryConfigSupplier);

    /**
     * Returns a managed {@link Retry} or creates a new one with a custom Retry configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                the name of the Retry
     * @param retryConfigSupplier a supplier of a custom Retry configuration
     * @param tags                tags added to the Retry
     * @return The {@link Retry}
     */
    Retry retry(String name, Supplier<RetryConfig> retryConfigSupplier,
        Map<String, String> tags);

    /**
     * Returns a managed {@link Retry} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the Retry
     * @param configName the name of the shared configuration
     * @return The {@link Retry}
     */
    Retry retry(String name, String configName);

    /**
     * Returns a managed {@link Retry} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the Retry
     * @param configName the name of the shared configuration
     * @param tags       tags added to the Retry
     * @return The {@link Retry}
     */
    Retry retry(String name, String configName, Map<String, String> tags);

    /**
     * Returns a builder to create a custom RetryRegistry.
     *
     * @return a {@link RetryRegistry.Builder}
     */
    static Builder custom() {
        return new Builder();
    }

    class Builder {

        private static final String DEFAULT_CONFIG = "default";
        private RegistryStore<Retry> registryStore;
        private Map<String, RetryConfig> retryConfigsMap;
        private List<RegistryEventConsumer<Retry>> registryEventConsumers;
        private Map<String, String> tags;

        public Builder() {
            this.retryConfigsMap = new java.util.HashMap<>();
            this.registryEventConsumers = new ArrayList<>();
        }

        public Builder withRegistryStore(RegistryStore<Retry> registryStore) {
            this.registryStore = registryStore;
            return this;
        }

        /**
         * Configures a RetryRegistry with a custom default Retry configuration.
         *
         * @param retryConfig a custom default Retry configuration
         * @return a {@link RetryRegistry.Builder}
         */
        public Builder withRetryConfig(RetryConfig retryConfig) {
            retryConfigsMap.put(DEFAULT_CONFIG, retryConfig);
            return this;
        }

        /**
         * Configures a RetryRegistry with a custom Retry configuration.
         *
         * @param configName configName for a custom shared Retry configuration
         * @param configuration a custom shared Retry configuration
         * @return a {@link RetryRegistry.Builder}
         * @throws IllegalArgumentException if {@code configName.equals("default")}
         */
        public Builder addRetryConfig(String configName, RetryConfig configuration) {
            if (configName.equals(DEFAULT_CONFIG)) {
                throw new IllegalArgumentException(
                    "You cannot add another configuration with name 'default' as it is preserved for default configuration");
            }
            retryConfigsMap.put(configName, configuration);
            return this;
        }

        /**
         * Configures a RetryRegistry with a Retry registry event consumer.
         *
         * @param registryEventConsumer a Retry registry event consumer.
         * @return a {@link RetryRegistry.Builder}
         */
        public Builder addRegistryEventConsumer(RegistryEventConsumer<Retry> registryEventConsumer) {
            this.registryEventConsumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Configures a RetryRegistry with Tags.
         * <p>
         * Tags added to the registry will be added to every instance created by this registry.
         *
         * @param tags default tags to add to the registry.
         * @return a {@link RetryRegistry.Builder}
         */
        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds a RetryRegistry
         *
         * @return the RetryRegistry
         */
        public RetryRegistry build() {
            return new InMemoryRetryRegistry(retryConfigsMap, registryEventConsumers, tags, registryStore);
        }
    }

}
