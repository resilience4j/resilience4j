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
package io.github.resilience4j.timelimiter;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.internal.InMemoryTimeLimiterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Manages all TimeLimiter instances.
 */
public interface TimeLimiterRegistry extends Registry<TimeLimiter, TimeLimiterConfig> {

    /**
     * Creates a TimeLimiterRegistry with a custom default TimeLimiter configuration.
     *
     * @param defaultTimeLimiterConfig a custom default TimeLimiter configuration
     * @return a TimeLimiterRegistry instance backed by a custom TimeLimiter configuration
     */
    static TimeLimiterRegistry of(TimeLimiterConfig defaultTimeLimiterConfig) {
        return new InMemoryTimeLimiterRegistry(defaultTimeLimiterConfig);
    }

    /**
     * Creates a TimeLimiterRegistry with a custom default TimeLimiter configuration and a
     * TimeLimiter registry event consumer.
     *
     * @param defaultTimeLimiterConfig a custom default TimeLimiter configuration.
     * @param registryEventConsumer    a TimeLimiter registry event consumer.
     * @return a TimeLimiterRegistry with a custom TimeLimiter configuration and a TimeLimiter
     * registry event consumer.
     */
    static TimeLimiterRegistry of(TimeLimiterConfig defaultTimeLimiterConfig,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer) {
        return new InMemoryTimeLimiterRegistry(defaultTimeLimiterConfig, registryEventConsumer);
    }

    /**
     * Creates a TimeLimiterRegistry with a custom default TimeLimiter configuration and a list of
     * TimeLimiter registry event consumers.
     *
     * @param defaultTimeLimiterConfig a custom default TimeLimiter configuration.
     * @param registryEventConsumers   a list of TimeLimiter registry event consumers.
     * @return a TimeLimiterRegistry with a custom TimeLimiter configuration and list of TimeLimiter
     * registry event consumers.
     */
    static TimeLimiterRegistry of(TimeLimiterConfig defaultTimeLimiterConfig,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers) {
        return new InMemoryTimeLimiterRegistry(defaultTimeLimiterConfig, registryEventConsumers);
    }

    /**
     * Returns a managed {@link TimeLimiterConfig} or creates a new one with a default TimeLimiter
     * configuration.
     *
     * @return The {@link TimeLimiterConfig}
     */
    static TimeLimiterRegistry ofDefaults() {
        return new InMemoryTimeLimiterRegistry(TimeLimiterConfig.ofDefaults());
    }

    /**
     * Creates a TimeLimiterRegistry with a Map of shared TimeLimiter configurations.
     *
     * @param configs a Map of shared TimeLimiter configurations
     * @return a TimeLimiterRegistry with a Map of shared TimeLimiter configurations.
     */
    static TimeLimiterRegistry of(Map<String, TimeLimiterConfig> configs) {
        return new InMemoryTimeLimiterRegistry(configs);
    }

    /**
     * Creates a TimeLimiterRegistry with a Map of shared TimeLimiter configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared TimeLimiter configurations
     * @param tags    default tags to add to the registry
     * @return a TimeLimiterRegistry with a Map of shared TimeLimiter configurations.
     */
    static TimeLimiterRegistry of(Map<String, TimeLimiterConfig> configs, Map<String, String> tags) {
        return new InMemoryTimeLimiterRegistry(configs, tags);
    }

    /**
     * Creates a TimeLimiterRegistry with a Map of shared TimeLimiter configurations and a
     * TimeLimiter registry event consumer.
     *
     * @param configs               a Map of shared TimeLimiter configurations.
     * @param registryEventConsumer a TimeLimiter registry event consumer.
     * @return a TimeLimiterRegistry with a Map of shared TimeLimiter configurations and a
     * TimeLimiter registry event consumer.
     */
    static TimeLimiterRegistry of(Map<String, TimeLimiterConfig> configs,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer) {
        return new InMemoryTimeLimiterRegistry(configs, registryEventConsumer);
    }

    /**
     * Creates a TimeLimiterRegistry with a Map of shared TimeLimiter configurations and a
     * TimeLimiter registry event consumer.
     *
     * @param configs               a Map of shared TimeLimiter configurations.
     * @param registryEventConsumer a TimeLimiter registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a TimeLimiterRegistry with a Map of shared TimeLimiter configurations and a
     * TimeLimiter registry event consumer.
     */
    static TimeLimiterRegistry of(Map<String, TimeLimiterConfig> configs,
        RegistryEventConsumer<TimeLimiter> registryEventConsumer, Map<String, String> tags) {
        return new InMemoryTimeLimiterRegistry(configs, registryEventConsumer, tags);
    }

    /**
     * Creates a TimeLimiterRegistry with a Map of shared TimeLimiter configurations and a list of
     * TimeLimiter registry event consumers.
     *
     * @param configs                a Map of shared TimeLimiter configurations.
     * @param registryEventConsumers a list of TimeLimiter registry event consumers.
     * @return a TimeLimiterRegistry with a Map of shared TimeLimiter configurations and a list of
     * TimeLimiter registry event consumers.
     */
    static TimeLimiterRegistry of(Map<String, TimeLimiterConfig> configs,
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers) {
        return new InMemoryTimeLimiterRegistry(configs, registryEventConsumers);
    }

    /**
     * Returns all managed {@link TimeLimiter} instances.
     *
     * @return all managed {@link TimeLimiter} instances.
     */
    Set<TimeLimiter> getAllTimeLimiters();

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with the default TimeLimiter
     * configuration.
     *
     * @param name the name of the TimeLimiter
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with the default TimeLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the TimeLimiter
     * @param tags tags added to the TimeLimiter
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name, Map<String, String> tags);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with a custom TimeLimiter
     * configuration.
     *
     * @param name              the name of the TimeLimiter
     * @param timeLimiterConfig a custom TimeLimiter configuration
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name, TimeLimiterConfig timeLimiterConfig);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with a custom TimeLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name              the name of the TimeLimiter
     * @param timeLimiterConfig a custom TimeLimiter configuration
     * @param tags              tags added to the TimeLimiter
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name, TimeLimiterConfig timeLimiterConfig, Map<String, String> tags);

    /**
     * Returns a managed {@link TimeLimiterConfig} or creates a new one with a custom
     * TimeLimiterConfig configuration.
     *
     * @param name                      the name of the TimeLimiterConfig
     * @param timeLimiterConfigSupplier a supplier of a custom TimeLimiterConfig configuration
     * @return The {@link TimeLimiterConfig}
     */
    TimeLimiter timeLimiter(String name, Supplier<TimeLimiterConfig> timeLimiterConfigSupplier);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with a custom TimeLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                      the name of the TimeLimiter
     * @param timeLimiterConfigSupplier a supplier of a custom TimeLimiter configuration
     * @param tags                      tags added to the TimeLimiter
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name,
        Supplier<TimeLimiterConfig> timeLimiterConfigSupplier, Map<String, String> tags);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the TimeLimiter
     * @param configName the name of the shared configuration
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name, String configName);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the TimeLimiter
     * @param configName the name of the shared configuration
     * @param tags       tags added to the TimeLimiter
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name, String configName, Map<String, String> tags);

    /**
     * Returns a builder to create a custom TimeLimiterRegistry.
     *
     * @return a {@link TimeLimiterRegistry.Builder}
     */
    static Builder custom() {
        return new Builder();
    }

    class Builder {

        private static final String DEFAULT_CONFIG = "default";
        private RegistryStore<TimeLimiter> registryStore;
        private final Map<String, TimeLimiterConfig> timeLimiterConfigsMap;
        private final List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers;
        private Map<String, String> tags;

        public Builder() {
            this.timeLimiterConfigsMap = new java.util.HashMap<>();
            this.registryEventConsumers = new ArrayList<>();
        }

        public Builder withRegistryStore(RegistryStore<TimeLimiter> registryStore) {
            this.registryStore = registryStore;
            return this;
        }

        /**
         * Configures a TimeLimiterRegistry with a custom default TimeLimiter configuration.
         *
         * @param timeLimiterConfig a custom default TimeLimiter configuration
         * @return a {@link TimeLimiterRegistry.Builder}
         */
        public Builder withTimeLimiterConfig(TimeLimiterConfig timeLimiterConfig) {
            timeLimiterConfigsMap.put(DEFAULT_CONFIG, timeLimiterConfig);
            return this;
        }

        /**
         * Configures a TimeLimiterRegistry with a custom TimeLimiter configuration.
         *
         * @param configName configName for a custom shared TimeLimiter configuration
         * @param configuration a custom shared TimeLimiter configuration
         * @return a {@link TimeLimiterRegistry.Builder}
         * @throws IllegalArgumentException if {@code configName.equals("default")}
         */
        public Builder addTimeLimiterConfig(String configName, TimeLimiterConfig configuration) {
            if (configName.equals(DEFAULT_CONFIG)) {
                throw new IllegalArgumentException(
                        "You cannot add another configuration with name 'default' as it is preserved for default configuration");
            }
            timeLimiterConfigsMap.put(configName, configuration);
            return this;
        }

        /**
         * Configures a TimeLimiterRegistry with a TimeLimiter registry event consumer.
         *
         * @param registryEventConsumer a TimeLimiter registry event consumer.
         * @return a {@link TimeLimiterRegistry.Builder}
         */
        public Builder addRegistryEventConsumer(RegistryEventConsumer<TimeLimiter> registryEventConsumer) {
            this.registryEventConsumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Configures a TimeLimiterRegistry with Tags.
         * <p>
         * Tags added to the registry will be added to every instance created by this registry.
         *
         * @param tags default tags to add to the registry.
         * @return a {@link TimeLimiterRegistry.Builder}
         */
        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds a TimeLimiterRegistry
         *
         * @return the TimeLimiterRegistry
         */
        public TimeLimiterRegistry build() {
            return new InMemoryTimeLimiterRegistry(timeLimiterConfigsMap, registryEventConsumers, tags,
                    registryStore);
        }
    }
}
