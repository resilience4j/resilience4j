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
package io.github.resilience4j.micrometer;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.micrometer.internal.InMemoryTimerRegistry;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Manages all Timer instances.
 */
public interface TimerRegistry extends Registry<Timer, TimerConfig> {

    /**
     * Gets a registry builder.
     *
     * @return a builder for building {@link InMemoryTimerRegistry} instances.
     */
    static InMemoryTimerRegistry.Builder builder() {
        return new InMemoryTimerRegistry.Builder();
    }

    /**
     * Returns all managed {@link Timer} instances.
     *
     * @return all managed {@link Timer} instances.
     */
    Stream<Timer> getAllTimers();

    /**
     * Returns a managed {@link Timer} or creates a new one with the default Timer
     * configuration.
     *
     * @param name     the name of the Timer
     * @param registry the registry to bind Timer to
     * @return The {@link Timer}
     */
    Timer timer(String name, MeterRegistry registry);

    /**
     * Returns a managed {@link Timer} or creates a new one with the default Timer
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name     the name of the Timer
     * @param registry the registry to bind Timer to
     * @param tags     tags added to the Timer
     * @return The {@link Timer}
     */
    Timer timer(String name, MeterRegistry registry, Map<String, String> tags);

    /**
     * Returns a managed {@link Timer} or creates a new one with a custom Timer
     * configuration.
     *
     * @param name        the name of the Timer
     * @param registry    the registry to bind Timer to
     * @param timerConfig a custom Timer configuration
     * @return The {@link Timer}
     */
    Timer timer(String name, MeterRegistry registry, TimerConfig timerConfig);

    /**
     * Returns a managed {@link Timer} or creates a new one with a custom Timer
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name        the name of the Timer
     * @param registry    the registry to bind Timer to
     * @param timerConfig a custom Timer configuration
     * @param tags        tags added to the Timer
     * @return The {@link Timer}
     */
    Timer timer(String name, MeterRegistry registry, TimerConfig timerConfig, Map<String, String> tags);

    /**
     * Returns a managed {@link TimerConfig} or creates a new one with a custom
     * TimerConfig configuration.
     *
     * @param name                the name of the TimerConfig
     * @param registry            the registry to bind Timer to
     * @param timerConfigSupplier a supplier of a custom TimerConfig configuration
     * @return The {@link TimerConfig}
     */
    Timer timer(String name, MeterRegistry registry, Supplier<TimerConfig> timerConfigSupplier);

    /**
     * Returns a managed {@link Timer} or creates a new one with a custom Timer
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                the name of the Timer
     * @param registry            the registry to bind Timer to
     * @param timerConfigSupplier a supplier of a custom Timer configuration
     * @param tags                tags added to the Timer
     * @return The {@link Timer}
     */
    Timer timer(String name, MeterRegistry registry, Supplier<TimerConfig> timerConfigSupplier, Map<String, String> tags);

    /**
     * Returns a managed {@link Timer} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the Timer
     * @param registry   the registry to bind Timer to
     * @param configName the name of the shared configuration
     * @return The {@link Timer}
     */
    Timer timer(String name, MeterRegistry registry, String configName);

    /**
     * Returns a managed {@link Timer} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name       the name of the Timer
     * @param registry   the registry to bind Timer to
     * @param configName the name of the shared configuration
     * @param tags       tags added to the Timer
     * @return The {@link Timer}
     */
    Timer timer(String name, MeterRegistry registry, String configName, Map<String, String> tags);
}
