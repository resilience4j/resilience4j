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

import java.util.Optional;
import java.util.function.Consumer;

/**
 * root resilience4j registry to be used by resilience types registries for common functionality
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
	Optional<Config> getConfiguration(String configName);


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


	/**
	 * @return the default configuration of the target
	 */
	Config getDefaultConfig();


}
