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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

/**
 * Thread pool Bulkhead instance manager;
 * Constructs/returns thread pool bulkhead instances.
 */
public final class InMemoryThreadPoolBulkheadRegistry implements ThreadPoolBulkheadRegistry {

	private final ThreadPoolBulkheadConfig defaultBulkheadConfig;

	/**
	 * The bulkheads, indexed by name
	 */
	private final ConcurrentMap<String, ThreadPoolBulkhead> bulkheads;

	/**
	 * The constructor with custom default bulkhead config
	 *
	 * @param bulkheadConfig custom bulkhead config to use
	 */
	public InMemoryThreadPoolBulkheadRegistry(ThreadPoolBulkheadConfig bulkheadConfig) {
		this.defaultBulkheadConfig = bulkheadConfig;
		this.bulkheads = new ConcurrentHashMap<>();
	}

	@Override
	public Seq<ThreadPoolBulkhead> getAllBulkheads() {
		return Array.ofAll(bulkheads.values());
	}

	@Override
	public ThreadPoolBulkhead bulkhead(String name) {
		return bulkhead(name, defaultBulkheadConfig);
	}

	@Override
	public ThreadPoolBulkhead bulkhead(String name, ThreadPoolBulkheadConfig bulkheadConfig) {
		return bulkheads.computeIfAbsent(
				Objects.requireNonNull(name, "Name must not be null"),
				k -> ThreadPoolBulkhead.of(name, bulkheadConfig)
		);
	}

	@Override
	public ThreadPoolBulkhead bulkhead(String name, Supplier<ThreadPoolBulkheadConfig> bulkheadConfigSupplier) {
		return bulkheads.computeIfAbsent(
				Objects.requireNonNull(name, "Name must not be null"),
				k -> ThreadPoolBulkhead.of(name, bulkheadConfigSupplier.get())
		);
	}

	@Override
	public ThreadPoolBulkheadConfig getDefaultBulkheadConfig() {
		return defaultBulkheadConfig;
	}
}
