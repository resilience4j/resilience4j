/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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
package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.Registry;
import io.github.resilience4j.ratelimiter.internal.InMemoryRefillRateLimiterRegistry;
import io.vavr.collection.Seq;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages all RefillRateLimiter instances.
 */
public interface RefillRateLimiterRegistry extends Registry<RateLimiter, RefillRateLimiterConfig> {

    /**
     * Creates a RefillRateLimiterRegistry with a custom RefillRateLimiter configuration.
     *
     * @param defaultRateLimiterConfig a custom RateLimiter configuration
     * @return a RateLimiterRegistry instance backed by a custom RateLimiter configuration
     */
    static RefillRateLimiterRegistry of(RefillRateLimiterConfig defaultRateLimiterConfig) {
        return new InMemoryRefillRateLimiterRegistry(defaultRateLimiterConfig);
    }

    /**
     * Creates a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     *
     * @param configs a Map of shared RateLimiter configurations
     * @param tags    default tags to add to the registry
     * @return a ThreadPoolBulkheadRegistry with a Map of shared RateLimiter configurations.
     */
    static RefillRateLimiterRegistry of(Map<String, RefillRateLimiterConfig> configs,
                                  io.vavr.collection.Map<String, String> tags) {
        return new InMemoryRefillRateLimiterRegistry(configs, tags);
    }

    /**
     * Returns all managed {@link RateLimiter} instances.
     *
     * @return all managed {@link RateLimiter} instances.
     */
    Seq<RateLimiter> getAllRateLimiters();

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter
     * configuration.
     *
     * @param name the name of the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with the default RateLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the RateLimiter
     * @param tags tags added to the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter
     * configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one with a custom RateLimiter
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @param tags              tags added to the RateLimiter
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RefillRateLimiterConfig} or creates a new one with a custom
     * RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier);

    /**
     * Returns a managed {@link RefillRateLimiterConfig} or creates a new one with a custom
     * RateLimiterConfig configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                      the name of the RateLimiterConfig
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @param tags                      tags added to the RateLimiter
     * @return The {@link RateLimiterConfig}
     */
    RateLimiter rateLimiter(String name, Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier,
                            io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the RateLimiter
     * @param configName the name of the shared configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, String configName);

    /**
     * Returns a managed {@link RateLimiter} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     *
     * @param name       the name of the RateLimiter
     * @param configName the name of the shared configuration
     * @return The {@link RateLimiter}
     */
    RateLimiter rateLimiter(String name, String configName,
                            io.vavr.collection.Map<String, String> tags);


}
