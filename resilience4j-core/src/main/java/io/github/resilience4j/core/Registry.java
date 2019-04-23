package io.github.resilience4j.core;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author romeh
 */
public interface Registry<Target, Config> {

	/**
	 * @param configName    the configuration name
	 * @param configuration the added configuration
	 */
	void addConfiguration(String configName, Config configuration);


	/**
	 * @param configName the configuration name
	 * @return the found configuration if any
	 */
	Optional<Config> getConfigurationByName(String configName);


	/**
	 * Allows for configuring some functionality to be executed when a new target is created.
	 *
	 * @param postCreationConsumer A consumer function to execute for a target that was created.
	 */
	void registerPostCreationConsumer(Consumer<Target> postCreationConsumer);


	/**
	 * Allows for configuring some functionality to be executed when a new target is created.
	 *
	 * @param postCreationConsumer A consumer function to execute for a target that was created.
	 */
	void unregisterPostCreationConsumer(Consumer<Target> postCreationConsumer);


}
