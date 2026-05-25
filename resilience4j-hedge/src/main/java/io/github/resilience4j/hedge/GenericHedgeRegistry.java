/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.hedge.internal.InMemoryGenericHedgeRegistry;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Manages all Hedge instances.
 *
 * @param <H> the specific Hedge type
 * @param <C> the specific configuration type
 */
public interface GenericHedgeRegistry<H extends GenericHedge, C extends SimpleHedgeConfig> extends Registry<H, C> {

    /**
     * Gets a registry builder.
     *
     * @param executorServiceFunction a function to provide an executor service
     * @return a builder for building {@link InMemoryGenericHedgeRegistry} instances.
     */
    static InMemoryGenericHedgeRegistry.Builder builder(Function<SimpleHedgeConfig, ScheduledExecutorService> executorServiceFunction) {
        return new InMemoryGenericHedgeRegistry.Builder(executorServiceFunction);
    }

    /**
     * Returns all managed {@link GenericHedge} instances.
     *
     * @return all managed {@link GenericHedge} instances.
     */
    Stream<? extends GenericHedge> getAllHedges();

    /**
     * Returns a managed {@link GenericHedge} or creates a new one with the default Hedge
     * configuration.
     *
     * @param name the name of the Hedge
     * @return The {@link GenericHedge}
     */
    GenericHedge hedge(String name);

    /**
     * Returns a managed {@link GenericHedge} or creates a new one with the default Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the Hedge
     * @param tags tags added to the Hedge
     * @return The {@link GenericHedge}
     */
    GenericHedge hedge(String name, Map<String, String> tags);

    /**
     * Returns a managed {@link GenericHedge} or creates a new one with a custom Hedge
     * configuration.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @return The {@link GenericHedge}
     */
    GenericHedge hedge(String name, C hedgeConfig);

    /**
     * Returns a managed {@link GenericHedge} or creates a new one with a custom Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @param tags        tags added to the Hedge
     * @return The {@link GenericHedge}
     */
    GenericHedge hedge(String name, C hedgeConfig,
                Map<String, String> tags);

    /**
     * Returns a managed {@link GenericHedge} or creates a new one with a custom
     * HedgeConfig configuration.
     *
     * @param name                the name of the HedgeConfig
     * @param hedgeConfigSupplier a supplier of a custom HedgeConfig configuration
     * @return The {@link HedgeConfig}
     */
    GenericHedge hedge(String name, Supplier<C> hedgeConfigSupplier);

    /**
     * Returns a managed {@link GenericHedge} or creates a new one with a custom Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                the name of the Hedge
     * @param hedgeConfigSupplier a supplier of a custom Hedge configuration
     * @param tags                tags added to the Hedge
     * @return The {@link GenericHedge}
     */
    GenericHedge hedge(String name,
                Supplier<C> hedgeConfigSupplier,
                Map<String, String> tags);

    /**
     * Returns a managed {@link GenericHedge} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the Hedge
     * @param configName the name of the shared configuration
     * @return The {@link GenericHedge}
     */
    GenericHedge hedge(String name, String configName);

    /**
     * Returns a managed {@link GenericHedge} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the Hedge
     * @param configName the name of the shared configuration
     * @param tags       tags added to the Hedge
     * @return The {@link GenericHedge}
     */
    GenericHedge hedge(String name, String configName,
                Map<String, String> tags);

}
