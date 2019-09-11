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
package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Backend circuitBreaker manager.
 * Constructs backend circuitBreakers according to configuration values.
 */
public final class InMemoryCircuitBreakerRegistry extends AbstractRegistry<CircuitBreaker, CircuitBreakerConfig> implements CircuitBreakerRegistry {

	/**
	 * The constructor with default default.
	 */
	public InMemoryCircuitBreakerRegistry() {
		this(CircuitBreakerConfig.ofDefaults());
	}

	public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs) {
		this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()));
		this.configurations.putAll(configs);
	}

	public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs, RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
		this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()), registryEventConsumer);
		this.configurations.putAll(configs);
	}

	public InMemoryCircuitBreakerRegistry(Map<String, CircuitBreakerConfig> configs, List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
		this(configs.getOrDefault(DEFAULT_CONFIG, CircuitBreakerConfig.ofDefaults()), registryEventConsumers);
		this.configurations.putAll(configs);
	}

	/**
	 * The constructor with custom default config.
	 *
	 * @param defaultConfig The default config.
	 */
	public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig) {
		super(defaultConfig);
	}

	public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig, RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
		super(defaultConfig, registryEventConsumer);
	}

	public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultConfig, List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
		super(defaultConfig, registryEventConsumers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Seq<CircuitBreaker> getAllCircuitBreakers() {
		return Array.ofAll(entryMap.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CircuitBreaker circuitBreaker(String name) {
		return circuitBreaker(name, getDefaultConfig());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config) {
		return computeIfAbsent(name, () -> CircuitBreaker.of(name, Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CircuitBreaker circuitBreaker(String name, String configName) {
		return computeIfAbsent(name, () -> CircuitBreaker.of(name, getConfiguration(configName)
						.orElseThrow(() -> new ConfigurationNotFoundException(configName))));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CircuitBreaker circuitBreaker(String name, Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
		return computeIfAbsent(name, () -> CircuitBreaker.of(name, Objects.requireNonNull(Objects.requireNonNull(circuitBreakerConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(), CONFIG_MUST_NOT_BE_NULL)));
	}


}
