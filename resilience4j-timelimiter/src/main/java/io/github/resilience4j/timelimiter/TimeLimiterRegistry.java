/*
 *
 *  Copyright 2019 authors
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
package io.github.resilience4j.timelimiter;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.timelimiter.internal.InMemoryTimeLimiterRegistry;
import io.vavr.collection.Seq;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages all TimeLimiter instances.
 */
public interface TimeLimiterRegistry extends Registry<TimeLimiter, TimeLimiterConfig> {

    /**
     * Returns all managed {@link TimeLimiter} instances.
     *
     * @return all managed {@link TimeLimiter} instances.
     */
    Seq<TimeLimiter> getAllTimeLimiters();

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with the default TimeLimiter configuration.
     *
     * @param name the name of the TimeLimiter
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with a custom TimeLimiter configuration.
     *
     * @param name              the name of the TimeLimiter
     * @param timeLimiterConfig a custom TimeLimiter configuration
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name, TimeLimiterConfig timeLimiterConfig);

    /**
     * Returns a managed {@link TimeLimiterConfig} or creates a new one with a custom TimeLimiterConfig configuration.
     *
     * @param name                      the name of the TimeLimiterConfig
     * @param timeLimiterConfigSupplier a supplier of a custom TimeLimiterConfig configuration
     * @return The {@link TimeLimiterConfig}
     */
    TimeLimiter timeLimiter(String name, Supplier<TimeLimiterConfig> timeLimiterConfigSupplier);

    /**
     * Returns a managed {@link TimeLimiter} or creates a new one with a custom TimeLimiter configuration.
     *
     * @param name       the name of the TimeLimiter
     * @param configName a custom TimeLimiter configuration name
     * @return The {@link TimeLimiter}
     */
    TimeLimiter timeLimiter(String name, String configName);

    /**
     * Creates a TimeLimiterRegistry with a custom TimeLimiter configuration.
     *
     * @param defaultTimeLimiterConfig a custom TimeLimiter configuration
     * @return a TimeLimiterRegistry instance backed by a custom TimeLimiter configuration
     */
    static TimeLimiterRegistry of(TimeLimiterConfig defaultTimeLimiterConfig) {
        return new InMemoryTimeLimiterRegistry(defaultTimeLimiterConfig);
    }

    /**
     * Returns a managed {@link TimeLimiterConfig} or creates a new one with a default TimeLimiter configuration.
     *
     * @return The {@link TimeLimiterConfig}
     */
    static TimeLimiterRegistry ofDefaults() {
        return new InMemoryTimeLimiterRegistry(TimeLimiterConfig.ofDefaults());
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared TimeLimiter configurations.
     *
     * @param configs a Map of shared TimeLimiter configurations
     * @return a ThreadPoolBulkheadRegistry with a Map of shared TimeLimiter configurations.
     */
    static TimeLimiterRegistry of(Map<String, TimeLimiterConfig> configs) {
        return new InMemoryTimeLimiterRegistry(configs);
    }
}
