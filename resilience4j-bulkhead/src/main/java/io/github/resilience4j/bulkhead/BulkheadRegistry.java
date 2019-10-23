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
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link BulkheadRegistry} is a factory to create Bulkhead instances which stores all bulkhead instances in a registry.
 */
public interface BulkheadRegistry extends Registry<Bulkhead, BulkheadConfig> {

	/**
	 * Returns all managed {@link Bulkhead} instances.
	 *
	 * @return all managed {@link Bulkhead} instances.
	 */
	Seq<Bulkhead> getAllBulkheads();

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with default configuration.
	 *
	 * @param name the name of the Bulkhead
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with default configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name the name of the Bulkhead
	 * @param tags tags to add to the bulkhead
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, io.vavr.collection.Map<String, String> tags);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom BulkheadConfig configuration.
	 *
	 * @param name           the name of the Bulkhead
	 * @param config a custom Bulkhead configuration
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, BulkheadConfig config);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom BulkheadConfig configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name   the name of the Bulkhead
	 * @param config a custom Bulkhead configuration
	 * @param tags   tags added to the bulkhead
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, BulkheadConfig config, io.vavr.collection.Map<String, String> tags);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead configuration.
	 *
	 * @param name                   the name of the Bulkhead
	 * @param bulkheadConfigSupplier a custom Bulkhead configuration supplier
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name                   the name of the Bulkhead
	 * @param bulkheadConfigSupplier a custom Bulkhead configuration supplier
	 * @param tags                   tags to add to the Bulkhead
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier, io.vavr.collection.Map<String, String> tags);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead configuration.
	 *
	 * @param name       the name of the Bulkhead
	 * @param configName a custom Bulkhead configuration name
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, String configName);

	/**
	 * Returns a managed {@link Bulkhead} or creates a new one with a custom Bulkhead configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name       the name of the Bulkhead
	 * @param configName a custom Bulkhead configuration name
	 * @param tags       tags to add to the Bulkhead
	 * @return The {@link Bulkhead}
	 */
	Bulkhead bulkhead(String name, String configName, io.vavr.collection.Map<String, String> tags);

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
	 *
	 * Tags added to the registry will be added to every instance created by this registry.
	 *
	 * @param bulkheadConfig a custom Bulkhead configuration
	 * @param tags           default tags to add to the registry
	 * @return a BulkheadRegistry instance backed by a custom Bulkhead configuration
	 */
	static BulkheadRegistry of(BulkheadConfig bulkheadConfig, io.vavr.collection.Map<String, String> tags) {
		return new InMemoryBulkheadRegistry(bulkheadConfig, tags);
	}

	/**
	 * Creates a BulkheadRegistry with a custom default Bulkhead configuration and a Bulkhead registry event consumer.
	 *
	 * @param bulkheadConfig a custom default Bulkhead configuration.
	 * @param registryEventConsumer a Bulkhead registry event consumer.
	 * @return a BulkheadRegistry with a custom Bulkhead configuration and a Bulkhead registry event consumer.
	 */
	static BulkheadRegistry of(BulkheadConfig bulkheadConfig, RegistryEventConsumer<Bulkhead> registryEventConsumer) {
		return new InMemoryBulkheadRegistry(bulkheadConfig, registryEventConsumer);
	}

	/**
	 * Creates a BulkheadRegistry with a custom default Bulkhead configuration and a list of Bulkhead registry event consumers.
	 *
	 * @param bulkheadConfig a custom default Bulkhead configuration.
	 * @param registryEventConsumers a list of Bulkhead registry event consumers.
	 * @return a BulkheadRegistry with a custom Bulkhead configuration and a list of Bulkhead registry event consumers.
	 */
	static BulkheadRegistry of(BulkheadConfig bulkheadConfig, List<RegistryEventConsumer<Bulkhead>> registryEventConsumers) {
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
	 *
	 * Tags added to the registry will be added to every instance created by this registry.
	 *
	 * @param configs a Map of shared Bulkhead configurations
	 * @param tags    default tags to add to the registry
	 * @return a RetryRegistry with a Map of shared Bulkhead configurations.
	 */
	static BulkheadRegistry of(Map<String, BulkheadConfig> configs, io.vavr.collection.Map<String, String> tags) {
		return new InMemoryBulkheadRegistry(configs, tags);
	}

	/**
	 * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations and a Bulkhead registry event consumer.
	 *
	 * @param configs a Map of shared Bulkhead configurations.
	 * @param registryEventConsumer a Bulkhead registry event consumer.
	 * @return a BulkheadRegistry with a Map of shared Bulkhead configurations and a Bulkhead registry event consumer.
	 */
	static BulkheadRegistry of(Map<String, BulkheadConfig> configs, RegistryEventConsumer<Bulkhead> registryEventConsumer) {
		return new InMemoryBulkheadRegistry(configs, registryEventConsumer);
	}

	/**
	 * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations and a list of Bulkhead registry event consumers.
	 *
	 * @param configs a Map of shared Bulkhead configurations.
	 * @param registryEventConsumers a list of Bulkhead registry event consumers.
	 * @return a BulkheadRegistry with a Map of shared Bulkhead configurations and a list of Bulkhead registry event consumers.
	 */
	static BulkheadRegistry of(Map<String, BulkheadConfig> configs, List<RegistryEventConsumer<Bulkhead>> registryEventConsumers) {
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

}
