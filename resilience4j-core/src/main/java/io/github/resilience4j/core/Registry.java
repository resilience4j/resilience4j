/*
 *
 *  Copyright 2019 Mahmoud romeh
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
package io.github.resilience4j.core;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEvent;

import java.util.Optional;

/**
 * root resilience4j registry to be used by resilience types registries for common functionality
 */
public interface Registry<Target, Config> {

	/**
	 * Adds a configuration to the registry
	 *
	 * @param configName    the configuration name
	 * @param configuration the added configuration
	 */
	void addConfiguration(String configName, Config configuration);

	/**
	 * Remove an entry from the Registry
	 *
	 * @param name    the  name
	 */
	Optional<Target> remove(String name);

	/**
	 * Replace an existing entry in the Registry by a new one.
	 *
	 * @param name    the existing name
	 * @param newEntry    a new entry
	 */
	Optional<Target> replace(String name, Target newEntry);

	/**
	 * Get a configuration by name
	 *
	 * @param configName the configuration name
	 * @return the found configuration if any
	 */
	Optional<Config> getConfiguration(String configName);

	/**
	 * Get the default configuration
	 *
	 * @return the default configuration
	 */
	Config getDefaultConfig();

	/**
	 * Returns an EventPublisher which can be used to register event consumers.
	 *
	 * @return an EventPublisher
	 */
	EventPublisher<Target> getEventPublisher();

	/**
	 * An EventPublisher can be used to register event consumers.
	 */
	interface EventPublisher<Target> extends io.github.resilience4j.core.EventPublisher<RegistryEvent> {

		EventPublisher onEntryAdded(EventConsumer<EntryAddedEvent<Target>> eventConsumer);

		EventPublisher onEntryRemoved(EventConsumer<EntryRemovedEvent<Target>> eventConsumer);

		EventPublisher onEntryReplaced(EventConsumer<EntryReplacedEvent<Target>> eventConsumer);
	}
}
