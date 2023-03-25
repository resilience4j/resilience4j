/*
 *
 *  Copyright 2019  Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive.internal;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.AbstractRegistry;

/**
 * Bulkhead instance manager;
 * Constructs/returns AdaptiveBulkhead instances.
 */
public final class InMemoryAdaptiveBulkheadRegistry extends AbstractRegistry<AdaptiveBulkhead, AdaptiveBulkheadConfig> implements AdaptiveBulkheadRegistry {

	/**
	 * The constructor with default default.
	 */
	public InMemoryAdaptiveBulkheadRegistry() {
		this(AdaptiveBulkheadConfig.ofDefaults());
	}

	public InMemoryAdaptiveBulkheadRegistry(Map<String, AdaptiveBulkheadConfig> configs) {
		this(configs.getOrDefault(DEFAULT_CONFIG, AdaptiveBulkheadConfig.ofDefaults()));
		this.configurations.putAll(configs);
	}

	/**
	 * The constructor with custom default config.
	 *
	 * @param defaultConfig The default config.
	 */
	public InMemoryAdaptiveBulkheadRegistry(AdaptiveBulkheadConfig defaultConfig) {
		super(defaultConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AdaptiveBulkhead> getAllBulkheads() {
		return new HashSet<>(entryMap.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AdaptiveBulkhead bulkhead(String name) {
		return bulkhead(name, getDefaultConfig());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AdaptiveBulkhead bulkhead(String name, AdaptiveBulkheadConfig config) {
		return computeIfAbsent(name, () -> AdaptiveBulkhead.of(name, Objects.requireNonNull(config, CONFIG_MUST_NOT_BE_NULL)));
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
    public AdaptiveBulkhead bulkhead(String name, Supplier<AdaptiveBulkheadConfig> bulkheadConfigSupplier) {
        Supplier<AdaptiveBulkheadConfig> supplier = Objects.requireNonNull(bulkheadConfigSupplier, SUPPLIER_MUST_NOT_BE_NULL);
        AdaptiveBulkheadConfig config = Objects.requireNonNull(supplier.get(), CONFIG_MUST_NOT_BE_NULL);
        return computeIfAbsent(name, () -> AdaptiveBulkhead.of(name, config));
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AdaptiveBulkhead bulkhead(String name, String configName) {
		return computeIfAbsent(name, () -> AdaptiveBulkhead.of(name, getConfiguration(configName)
				.orElseThrow(() -> new ConfigurationNotFoundException(configName))));
	}

}
