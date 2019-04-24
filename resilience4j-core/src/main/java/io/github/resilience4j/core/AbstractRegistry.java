package io.github.resilience4j.core;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Abstract registry to be shared with all resilience4j registries
 */
public class AbstractRegistry<Target, Config> implements Registry<Target, Config> {
	protected static final String DEFAULT_CONFIG = "default";
	/**
	 * The list of consumer functions to execute after a target is created.
	 */
	protected final List<Consumer<Target>> postCreationConsumers;

	/**
	 * The map of shared  configuration by name
	 */
	protected final ConcurrentMap<String, Config> configurations;

	public AbstractRegistry() {
		this.postCreationConsumers = new CopyOnWriteArrayList<>();
		this.configurations = new ConcurrentHashMap<>();
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

	protected Target notifyPostCreationConsumers(Target target) {
		if (!postCreationConsumers.isEmpty()) {
			postCreationConsumers.forEach(consumer -> consumer.accept(target));
		}
		return target;
	}

}
