/*
 *
 *  Copyright 2019 Mahmoud Romeh, Robert Winkler
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

import java.util.Map;
import java.util.Optional;

/**
 * root resilience4j registry to be used by resilience types registries for common functionality
 */
public interface Registry<E, C> {

    /**
     * Adds a configuration to the registry
     *
     * @param configName    the configuration name
     * @param configuration the added configuration
     */
    void addConfiguration(String configName, C configuration);

    /**
     * Find a named entry in the Registry
     *
     * @param name the  name
     */
    Optional<E> find(String name);

    /**
     * Remove an entry from the Registry
     *
     * @param name the  name
     */
    Optional<E> remove(String name);

    /**
     * Replace an existing entry in the Registry by a new one.
     *
     * @param name     the existing name
     * @param newEntry a new entry
     */
    Optional<E> replace(String name, E newEntry);

    /**
     * Get a configuration by name
     *
     * @param configName the configuration name
     * @return the found configuration if any
     */
    Optional<C> getConfiguration(String configName);

    /**
     * Get the default configuration
     *
     * @return the default configuration
     */
    C getDefaultConfig();

    /**
     * @return global configured registry tags
     */
    Map<String, String> getTags();

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher<E> getEventPublisher();

    /**
     * An EventPublisher can be used to register event consumers.
     */
    interface EventPublisher<E> extends io.github.resilience4j.core.EventPublisher<RegistryEvent> {

        EventPublisher<E> onEntryAdded(EventConsumer<EntryAddedEvent<E>> eventConsumer);

        EventPublisher<E> onEntryRemoved(EventConsumer<EntryRemovedEvent<E>> eventConsumer);

        EventPublisher<E> onEntryReplaced(EventConsumer<EntryReplacedEvent<E>> eventConsumer);
    }
}
