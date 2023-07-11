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
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerConfig;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNullElse;

public class InMemoryTimerRegistry extends AbstractRegistry<Timer, TimerConfig> implements TimerRegistry {

    public InMemoryTimerRegistry(TimerConfig defaultConfig, Map<String, TimerConfig> configs, List<RegistryEventConsumer<Timer>> registryEventConsumers, Map<String, String> tags) {
        super(defaultConfig, registryEventConsumers, tags);
        requireNonNullElse(configs, Collections.<String, TimerConfig>emptyMap()).entrySet().stream()
                .filter(entry -> !entry.getKey().equals(DEFAULT_CONFIG))
                .forEach(entry -> addConfiguration(entry.getKey(), entry.getValue()));
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
