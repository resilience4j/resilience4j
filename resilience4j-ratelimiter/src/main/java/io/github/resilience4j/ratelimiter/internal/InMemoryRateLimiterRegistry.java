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

import io.github.resilience4j.core.AbstractRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Backend RateLimiter manager.
 * Constructs backend RateLimiters according to configuration values.
 */
public class InMemoryRateLimiterRegistry extends AbstractRegistry<RateLimiter, RateLimiterConfig> implements RateLimiterRegistry {

	private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
	private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
	private static final String SUPPLIER_MUST_NOT_BE_NULL = "Supplier must not be null";

	/**
	 * The constructor with default default.
	 */
	public InMemoryRateLimiterRegistry() {
		this(RateLimiterConfig.ofDefaults());
	}

	public InMemoryRateLimiterRegistry(Map<String, RateLimiterConfig> configs) {
		this(configs.getOrDefault(DEFAULT_CONFIG, RateLimiterConfig.ofDefaults()));
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Seq<RateLimiter> getAllRateLimiters() {
		return Array.ofAll(targetMap.values());
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
	public RateLimiter rateLimiter(final String name, final RateLimiterConfig rateLimiterConfig) {
		return computeIfAbsent(name, () -> new AtomicRateLimiter(name, rateLimiterConfig));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RateLimiter rateLimiter(final String name, final Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
		return computeIfAbsent(name, () -> new AtomicRateLimiter(name, rateLimiterConfigSupplier.get()));
	}
}
