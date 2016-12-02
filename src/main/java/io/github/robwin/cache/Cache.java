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
import io.reactivex.Flowable;
import javaslang.control.Option;
import javaslang.control.Try;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

interface Cache<K, V>  {

    /**
     * Tries to determine if the Cache contains an entry for the specified key.
     * <p>
     * More formally, returns <tt>true</tt> if and only if this cache contains a
     * mapping for a key <tt>k</tt> such that <tt>key.equals(k)</tt>.
     *
     * Returns <tt>false</tt> if an exception occurs.
     *
     * @param key key whose presence in this cache is to be tested.
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     */
    boolean containsKey(K key);

    /**
     * Tries to associate the specified value with the specified key in the cache.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    void put(K key, V value);

    /**
     * Tries to get an entry from the cache.
     * Returns <tt>null</tt> if an exception occurs.
     *
     * @param key the key whose associated value is to be returned
     * @return the element, or null, if it does not exist or an exception occurs.
     */
    Option<V> get(K key);


    /**
     * Returns a reactive stream of CacheEvents.
     *
     * @return a reactive stream of CacheEvents
     */
    Flowable<CacheEvent> getEventStream();

    /**
     * Creates a Retry with default configuration.
     *
     * @return a Retry with default configuration
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
        return (K cacheKey) -> {
            if(cache.containsKey(cacheKey)){
                return cache.get(cacheKey)
                        .getOrElseTry(supplier);
            } else{
                R value = supplier.get();
                cache.put(cacheKey, value);
                return value;
            }
        };
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
        return (K cacheKey) -> {
            if(cache.containsKey(cacheKey)){
                return cache.get(cacheKey)
                        .getOrElse(supplier);
            } else{
                R value = supplier.get();
                cache.put(cacheKey, value);
                return value;
            }
        };
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
        return (K cacheKey) -> {
            if(cache.containsKey(cacheKey)){
                return cache.get(cacheKey)
                        .getOrElseTry(callable::call);
            } else{
                R value = callable.call();
                cache.put(cacheKey, value);
                return value;
            }
        };
    }
}
