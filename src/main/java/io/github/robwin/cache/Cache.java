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
package io.github.robwin.cache;

import io.github.robwin.cache.event.CacheEvent;
import io.github.robwin.cache.internal.CacheContext;
import io.reactivex.Flowable;
import javaslang.control.Try;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Cache<K, V>  {

    /**
     * Return the cache name.
     */
    String getName();

    /**
     * If the key is not already associated with a cached value, attempts to compute its value using the
     * given supplier and puts it into the cache. Otherwise it returns the cached value.
     * If the function itself throws an (unchecked) exception, the exception is rethrown.
     *
     * @param key   key with which the specified value is to be associated
     * @param supplier value to be associated with the specified key
     */
    V computeIfAbsent(K key, Try.CheckedSupplier<V> supplier);

    /**
     * Returns a reactive stream of CacheEvents.
     *
     * @return a reactive stream of CacheEvents
     */
    Flowable<CacheEvent> getEventStream();

    /**
     * Creates a Retry with default configuration.
     *
     * @param cache the wrapped JCache instance
     * @return a Cache
     */
    static <K,V> Cache<K,V> of(javax.cache.Cache<K, V> cache){
        Objects.requireNonNull(cache, "Cache must not be null");
        return new CacheContext<>(cache);
    }

    /**
     * Creates a functions which returns a value from a cache, if it exists.
     * Otherwise it calls the Supplier.
     *
     * @param cache the Cache
     * @param supplier the original Supplier
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <K, R> Try.CheckedFunction<K, R> decorateCheckedSupplier(Cache<K, R> cache, Try.CheckedSupplier<R> supplier){
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, supplier);
    }

    /**
     * Creates a functions which returns a value from a cache, if it exists.
     * Otherwise it calls the Supplier.
     *
     * @param cache the Cache
     * @param supplier the original Supplier
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <K, R> Function<K, R> decorateSupplier(Cache<K, R> cache, Supplier<R> supplier){
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, supplier::get);
    }

    /**
     * Creates a functions which returns a value from a cache, if it exists.
     * Otherwise it calls the Callable.
     *
     * @param cache the Cache
     * @param callable the original Callable
     *
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <K, R> Try.CheckedFunction<K, R> decorateCallable(Cache<K, R> cache, Callable<R> callable){
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, callable::call);
    }
}
