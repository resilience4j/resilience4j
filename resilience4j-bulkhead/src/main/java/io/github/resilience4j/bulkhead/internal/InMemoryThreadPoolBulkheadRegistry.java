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
package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
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
 * Thread pool Bulkhead instance manager;
 * Constructs/returns thread pool bulkhead instances.
 */
public final class InMemoryThreadPoolBulkheadRegistry extends AbstractRegistry<ThreadPoolBulkhead, ThreadPoolBulkheadConfig> implements ThreadPoolBulkheadRegistry {

	/**
	 * The constructor with default default.
	 */
	public InMemoryThreadPoolBulkheadRegistry() {
		this(ThreadPoolBulkheadConfig.ofDefaults());
	}

	public InMemoryThreadPoolBulkheadRegistry(Map<String, ThreadPoolBulkheadConfig> configs) {
		this(configs.getOrDefault(DEFAULT_CONFIG, ThreadPoolBulkheadConfig.ofDefaults()));
		this.configurations.putAll(configs);
	}

	public InMemoryThreadPoolBulkheadRegistry(
			Map<String, ThreadPoolBulkheadConfig> configs, RegistryEventConsumer<ThreadPoolBulkhead> registryEventConsumer) {
		this(configs.getOrDefault(DEFAULT_CONFIG, ThreadPoolBulkheadConfig.ofDefaults()), registryEventConsumer);
		this.configurations.putAll(configs);
	}

	public InMemoryThreadPoolBulkheadRegistry(
			Map<String, ThreadPoolBulkheadConfig> configs, List<RegistryEventConsumer<ThreadPoolBulkhead>> registryEventConsumers) {
		this(configs.getOrDefault(DEFAULT_CONFIG, ThreadPoolBulkheadConfig.ofDefaults()), registryEventConsumers);
		this.configurations.putAll(configs);
	}

	/**
	 * The constructor with custom default config.
	 *
	 * @param defaultConfig The default config.
	 */
	public InMemoryThreadPoolBulkheadRegistry(ThreadPoolBulkheadConfig defaultConfig) {
		super(defaultConfig);
	}

	public InMemoryThreadPoolBulkheadRegistry(
			ThreadPoolBulkheadConfig defaultConfig, RegistryEventConsumer<ThreadPoolBulkhead> registryEventConsumer) {
		super(defaultConfig, registryEventConsumer);
	}

	public InMemoryThreadPoolBulkheadRegistry(
			ThreadPoolBulkheadConfig defaultConfig, List<RegistryEventConsumer<ThreadPoolBulkhead>> registryEventConsumers) {
		super(defaultConfig, registryEventConsumers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Seq<ThreadPoolBulkhead> getAllBulkheads() {
		return Array.ofAll(entryMap.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThreadPoolBulkhead bulkhead(String name) {
		return bulkhead(name, getDefaultConfig());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThreadPoolBulkhead bulkhead(String name, ThreadPoolBulkheadConfig config) {
		return computeIfAbsent(name, () -> ThreadPoolBulkhead.of(name, Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThreadPoolBulkhead bulkhead(String name, Supplier<ThreadPoolBulkheadConfig> bulkheadConfigSupplier) {
		return computeIfAbsent(name, () -> ThreadPoolBulkhead.of(name, Objects.requireNonNull(Objects.requireNonNull(bulkheadConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(), CONFIG_MUST_NOT_BE_NULL)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThreadPoolBulkhead bulkhead(String name, String configName) {
		return computeIfAbsent(name, () -> ThreadPoolBulkhead.of(name, getConfiguration(configName)
				.orElseThrow(() -> new ConfigurationNotFoundException(configName))));
	}
}
