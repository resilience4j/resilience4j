/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.cache;

import io.github.resilience4j.cache.event.CacheEvent;
import io.github.resilience4j.cache.event.CacheOnErrorEvent;
import io.github.resilience4j.cache.event.CacheOnHitEvent;
import io.github.resilience4j.cache.event.CacheOnMissEvent;
import io.github.resilience4j.cache.internal.CacheImpl;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Cache<K, V> {

    /**
     * Creates a Retry with default configuration.
     *
     * @param cache the wrapped JCache instance
     * @param <K>   the type of key
     * @param <V>   the type of value
     * @return a Cache
     */
    static <K, V> Cache<K, V> of(javax.cache.Cache<K, V> cache) {
        Objects.requireNonNull(cache, "Cache must not be null");
        return new CacheImpl<>(cache);
    }

    /**
     * Creates a functions which returns a value from a cache, if it exists. Otherwise it calls the
     * Supplier.
     *
     * @param cache    the Cache
     * @param supplier the original Supplier
     * @param <K>      the type of key
     * @param <R>      the type of value
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <K, R> CheckedFunction<K, R> decorateCheckedSupplier(Cache<K, R> cache,
                                                                CheckedSupplier<R> supplier) {
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, supplier);
    }

    /**
     * Creates a functions which returns a value from a cache, if it exists. Otherwise it calls the
     * Supplier.
     *
     * @param cache    the Cache
     * @param supplier the original Supplier
     * @param <K>      the type of key
     * @param <R>      the type of value
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <K, R> Function<K, R> decorateSupplier(Cache<K, R> cache, Supplier<R> supplier) {
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, supplier::get);
    }

    /**
     * Creates a functions which returns a value from a cache, if it exists. Otherwise it calls the
     * Callable.
     *
     * @param cache    the Cache
     * @param callable the original Callable
     * @param <K>      the type of key
     * @param <R>      the type of value
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <K, R> CheckedFunction<K, R> decorateCallable(Cache<K, R> cache, Callable<R> callable) {
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, callable::call);
    }

    /**
     * @return the cache name
     */
    String getName();

    /**
     * Returns the Metrics of this Cache.
     *
     * @return the Metrics of this Cache
     */
    Metrics getMetrics();

    /**
     * If the key is not already associated with a cached value, attempts to compute its value using
     * the given supplier and puts it into the cache. Otherwise it returns the cached value. If the
     * function itself throws an (unchecked) exception, the exception is rethrown.
     *
     * @param key      key with which the specified value is to be associated
     * @param supplier value to be associated with the specified key
     * @return cached value
     */
    V computeIfAbsent(K key, CheckedSupplier<V> supplier);

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    interface Metrics {

        /**
         * Returns the current number of cache hits
         *
         * @return the current number of cache hits
         */
        long getNumberOfCacheHits();

        /**
         * Returns the current number of cache misses.
         *
         * @return the current number of cache misses
         */
        long getNumberOfCacheMisses();
    }

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<CacheEvent> {

        EventPublisher onCacheHit(EventConsumer<CacheOnHitEvent> eventConsumer);

        EventPublisher onCacheMiss(EventConsumer<CacheOnMissEvent> eventConsumer);

        EventPublisher onError(EventConsumer<CacheOnErrorEvent> eventConsumer);

    }
}
