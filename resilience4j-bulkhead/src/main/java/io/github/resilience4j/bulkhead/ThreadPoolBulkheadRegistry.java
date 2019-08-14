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
package io.github.resilience4j.bulkhead;


import io.github.resilience4j.bulkhead.internal.InMemoryThreadPoolBulkheadRegistry;
import io.github.resilience4j.core.Registry;
import io.vavr.collection.Seq;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link ThreadPoolBulkheadRegistry} is a factory to create ThreadPoolBulkhead instances which stores all bulkhead instances in a registry.
 */
public interface ThreadPoolBulkheadRegistry  extends Registry<ThreadPoolBulkhead, ThreadPoolBulkheadConfig> {

	/**
	 * Creates a BulkheadRegistry with a custom Bulkhead configuration.
	 *
	 * @param bulkheadConfig a custom ThreadPoolBulkhead configuration
	 * @return a ThreadPoolBulkheadRegistry instance backed by a custom ThreadPoolBulkhead configuration
	 */
	static ThreadPoolBulkheadRegistry of(ThreadPoolBulkheadConfig bulkheadConfig) {
		return new InMemoryThreadPoolBulkheadRegistry(bulkheadConfig);
	}

	/**
	 * Creates a ThreadPoolBulkheadRegistry with a default ThreadPoolBulkhead configuration
	 *
	 * @return a ThreadPoolBulkheadRegistry instance backed by a default ThreadPoolBulkhead configuration
	 */
	static ThreadPoolBulkheadRegistry ofDefaults() {
		return new InMemoryThreadPoolBulkheadRegistry(ThreadPoolBulkheadConfig.ofDefaults());
	}

	/**
	 * Creates a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations.
	 *
	 * @param configs a Map of shared Bulkhead configurations
	 * @return a ThreadPoolBulkheadRegistry with a Map of shared ThreadPoolBulkhead configurations.
	 */
	static ThreadPoolBulkheadRegistry of(Map<String, ThreadPoolBulkheadConfig> configs) {
		return new InMemoryThreadPoolBulkheadRegistry(configs);
	}

	/**
	 * Returns all managed {@link ThreadPoolBulkhead} instances.
	 *
	 * @return all managed {@link ThreadPoolBulkhead} instances.
	 */
	Seq<ThreadPoolBulkhead> getAllBulkheads();

	/**
	 * Returns a managed {@link ThreadPoolBulkhead} or creates a new one with default configuration.
	 *
	 * @param name the name of the ThreadPoolBulkhead
	 * @return The {@link ThreadPoolBulkhead}
	 */
	ThreadPoolBulkhead bulkhead(String name);

	/**
	 * Returns a managed {@link ThreadPoolBulkhead} or creates a new one with a custom ThreadPoolBulkhead configuration.
	 *
	 * @param name           the name of the ThreadPoolBulkhead
	 * @param config a custom ThreadPoolBulkheadConfig configuration
	 * @return The {@link ThreadPoolBulkhead}
	 */
	ThreadPoolBulkhead bulkhead(String name, ThreadPoolBulkheadConfig config);

	/**
	 * Returns a managed {@link ThreadPoolBulkhead} or creates a new one with a custom ThreadPoolBulkhead configuration.
	 *
	 * @param name                   the name of the ThreadPoolBulkhead
	 * @param bulkheadConfigSupplier a custom ThreadPoolBulkhead configuration supplier
	 * @return The {@link ThreadPoolBulkhead}
	 */
	ThreadPoolBulkhead bulkhead(String name, Supplier<ThreadPoolBulkheadConfig> bulkheadConfigSupplier);

	/**
	 * Returns a managed {@link ThreadPoolBulkhead} or creates a new one with a custom ThreadPoolBulkhead configuration.
	 *
	 * @param name       the name of the ThreadPoolBulkhead
	 * @param configName a custom CircuitBreaker ThreadPoolBulkhead name
	 * @return The {@link ThreadPoolBulkhead}
	 */
	ThreadPoolBulkhead bulkhead(String name, String configName);

}
