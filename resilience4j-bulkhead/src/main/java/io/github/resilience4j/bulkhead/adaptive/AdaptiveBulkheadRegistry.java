/*
 *
 *  Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive;


import io.github.resilience4j.bulkhead.adaptive.internal.InMemoryAdaptiveBulkheadRegistry;
import io.github.resilience4j.core.Registry;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The {@link AdaptiveBulkheadRegistry} is a factory to create AdaptiveBulkhead instances which stores all bulkhead instances in a registry.
 */
public interface AdaptiveBulkheadRegistry extends Registry<AdaptiveBulkhead, AdaptiveBulkheadConfig> {

	/**
	 * Returns all managed {@link AdaptiveBulkhead} instances.
	 *
	 * @return all managed {@link AdaptiveBulkhead} instances.
	 */
	Set<AdaptiveBulkhead> getAllBulkheads();

	/**
	 * Returns a managed {@link AdaptiveBulkhead} or creates a new one with default configuration.
	 *
	 * @param name the name of the AdaptiveBulkhead
	 * @return The {@link AdaptiveBulkhead}
	 */
	AdaptiveBulkhead bulkhead(String name);

	/**
	 * Returns a managed {@link AdaptiveBulkhead} or creates a new one with a custom AdaptiveBulkheadConfig configuration.
	 *
	 * @param name   the name of the AdaptiveBulkhead
	 * @param config a custom AdaptiveBulkhead configuration
	 * @return The {@link AdaptiveBulkhead}
	 */
	AdaptiveBulkhead bulkhead(String name, AdaptiveBulkheadConfig config);

	/**
	 * Returns a managed {@link AdaptiveBulkhead} or creates a new one with a custom AdaptiveBulkhead configuration.
	 *
	 * @param name                   the name of the AdaptiveBulkhead
	 * @param bulkheadConfigSupplier a custom AdaptiveBulkhead configuration supplier
	 * @return The {@link AdaptiveBulkhead}
	 */
	AdaptiveBulkhead bulkhead(String name, Supplier<AdaptiveBulkheadConfig> bulkheadConfigSupplier);

	/**
	 * Returns a managed {@link AdaptiveBulkhead} or creates a new one with a custom AdaptiveBulkhead configuration.
	 *
	 * @param name       the name of the AdaptiveBulkhead
	 * @param configName a custom AdaptiveBulkhead configuration name
	 * @return The {@link AdaptiveBulkhead}
	 */
	AdaptiveBulkhead bulkhead(String name, String configName);

	/**
	 * Creates a BulkheadRegistry with a custom AdaptiveBulkhead configuration.
	 *
	 * @param bulkheadConfig a custom AdaptiveBulkhead configuration
	 * @return a BulkheadRegistry instance backed by a custom AdaptiveBulkhead configuration
	 */
	static AdaptiveBulkheadRegistry of(AdaptiveBulkheadConfig bulkheadConfig) {
		return new InMemoryAdaptiveBulkheadRegistry(bulkheadConfig);
	}

	/**
	 * Creates a BulkheadRegistry with a Map of shared Bulkhead configurations.
	 *
	 * @param configs a Map of shared AdaptiveBulkhead configurations
	 * @return a RetryRegistry with a Map of shared AdaptiveBulkhead configurations.
	 */
	static AdaptiveBulkheadRegistry of(Map<String, AdaptiveBulkheadConfig> configs) {
		return new InMemoryAdaptiveBulkheadRegistry(configs);
	}

	/**
	 * Creates a BulkheadRegistry with a default AdaptiveBulkhead configuration
	 *
	 * @return a BulkheadRegistry instance backed by a default Bulkhead configuration
	 */
	static AdaptiveBulkheadRegistry ofDefaults() {
		return new InMemoryAdaptiveBulkheadRegistry(AdaptiveBulkheadConfig.ofDefaults());
	}

}
