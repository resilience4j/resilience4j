/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;


import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link CircuitBreakerRegistry} is a factory to create CircuitBreaker instances which stores all CircuitBreaker instances in a registry.
 */
public interface CircuitBreakerRegistry extends Registry<CircuitBreaker, CircuitBreakerConfig> {
	/**
	 * Returns all managed {@link CircuitBreaker} instances.
	 *
	 * @return all managed {@link CircuitBreaker} instances.
	 */
	Seq<CircuitBreaker> getAllCircuitBreakers();

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with the default CircuitBreaker configuration.
	 *
	 * @param name the name of the CircuitBreaker
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name);

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with the default CircuitBreaker configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name the name of the CircuitBreaker
	 * @param tags tags added to the CircuitBreaker
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name, io.vavr.collection.Map<String, String> tags);

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker configuration.
	 *
	 * @param name                 the name of the CircuitBreaker
	 * @param config a custom CircuitBreaker configuration
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config);

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name                 the name of the CircuitBreaker
	 * @param config a custom CircuitBreaker configuration
	 * @param tags tags added to the CircuitBreaker
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config, io.vavr.collection.Map<String, String> tags);

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker configuration.
	 *
	 * @param name       the name of the CircuitBreaker
	 * @param configName a custom CircuitBreaker configuration name
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name, String configName);

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name       the name of the CircuitBreaker
	 * @param configName a custom CircuitBreaker configuration name
	 * @param tags tags added to the CircuitBreaker
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name, String configName, io.vavr.collection.Map<String, String> tags);

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker configuration.
	 *
	 * @param name                         the name of the CircuitBreaker
	 * @param circuitBreakerConfigSupplier a supplier of a custom CircuitBreaker configuration
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name, Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier);

	/**
	 * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker configuration.
	 *
	 * The {@code tags} passed will be appended to the tags already configured for the registry. When tags (keys) of
	 * the two collide the tags passed with this method will override the tags of the registry.
	 *
	 * @param name                         the name of the CircuitBreaker
	 * @param circuitBreakerConfigSupplier a supplier of a custom CircuitBreaker configuration
	 * @param tags                         tags added to the CircuitBreaker
	 * @return The {@link CircuitBreaker}
	 */
	CircuitBreaker circuitBreaker(String name, Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier, io.vavr.collection.Map<String, String> tags);

	/**
	 * Creates a CircuitBreakerRegistry with a custom default CircuitBreaker configuration.
	 *
	 * @param circuitBreakerConfig a custom default CircuitBreaker configuration
	 * @return a CircuitBreakerRegistry with a custom CircuitBreaker configuration.
	 */
	static CircuitBreakerRegistry of(CircuitBreakerConfig circuitBreakerConfig) {
		return new InMemoryCircuitBreakerRegistry(circuitBreakerConfig);
	}

	/**
	 * Creates a CircuitBreakerRegistry with a custom default CircuitBreaker configuration and a CircuitBreaker registry event consumer.
	 *
	 * @param circuitBreakerConfig a custom default CircuitBreaker configuration.
	 * @param registryEventConsumer a CircuitBreaker registry event consumer.
	 * @return a CircuitBreakerRegistry with a custom CircuitBreaker configuration and a CircuitBreaker registry event consumer.
	 */
	static CircuitBreakerRegistry of(CircuitBreakerConfig circuitBreakerConfig, RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
		return new InMemoryCircuitBreakerRegistry(circuitBreakerConfig, registryEventConsumer);
	}

	/**
	 * Creates a CircuitBreakerRegistry with a custom default CircuitBreaker configuration and a list of CircuitBreaker registry event consumers.
	 *
	 * @param circuitBreakerConfig a custom default CircuitBreaker configuration.
	 * @param registryEventConsumers a list of CircuitBreaker registry event consumers.
	 * @return a CircuitBreakerRegistry with a custom CircuitBreaker configuration and list of CircuitBreaker registry event consumers.
	 */
	static CircuitBreakerRegistry of(CircuitBreakerConfig circuitBreakerConfig, List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
		return new InMemoryCircuitBreakerRegistry(circuitBreakerConfig, registryEventConsumers);
	}

	/**
	 * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
	 *
	 * @param configs a Map of shared CircuitBreaker configurations
	 * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
	 */
	static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs) {
		return of(configs, HashMap.empty());
	}

	/**
	 * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
	 *
	 * Tags added to the registry will be added to every instance created by this registry.
	 *
	 * @param configs a Map of shared CircuitBreaker configurations
	 * @param tags default tags to add to the registry
	 * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
	 */
	static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs, io.vavr.collection.Map<String, String> tags) {
		return new InMemoryCircuitBreakerRegistry(configs, tags);
	}

	/**
	 * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a CircuitBreaker registry event consumer.
	 *
	 * @param configs a Map of shared CircuitBreaker configurations.
	 * @param registryEventConsumer a CircuitBreaker registry event consumer.
	 * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a CircuitBreaker registry event consumer.
	 */
	static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs, RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
		return new InMemoryCircuitBreakerRegistry(configs, registryEventConsumer);
	}

	/**
	 * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a list of CircuitBreaker registry event consumers.
	 *
	 * @param configs a Map of shared CircuitBreaker configurations.
	 * @param registryEventConsumers a list of CircuitBreaker registry event consumers.
	 * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a list of CircuitBreaker registry event consumers.
	 */
	static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs, List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
		return new InMemoryCircuitBreakerRegistry(configs, registryEventConsumers);
	}

	/**
	 * Creates a CircuitBreakerRegistry with a default CircuitBreaker configuration.
	 *
	 * @return a CircuitBreakerRegistry with a default CircuitBreaker configuration.
	 */
	static CircuitBreakerRegistry ofDefaults() {
		return new InMemoryCircuitBreakerRegistry();
	}

}
