/*
 *
 *  Copyright 2020: KrnSaurabh
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

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;

import java.util.concurrent.Callable;

public interface VavrCache {

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
    static <K, R> CheckedFunction1<K, R> decorateCheckedSupplier(Cache<K, R> cache,
                                                                 CheckedFunction0<R> supplier) {
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, () -> {
            try {
                return supplier.apply();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
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
    static <K, R> CheckedFunction1<K, R> decorateCallable(Cache<K, R> cache, Callable<R> callable) {
        return (K cacheKey) -> cache.computeIfAbsent(cacheKey, () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
