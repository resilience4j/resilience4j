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
package io.github.resilience4j.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Abstract registry to be shared with all resilience4j registries
 */
public class AbstractRegistry<Target, Config> implements Registry<Target, Config> {
	protected static final String DEFAULT_CONFIG = "default";
	private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";

	/**
	 * The list of consumer functions to execute after a target is created.
	 */
	private final List<Consumer<Target>> postCreationConsumers;

	/**
	 * The map of targets by name
	 */
	protected final ConcurrentMap<String, Target> targetMap;

	/**
	 * The map of shared configuration by name
	 */
	protected final ConcurrentMap<String, Config> configurations;

	public AbstractRegistry(Config defaultConfig) {
		this.postCreationConsumers = new CopyOnWriteArrayList<>();
		this.configurations = new ConcurrentHashMap<>();
		this.targetMap = new ConcurrentHashMap<>();
		this.configurations.put(DEFAULT_CONFIG, Objects.requireNonNull(defaultConfig, "Default config must not be null"));
	}

	protected Target computeIfAbsent(String name, Supplier<Target> supplier){
		return targetMap.computeIfAbsent(Objects.requireNonNull(name, NAME_MUST_NOT_BE_NULL), k -> notifyPostCreationConsumers(supplier.get()));
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
	public void registerPostCreationConsumer(Consumer<Target> postCreationConsumer) {
		postCreationConsumers.add(postCreationConsumer);
	}

	@Override
	public void unregisterPostCreationConsumer(Consumer<Target> postCreationConsumer) {
		postCreationConsumers.remove(postCreationConsumer);
	}

	@Override
	public Config getDefaultConfig() {
		return configurations.get(DEFAULT_CONFIG);
	}

	protected Target notifyPostCreationConsumers(Target target) {
		if (!postCreationConsumers.isEmpty()) {
			postCreationConsumers.forEach(consumer -> consumer.accept(target));
		}
		return target;
	}

}
