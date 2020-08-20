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
package io.github.resilience4j.core.registry;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Abstract registry to be shared with all resilience4j registries
 */
public class AbstractRegistry<E, C> implements Registry<E, C> {

    protected static final String DEFAULT_CONFIG = "default";
    protected static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    protected static final String CONSUMER_MUST_NOT_BE_NULL = "EventConsumers must not be null";
    protected static final String SUPPLIER_MUST_NOT_BE_NULL = "Supplier must not be null";
    protected static final String TAGS_MUST_NOT_BE_NULL = "Tags must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String REGISTRY_STORE_MUST_NOT_BE_NULL = "Registry Store must not be null";

    protected final RegistryStore<E> entryMap;

    protected final ConcurrentMap<String, C> configurations;
    /**
     * Global tags which must be added to each instance created by this registry.
     */
    protected final Map<String, String> registryTags;

    private final RegistryEventProcessor eventProcessor;

    public AbstractRegistry(C defaultConfig) {
        this(defaultConfig, Collections.emptyMap());
    }

    public AbstractRegistry(C defaultConfig, Map<String, String> registryTags) {
        this(defaultConfig, new ArrayList<>(), registryTags);
    }

    public AbstractRegistry(C defaultConfig, RegistryEventConsumer<E> registryEventConsumer) {
        this(defaultConfig, registryEventConsumer, Collections.emptyMap());
    }

    public AbstractRegistry(C defaultConfig, RegistryEventConsumer<E> registryEventConsumer,
        Map<String, String> tags) {
        this(defaultConfig, Collections.singletonList(
            Objects.requireNonNull(registryEventConsumer, CONSUMER_MUST_NOT_BE_NULL)), tags);
    }

    public AbstractRegistry(C defaultConfig,
        List<RegistryEventConsumer<E>> registryEventConsumers) {
        this(defaultConfig, registryEventConsumers, Collections.emptyMap());
    }

    public AbstractRegistry(C defaultConfig, List<RegistryEventConsumer<E>> registryEventConsumers,
        Map<String, String> tags) {
        this.configurations = new ConcurrentHashMap<>();
        this.entryMap = new InMemoryRegistryStore<E>();
        this.eventProcessor = new RegistryEventProcessor(
            Objects.requireNonNull(registryEventConsumers, CONSUMER_MUST_NOT_BE_NULL));
        this.registryTags = Objects.requireNonNull(tags, TAGS_MUST_NOT_BE_NULL);
        this.configurations
            .put(DEFAULT_CONFIG, Objects.requireNonNull(defaultConfig, CONFIG_MUST_NOT_BE_NULL));
    }

    public AbstractRegistry(C defaultConfig, List<RegistryEventConsumer<E>> registryEventConsumers,
                            Map<String, String> tags, RegistryStore<E> registryStore) {
        this.configurations = new ConcurrentHashMap<>();
        this.entryMap = Objects.requireNonNull(registryStore, REGISTRY_STORE_MUST_NOT_BE_NULL);
        this.eventProcessor = new RegistryEventProcessor(
            Objects.requireNonNull(registryEventConsumers, CONSUMER_MUST_NOT_BE_NULL));
        this.registryTags = Objects.requireNonNull(tags, TAGS_MUST_NOT_BE_NULL);
        this.configurations
            .put(DEFAULT_CONFIG, Objects.requireNonNull(defaultConfig, CONFIG_MUST_NOT_BE_NULL));
    }

    protected E computeIfAbsent(String name, Supplier<E> supplier) {
        return entryMap.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL), k -> {
            E entry = supplier.get();
            eventProcessor.processEvent(new EntryAddedEvent<>(entry));
            return entry;
        });
    }

    @Override
    public Optional<E> find(String name) {
        return entryMap.find(name);
    }

    @Override
    public Optional<E> remove(String name) {
        Optional<E> removedEntry = entryMap.remove(name);
        removedEntry
            .ifPresent(entry -> eventProcessor.processEvent(new EntryRemovedEvent<>(entry)));
        return removedEntry;
    }

    @Override
    public Optional<E> replace(String name, E newEntry) {
        Optional<E> replacedEntry = entryMap.replace(name, newEntry);
        replacedEntry.ifPresent(
            oldEntry -> eventProcessor.processEvent(new EntryReplacedEvent<>(oldEntry, newEntry)));
        return replacedEntry;
    }

    @Override
    public void addConfiguration(String configName, C configuration) {
        if (configName.equals(DEFAULT_CONFIG)) {
            throw new IllegalArgumentException(
                "You cannot use 'default' as a configuration name as it is preserved for default configuration");
        }
        this.configurations.put(configName, configuration);
    }

    @Override
    public Optional<C> getConfiguration(String configName) {
        return Optional.ofNullable(this.configurations.get(configName));
    }

    @Override
    public C getDefaultConfig() {
        return configurations.get(DEFAULT_CONFIG);
    }

    @Override
    public Map<String, String> getTags() {
        return registryTags;
    }

    @Override
    public EventPublisher<E> getEventPublisher() {
        return eventProcessor;
    }

    /**
     * Creates map with all tags (registry and instance). When tags (keys) of the two collide the
     * tags passed with this method will override the tags of the registry.
     *
     * @param tags Tags of the instance.
     * @return Map containing all tags
     */
    protected Map<String, String> getAllTags(Map<String, String> tags) {
        final HashMap<String, String> allTags = new HashMap<>(Objects.requireNonNull(registryTags, TAGS_MUST_NOT_BE_NULL));
        allTags.putAll(tags);
        return allTags;
    }

    private class RegistryEventProcessor extends EventProcessor<RegistryEvent> implements
        EventConsumer<RegistryEvent>, EventPublisher<E> {

        private RegistryEventProcessor() {
        }

        private RegistryEventProcessor(List<RegistryEventConsumer<E>> registryEventConsumers) {
            registryEventConsumers.forEach(consumer -> {
                onEntryAdded(consumer::onEntryAddedEvent);
                onEntryRemoved(consumer::onEntryRemovedEvent);
                onEntryReplaced(consumer::onEntryReplacedEvent);
            });
        }

        @Override
        public EventPublisher<E> onEntryAdded(
            EventConsumer<EntryAddedEvent<E>> onSuccessEventConsumer) {
            registerConsumer(EntryAddedEvent.class.getSimpleName(), onSuccessEventConsumer);
            return this;
        }

        @Override
        public EventPublisher<E> onEntryRemoved(
            EventConsumer<EntryRemovedEvent<E>> onErrorEventConsumer) {
            registerConsumer(EntryRemovedEvent.class.getSimpleName(), onErrorEventConsumer);
            return this;
        }

        @Override
        public EventPublisher<E> onEntryReplaced(
            EventConsumer<EntryReplacedEvent<E>> onStateTransitionEventConsumer) {
            registerConsumer(EntryReplacedEvent.class.getSimpleName(),
                onStateTransitionEventConsumer);
            return this;
        }

        @Override
        public void consumeEvent(RegistryEvent event) {
            super.processEvent(event);
        }
    }

}
