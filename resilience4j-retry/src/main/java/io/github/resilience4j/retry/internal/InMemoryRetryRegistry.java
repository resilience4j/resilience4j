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
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.AbstractRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Backend retry manager.
 * Constructs backend retries according to configuration values.
 */
public final class InMemoryRetryRegistry extends AbstractRegistry<Retry, RetryConfig> implements RetryRegistry {

	/**
	 * The constructor with default default.
	 */
	public InMemoryRetryRegistry() {
		this(RetryConfig.ofDefaults());
	}

	public InMemoryRetryRegistry(Map<String, RetryConfig> configs) {
		this(configs.getOrDefault(DEFAULT_CONFIG, RetryConfig.ofDefaults()));
		this.configurations.putAll(configs);
	}

	/**
	 * The constructor with custom default config.
	 *
	 * @param defaultConfig The default config.
	 */
	public InMemoryRetryRegistry(RetryConfig defaultConfig) {
		super(defaultConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Seq<Retry> getAllRetries() {
		return Array.ofAll(targetMap.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Retry retry(String name) {
		return retry(name, getDefaultConfig());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Retry retry(String name, RetryConfig retryConfig) {
		return computeIfAbsent(name, () -> Retry.of(name, retryConfig));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Retry retry(String name, Supplier<RetryConfig> retryConfigSupplier) {
		return computeIfAbsent(name, () -> Retry.of(name, retryConfigSupplier.get()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Retry retry(String name, String configName) {
		return computeIfAbsent(name, () -> Retry.of(name, getConfiguration(configName)
				.orElseThrow(() -> new ConfigurationNotFoundException(String.format("Configuration with name '%s' is not found ", configName)))));
	}
}
