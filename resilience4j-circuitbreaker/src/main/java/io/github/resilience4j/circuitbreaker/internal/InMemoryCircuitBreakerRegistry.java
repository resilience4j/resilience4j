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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

/**
 * Backend circuitBreaker manager.
 * Constructs backend circuitBreakers according to configuration values.
 */
public final class InMemoryCircuitBreakerRegistry implements CircuitBreakerRegistry {

	private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
	private static final String DEFAULT_CONFIG = "default";
	private final CircuitBreakerConfig defaultCircuitBreakerConfig;

	/**
	 * The circuitBreakers, indexed by name of the backend.
	 */
	private final ConcurrentMap<String, CircuitBreaker> circuitBreakers;

	/**
	 * The list of consumer functions to execute after a circuit breaker is created.
	 */
	private final List<Consumer<CircuitBreaker>> postCreationConsumers;

	/**
	 * The map of shared circuit breaker configuration by name
	 */
	private final ConcurrentMap<String, CircuitBreakerConfig> sharedCircuitBreakerConfiguration;


	/**
	 * The constructor with default circuitBreaker properties.
	 */
	public InMemoryCircuitBreakerRegistry() {
		this.defaultCircuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
		this.circuitBreakers = new ConcurrentHashMap<>();
		this.postCreationConsumers = new CopyOnWriteArrayList<>();
		this.sharedCircuitBreakerConfiguration = new ConcurrentHashMap<>();
		this.sharedCircuitBreakerConfiguration.put(DEFAULT_CONFIG, defaultCircuitBreakerConfig);
	}

	/**
	 * The constructor with custom default circuitBreaker properties.
	 *
	 * @param defaultCircuitBreakerConfig The BackendMonitor service properties.
	 */
	public InMemoryCircuitBreakerRegistry(CircuitBreakerConfig defaultCircuitBreakerConfig) {
		this.defaultCircuitBreakerConfig = Objects.requireNonNull(defaultCircuitBreakerConfig, "CircuitBreakerConfig must not be null");
		this.circuitBreakers = new ConcurrentHashMap<>();
		this.postCreationConsumers = new ArrayList<>();
		this.sharedCircuitBreakerConfiguration = new ConcurrentHashMap<>();
		this.sharedCircuitBreakerConfiguration.put(DEFAULT_CONFIG, defaultCircuitBreakerConfig);
	}

	@Override
	public void addCircuitBreakerConfig(String configName, CircuitBreakerConfig circuitBreakerConfig) {
		this.sharedCircuitBreakerConfiguration.put(configName, circuitBreakerConfig);
	}

	@Override
	public CircuitBreakerConfig getCircuitBreakerConfigByName(String configName) {
		return Optional.ofNullable(this.sharedCircuitBreakerConfiguration.get(configName)).
				orElseThrow(() -> new IllegalArgumentException("The circuit breaker configuration is not found for this name "));
	}

	@Override
	public Seq<CircuitBreaker> getAllCircuitBreakers() {
		return Array.ofAll(circuitBreakers.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CircuitBreaker circuitBreaker(String name) {
		return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL),
				k -> postCreateCircuitBreaker(CircuitBreaker.of(name, defaultCircuitBreakerConfig)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig customCircuitBreakerConfig) {
		return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL),
				k -> postCreateCircuitBreaker(CircuitBreaker.of(name, customCircuitBreakerConfig)));
	}

	@Override
	public CircuitBreaker circuitBreaker(String name, Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
		return circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL),
				k -> {
					CircuitBreakerConfig config = circuitBreakerConfigSupplier.get();
					return postCreateCircuitBreaker(CircuitBreaker.of(name, config));
				});
	}

	@Override
	public void registerPostCreationConsumer(Consumer<CircuitBreaker> postCreationConsumer) {
		postCreationConsumers.add(postCreationConsumer);
	}

	@Override
	public void unregisterPostCreationConsumer(Consumer<CircuitBreaker> postCreationConsumer) {
		postCreationConsumers.remove(postCreationConsumer);
	}

	private CircuitBreaker postCreateCircuitBreaker(CircuitBreaker createdCircuitBreaker) {
		if (!postCreationConsumers.isEmpty()) {
			postCreationConsumers.forEach(consumer -> consumer.accept(createdCircuitBreaker));
		}
		return createdCircuitBreaker;
	}

}
