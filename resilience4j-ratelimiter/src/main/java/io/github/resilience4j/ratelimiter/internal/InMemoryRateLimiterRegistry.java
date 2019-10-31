/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Backend RateLimiter manager.
 * Constructs backend RateLimiters according to configuration values.
 */
public class InMemoryRateLimiterRegistry extends AbstractRegistry<RateLimiter, RateLimiterConfig> implements RateLimiterRegistry {

	/**
	 * The constructor with default default.
	 */
	public InMemoryRateLimiterRegistry() {
		this(RateLimiterConfig.ofDefaults());
	}

	public InMemoryRateLimiterRegistry(io.vavr.collection.Map<String, String> tags) {
		this(RateLimiterConfig.ofDefaults(), tags);
	}

	public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs) {
		this(configs, HashMap.empty());
	}

	public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs, io.vavr.collection.Map<String, String> tags) {
		this(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()), tags);
		this.configurations.putAll(configs);
	}

	public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs, RegistryEventConsumer<RateLimiter> registryEventConsumer) {
		this(configs, registryEventConsumer, HashMap.empty());
	}

	public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs, RegistryEventConsumer<RateLimiter> registryEventConsumer, io.vavr.collection.Map<String, String> tags) {
		this(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()), registryEventConsumer, tags);
		this.configurations.putAll(configs);
	}

	public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs, List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
		this(configs, registryEventConsumers, HashMap.empty());
	}

	public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs, List<RegistryEventConsumer<RateLimiter>> registryEventConsumers, io.vavr.collection.Map<String, String> tags) {
		this(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()), registryEventConsumers, tags);
		this.configurations.putAll(configs);
	}

	/**
	 * The constructor with custom default config.
	 *
	 * @param defaultConfig The default config.
	 */
	public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig) {
		super(defaultConfig);
	}

	public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig, io.vavr.collection.Map<String, String> tags) {
		super(defaultConfig, tags);
	}

	public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig, RegistryEventConsumer<RateLimiter> registryEventConsumer) {
		super(defaultConfig, registryEventConsumer);
	}

	public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig, RegistryEventConsumer<RateLimiter> registryEventConsumer, io.vavr.collection.Map<String, String> tags) {
		super(defaultConfig, registryEventConsumer, tags);
	}

	public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig, List<RegistryEventConsumer<RateLimiter>> registryEventConsumers) {
		super(defaultConfig, registryEventConsumers);
	}

	public InMemoryRateLimiterRegistry(RateLimiterConfig defaultConfig, List<RegistryEventConsumer<RateLimiter>> registryEventConsumers, io.vavr.collection.Map<String, String> tags) {
		super(defaultConfig, registryEventConsumers, tags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Seq<RateLimiter> getAllRateLimiters() {
		return Array.ofAll(entryMap.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(final String name) {
		return rateLimiter(name, getDefaultConfig());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(String name, io.vavr.collection.Map<String, String> tags) {
		return rateLimiter(name, getDefaultConfig(), tags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(final String name, final RateLimiterConfig config) {
		return rateLimiter(name, config, HashMap.empty());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(String name, RateLimiterConfig config, io.vavr.collection.Map<String, String> tags) {
		return computeIfAbsent(name, () -> new AtomicRateLimiter(name, Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(final String name, final Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
		return rateLimiter(name, rateLimiterConfigSupplier, HashMap.empty());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier, io.vavr.collection.Map<String, String> tags) {
		return computeIfAbsent(name, () -> new AtomicRateLimiter(name, Objects.requireNonNull(Objects.requireNonNull(rateLimiterConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL).get(), CONFIG_MUST_NOT_BE_NULL), getAllTags(tags)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(String name, String configName) {
		return rateLimiter(name, configName, HashMap.empty());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(String name, String configName, io.vavr.collection.Map<String, String> tags) {
		return computeIfAbsent(name, () -> RateLimiter.of(name, getConfiguration(configName)
				.orElseThrow(() -> new ConfigurationNotFoundException(configName)), getAllTags(tags)));
	}
}
