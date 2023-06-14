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
package io.github.resilience4j.monitor.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.monitor.Monitor;
import io.github.resilience4j.monitor.MonitorConfig;
import io.github.resilience4j.monitor.MonitorRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Constructs backend Monitors according to configuration values.
 */
public class InMemoryMonitorRegistry extends AbstractRegistry<Monitor, MonitorConfig> implements MonitorRegistry {

    /**
     * Constructor
     *
     * @param defaultConfig          the default config to use for new Monitors
     * @param registryEventConsumers initialized consumers for monitors
     * @param tags                   a map of tags for the registry
     */
    public InMemoryMonitorRegistry(MonitorConfig defaultConfig,
                                   List<RegistryEventConsumer<Monitor>> registryEventConsumers,
                                   Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);

    }

    public static class Builder {

        private final Map<String, String> tags = new HashMap<>();
        private final Map<String, MonitorConfig> configs = new HashMap<>();
        private MonitorConfig defaultConfig = MonitorConfig.ofDefaults();
        private final List<RegistryEventConsumer<Monitor>> consumers = new ArrayList<>();

        public Builder withTags(@NonNull Map<String, String> tags) {
            this.tags.putAll(tags);
            return this;
        }

        public Builder withConfigs(@NonNull Map<String, MonitorConfig> configs) {
            this.configs.putAll(configs);
            return this;
        }

        public Builder withDefaultConfig(@NonNull MonitorConfig defaultConfig) {
            this.defaultConfig = defaultConfig;
            return this;
        }

        public Builder withConsumers(@NonNull List<RegistryEventConsumer<Monitor>> registryEventConsumers) {
            this.consumers.addAll(registryEventConsumers);
            return this;
        }

        public Builder withConsumer(@NonNull RegistryEventConsumer<Monitor> registryEventConsumer) {
            this.consumers.add(registryEventConsumer);
            return this;
        }

        public MonitorRegistry build() {
            configs.remove("default");
            MonitorRegistry registry = new InMemoryMonitorRegistry(defaultConfig, consumers, tags);
            configs.forEach(registry::addConfiguration);
            return registry;
        }
    }

    @Override
    public Stream<Monitor> getAllMonitors() {
        return entryMap.values().stream();
    }

    @Override
    public Monitor monitor(final String name) {
        return monitor(name, getDefaultConfig(), emptyMap());
    }

    @Override
    public Monitor monitor(String name, Map<String, String> tags) {
        return monitor(name, getDefaultConfig(), tags);
    }

    @Override
    public Monitor monitor(final String name, final MonitorConfig config) {
        return monitor(name, config, emptyMap());
    }

    @Override
    public Monitor monitor(String name, MonitorConfig monitorConfig, Map<String, String> tags) {
        return computeIfAbsent(name, () -> Monitor.of(name, monitorConfig, getAllTags(tags)));
    }

    @Override
    public Monitor monitor(final String name, final Supplier<MonitorConfig> monitorConfigSupplier) {
        return monitor(name, monitorConfigSupplier, emptyMap());
    }

    @Override
    public Monitor monitor(String name, Supplier<MonitorConfig> monitorConfigSupplier, Map<String, String> tags) {
        return computeIfAbsent(name, () -> Monitor.of(name, monitorConfigSupplier.get(), getAllTags(tags)));
    }

    @Override
    public Monitor monitor(String name, String configName) {
        return monitor(name, configName, emptyMap());
    }

    @Override
    public Monitor monitor(String name, String configName, Map<String, String> tags) {
        MonitorConfig config = getConfiguration(configName).orElseThrow(() -> new ConfigurationNotFoundException(configName));
        return monitor(name, config, tags);
    }
}
