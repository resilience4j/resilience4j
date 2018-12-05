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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.AsyncRetryRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;

/**
 * Backend retry manager.
 * Constructs backend retries according to configuration values.
 */
public final class InMemoryAsyncRetryRegistry implements AsyncRetryRegistry {

    private final RetryConfig defaultRetryConfig;

    /**
     * The retries, indexed by name of the backend.
     */
    private final ConcurrentMap<String, AsyncRetry> retries;

    /**
     * The constructor with default retry properties.
     */
    public InMemoryAsyncRetryRegistry() {
        this.defaultRetryConfig = RetryConfig.ofDefaults();
        this.retries = new ConcurrentHashMap<>();
    }

    /**
     * The constructor with custom default retry properties.
     *
     * @param defaultRetryConfig The BackendMonitor service properties.
     */
    public InMemoryAsyncRetryRegistry(RetryConfig defaultRetryConfig) {
        this.defaultRetryConfig = Objects.requireNonNull(defaultRetryConfig, "RetryConfig must not be null");
        this.retries = new ConcurrentHashMap<>();
    }

    @Override
    public Seq<AsyncRetry> getAllRetries() {
        return Array.ofAll(retries.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncRetry retry(String name) {
        return retries.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> AsyncRetry.of(name,
                defaultRetryConfig));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncRetry retry(String name, RetryConfig customRetryConfig) {
        return retries.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> AsyncRetry.of(name,
                customRetryConfig));
    }

    @Override
    public AsyncRetry retry(String name, Supplier<RetryConfig> retryConfigSupplier) {
        return retries.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> AsyncRetry.of(name,
                retryConfigSupplier.get()));
    }
}
