/*
 * Copyright 2017 Dan Maas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Backend retry manager.
 * Constructs backend retries according to configuration values.
 */
public final class InMemoryRetryRegistry implements RetryRegistry {

    private final RetryConfig defaultRetryConfig;

    /**
     * The retries, indexed by name of the backend.
     */
    private final ConcurrentMap<String, Retry> retries;

    /**
     * The constructor with default retry properties.
     */
    public InMemoryRetryRegistry() {
        this.defaultRetryConfig = RetryConfig.ofDefaults();
        this.retries = new ConcurrentHashMap<>();
    }

    /**
     * The constructor with custom default retry properties.
     *
     * @param defaultRetryConfig The BackendMonitor service properties.
     */
    public InMemoryRetryRegistry(RetryConfig defaultRetryConfig) {
        this.defaultRetryConfig = Objects.requireNonNull(defaultRetryConfig, "RetryConfig must not be null");
        this.retries = new ConcurrentHashMap<>();
    }

    @Override
    public Seq<Retry> getAllRetries() {
        return Array.ofAll(retries.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry retry(String name) {
        return retries.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> Retry.of(name,
                defaultRetryConfig));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Retry retry(String name, RetryConfig customRetryConfig) {
        return retries.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> Retry.of(name,
                customRetryConfig));
    }

    @Override
    public Retry retry(String name, Supplier<RetryConfig> retryConfigSupplier) {
        return retries.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> Retry.of(name,
                retryConfigSupplier.get()));
    }
}
