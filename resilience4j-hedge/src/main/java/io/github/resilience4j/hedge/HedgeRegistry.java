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
import io.github.resilience4j.hedge.internal.InMemoryHedgeRegistry;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Manages all Hedge instances.
 */
public interface HedgeRegistry extends Registry<Hedge, HedgeConfig> {

    /**
     * Gets a registry builder.
     *
     * @return a builder for building {@link InMemoryHedgeRegistry} instances.
     */
    static InMemoryHedgeRegistry.Builder builder() {
        return new InMemoryHedgeRegistry.Builder();
    }

    /**
     * Returns all managed {@link Hedge} instances.
     *
     * @return all managed {@link Hedge} instances.
     */
    Stream<Hedge> getAllHedges();

    /**
     * Returns a managed {@link Hedge} or creates a new one with the default Hedge
     * configuration.
     *
     * @param name the name of the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name);

    /**
     * Returns a managed {@link Hedge} or creates a new one with the default Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the Hedge
     * @param tags tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, Map<String, String> tags);

    /**
     * Returns a managed {@link Hedge} or creates a new one with a custom Hedge
     * configuration.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, HedgeConfig hedgeConfig);

    /**
     * Returns a managed {@link Hedge} or creates a new one with a custom Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @param tags        tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, HedgeConfig hedgeConfig,
                Map<String, String> tags);

    /**
     * Returns a managed {@link HedgeConfig} or creates a new one with a custom
     * HedgeConfig configuration.
     *
     * @param name                the name of the HedgeConfig
     * @param hedgeConfigSupplier a supplier of a custom HedgeConfig configuration
     * @return The {@link HedgeConfig}
     */
    Hedge hedge(String name, Supplier<HedgeConfig> hedgeConfigSupplier);

    /**
     * Returns a managed {@link Hedge} or creates a new one with a custom Hedge
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                the name of the Hedge
     * @param hedgeConfigSupplier a supplier of a custom Hedge configuration
     * @param tags                tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name,
                Supplier<HedgeConfig> hedgeConfigSupplier,
                Map<String, String> tags);

    /**
     * Returns a managed {@link Hedge} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the Hedge
     * @param configName the name of the shared configuration
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, String configName);

    /**
     * Returns a managed {@link Hedge} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the Hedge
     * @param configName the name of the shared configuration
     * @param tags       tags added to the Hedge
     * @return The {@link Hedge}
     */
    Hedge hedge(String name, String configName,
                Map<String, String> tags);

}
