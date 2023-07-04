/*
 *
 *  Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.monitor;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.monitor.internal.InMemoryMonitorRegistry;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Manages all Monitor instances.
 */
public interface MonitorRegistry extends Registry<Monitor, MonitorConfig> {

    /**
     * Gets a registry builder.
     *
     * @return a builder for building {@link InMemoryMonitorRegistry} instances.
     */
    static InMemoryMonitorRegistry.Builder builder() {
        return new InMemoryMonitorRegistry.Builder();
    }

    /**
     * Returns all managed {@link Monitor} instances.
     *
     * @return all managed {@link Monitor} instances.
     */
    Stream<Monitor> getAllMonitors();

    /**
     * Returns a managed {@link Monitor} or creates a new one with the default Monitor
     * configuration.
     *
     * @param name the name of the Monitor
     * @return The {@link Monitor}
     */
    Monitor monitor(String name);

    /**
     * Returns a managed {@link Monitor} or creates a new one with the default Monitor
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the Monitor
     * @param tags tags added to the Monitor
     * @return The {@link Monitor}
     */
    Monitor monitor(String name, Map<String, String> tags);

    /**
     * Returns a managed {@link Monitor} or creates a new one with a custom Monitor
     * configuration.
     *
     * @param name        the name of the Monitor
     * @param monitorConfig a custom Monitor configuration
     * @return The {@link Monitor}
     */
    Monitor monitor(String name, MonitorConfig monitorConfig);

    /**
     * Returns a managed {@link Monitor} or creates a new one with a custom Monitor
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name        the name of the Monitor
     * @param monitorConfig a custom Monitor configuration
     * @param tags        tags added to the Monitor
     * @return The {@link Monitor}
     */
    Monitor monitor(String name, MonitorConfig monitorConfig, Map<String, String> tags);

    /**
     * Returns a managed {@link MonitorConfig} or creates a new one with a custom
     * MonitorConfig configuration.
     *
     * @param name                the name of the MonitorConfig
     * @param monitorConfigSupplier a supplier of a custom MonitorConfig configuration
     * @return The {@link MonitorConfig}
     */
    Monitor monitor(String name, Supplier<MonitorConfig> monitorConfigSupplier);

    /**
     * Returns a managed {@link Monitor} or creates a new one with a custom Monitor
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                the name of the Monitor
     * @param monitorConfigSupplier a supplier of a custom Monitor configuration
     * @param tags                tags added to the Monitor
     * @return The {@link Monitor}
     */
    Monitor monitor(String name, Supplier<MonitorConfig> monitorConfigSupplier, Map<String, String> tags);

    /**
     * Returns a managed {@link Monitor} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the Monitor
     * @param configName the name of the shared configuration
     * @return The {@link Monitor}
     */
    Monitor monitor(String name, String configName);

    /**
     * Returns a managed {@link Monitor} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the Monitor
     * @param configName the name of the shared configuration
     * @param tags       tags added to the Monitor
     * @return The {@link Monitor}
     */
    Monitor monitor(String name, String configName, Map<String, String> tags);
}
