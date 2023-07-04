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
package io.github.resilience4j.micrometer.internal;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerConfig;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Constructs backend Timers according to configuration values.
 */
public class InMemoryTimerRegistry extends AbstractRegistry<Timer, TimerConfig> implements TimerRegistry {

    /**
     * Constructor
     *
     * @param defaultConfig          the default config to use for new Timers
     * @param registryEventConsumers initialized consumers for timers
     * @param tags                   a map of tags for the registry
     */
    public InMemoryTimerRegistry(TimerConfig defaultConfig, List<RegistryEventConsumer<Timer>> registryEventConsumers, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
    }

    public static class Builder {

        private final Map<String, String> tags = new HashMap<>();
        private final Map<String, TimerConfig> configs = new HashMap<>();
        private TimerConfig defaultConfig = TimerConfig.ofDefaults();
        private final List<RegistryEventConsumer<Timer>> consumers = new ArrayList<>();

        public Builder withTags(@NonNull Map<String, String> tags) {
            this.tags.putAll(tags);
            return this;
        }

        public Builder withConfigs(@NonNull Map<String, TimerConfig> configs) {
            this.configs.putAll(configs);
            return this;
        }

        public Builder withDefaultConfig(@NonNull TimerConfig defaultConfig) {
            this.defaultConfig = defaultConfig;
            return this;
        }

        public Builder withConsumers(@NonNull List<RegistryEventConsumer<Timer>> registryEventConsumers) {
            this.consumers.addAll(registryEventConsumers);
            return this;
        }

        public Builder withConsumer(@NonNull RegistryEventConsumer<Timer> registryEventConsumer) {
            this.consumers.add(registryEventConsumer);
            return this;
        }

        public TimerRegistry build() {
            configs.remove("default");
            TimerRegistry registry = new InMemoryTimerRegistry(defaultConfig, consumers, tags);
            configs.forEach(registry::addConfiguration);
            return registry;
        }
    }

    @Override
    public Stream<Timer> getAllTimers() {
        return entryMap.values().stream();
    }

    @Override
    public Timer timer(String name, MeterRegistry registry) {
        return timer(name, registry, getDefaultConfig(), emptyMap());
    }

    @Override
    public Timer timer(String name, MeterRegistry registry, Map<String, String> tags) {
        return timer(name, registry, getDefaultConfig(), tags);
    }

    @Override
    public Timer timer(String name, MeterRegistry registry, TimerConfig config) {
        return timer(name, registry, config, emptyMap());
    }

    @Override
    public Timer timer(String name, MeterRegistry registry, TimerConfig timerConfig, Map<String, String> tags) {
        return computeIfAbsent(name, () -> Timer.of(name, registry, timerConfig, getAllTags(tags)));
    }

    @Override
    public Timer timer(String name, MeterRegistry registry, Supplier<TimerConfig> timerConfigSupplier) {
        return timer(name, registry, timerConfigSupplier, emptyMap());
    }

    @Override
    public Timer timer(String name, MeterRegistry registry, Supplier<TimerConfig> timerConfigSupplier, Map<String, String> tags) {
        return computeIfAbsent(name, () -> Timer.of(name, registry, timerConfigSupplier.get(), getAllTags(tags)));
    }

    @Override
    public Timer timer(String name, MeterRegistry registry, String configName) {
        return timer(name, registry, configName, emptyMap());
    }

    @Override
    public Timer timer(String name, MeterRegistry registry, String configName, Map<String, String> tags) {
        TimerConfig config = getConfiguration(configName).orElseThrow(() -> new ConfigurationNotFoundException(configName));
        return timer(name, registry, config, tags);
    }
}
