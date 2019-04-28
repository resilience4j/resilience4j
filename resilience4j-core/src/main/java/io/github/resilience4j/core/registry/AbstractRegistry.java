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
package io.github.resilience4j.core.registry;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Abstract registry to be shared with all resilience4j registries
 */
public class AbstractRegistry<Target, Config> implements Registry<Target, Config> {
	protected static final String DEFAULT_CONFIG = "default";
	private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
	protected static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
	protected static final String SUPPLIER_MUST_NOT_BE_NULL = "Supplier must not be null";

	/**
	 * The map of targets by name
	 */
	protected final ConcurrentMap<String, Target> entryMap;

	/**
	 * The map of shared configuration by name
	 */
	protected final ConcurrentMap<String, Config> configurations;

	private final RegistryEventProcessor eventProcessor;

	public AbstractRegistry(Config defaultConfig) {
		this.configurations = new ConcurrentHashMap<>();
		this.entryMap = new ConcurrentHashMap<>();
		this.eventProcessor = new RegistryEventProcessor();
		this.configurations.put(DEFAULT_CONFIG, Objects.requireNonNull(defaultConfig, CONFIG_MUST_NOT_BE_NULL));
	}

	protected Target computeIfAbsent(String name, Supplier<Target> supplier){
		return entryMap.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL), k -> {
			Target entry = supplier.get();
			eventProcessor.processEvent(new EntryAddedEvent<>(entry));
			return entry;
		});
	}

	@Override
	public Optional<Target> remove(String name){
		Optional<Target> removedEntry = Optional.ofNullable(entryMap.remove(name));
		removedEntry.ifPresent(entry -> eventProcessor.processEvent(new EntryRemovedEvent<>(entry)));
		return removedEntry;
	}

	@Override
	public Optional<Target> replace(String name, Target newEntry){
		Optional<Target> replacedEntry = Optional.ofNullable(entryMap.replace(name, newEntry));
		replacedEntry.ifPresent(oldEntry -> eventProcessor.processEvent(new EntryReplacedEvent<>(oldEntry, newEntry)));
		return replacedEntry;
	}

	@Override
	public void addConfiguration(String configName, Config configuration) {
		if (configName.equals(DEFAULT_CONFIG)) {
			throw new IllegalArgumentException("you can not use 'default' as a configuration name as it is preserved for default configuration");
		}
		this.configurations.put(configName, configuration);
	}

	@Override
	public Optional<Config> getConfiguration(String configName) {
		return Optional.ofNullable(this.configurations.get(configName));
	}

	@Override
	public Config getDefaultConfig() {
		return configurations.get(DEFAULT_CONFIG);
	}

	@Override
	public EventPublisher<Target> getEventPublisher() {
		return eventProcessor;
	}

	private class RegistryEventProcessor extends EventProcessor<RegistryEvent> implements EventConsumer<RegistryEvent>, EventPublisher<Target> {

		@Override
		public EventPublisher onEntryAdded(EventConsumer<EntryAddedEvent<Target>> onSuccessEventConsumer) {
			registerConsumer(EntryAddedEvent.class.getSimpleName(), onSuccessEventConsumer);
			return this;
		}

		@Override
		public EventPublisher onEntryRemoved(EventConsumer<EntryRemovedEvent<Target>> onErrorEventConsumer) {
			registerConsumer(EntryRemovedEvent.class.getSimpleName(), onErrorEventConsumer);
			return this;
		}

		@Override
		public EventPublisher onEntryReplaced(EventConsumer<EntryReplacedEvent<Target>> onStateTransitionEventConsumer) {
			registerConsumer(EntryReplacedEvent.class.getSimpleName(), onStateTransitionEventConsumer);
			return this;
		}

		@Override
		public void consumeEvent(RegistryEvent event) {
			super.processEvent(event);
		}
	}

}
