/*
 * Copyright 2020 KrnSaurabh
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

package io.github.resilience4j.core.registry;

import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.lang.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Default Implementation Of RegistryStore using FutureHashMap pattern for virtual thread optimization
 */
public class InMemoryRegistryStore<E> implements RegistryStore<E> {

    // FutureHashMap pattern for virtual thread optimization - avoids blocking inside map operations
    private final ConcurrentHashMap<String, CompletableFuture<E>> entryMap;

    public InMemoryRegistryStore() {
        this.entryMap = new ConcurrentHashMap<>();
    }

    /**
     * Checks if the future is successfully completed (not null, done, and not exceptionally completed)
     */
    private boolean isSuccessfullyCompleted(@Nullable CompletableFuture<E> future) {
        return future != null && future.isDone() && !future.isCompletedExceptionally();
    }

    @Override
    public E computeIfAbsent(String key,
                             Function<? super String, ? extends E> mappingFunction) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(mappingFunction, "Mapping function cannot be null");

        /*
         * FutureHashMap pattern to prevent virtual thread pinning.
         *
         * We intentionally avoid using ConcurrentHashMap.computeIfAbsent() because:
         * 1. mappingFunction.apply() can involve blocking I/O, database calls, or expensive initialization
         * 2. Blocking inside computeIfAbsent holds the segment lock, causing virtual thread pinning
         * 3. This pattern executes expensive operations outside the map lock
         *
         * Pattern explanation:
         * - Winner thread: Creates the entry, executes mappingFunction outside map lock, completes future
         * - Loser threads: Wait on future.join(), which is virtual thread friendly (no carrier thread blocking)
         *
         * This is a performance optimization for virtual thread environments and should NOT be
         * "simplified" to standard computeIfAbsent in future JDK versions, as it would reintroduce
         * the pinning issue.
         */
        CompletableFuture<E> created = new CompletableFuture<>();
        CompletableFuture<E> future = entryMap.putIfAbsent(key, created);

        if (future == null) { // I am the winner
            future = created;
            try {
                E value = mappingFunction.apply(key);     // ***Compute outside map lock*** (no pinning)
                future.complete(value);
            } catch (Throwable t) {
                // Only cleanup if I'm the first to complete exceptionally → retry possible
                if (future.completeExceptionally(t)) {
                    entryMap.remove(key, future);
                }
                throw t;
            }
        }
        return future.join(); // Losers wait (VT friendly - no carrier thread blocking)
    }

    @Override
    @Nullable
    public E putIfAbsent(String key, E value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        CompletableFuture<E> future = entryMap.putIfAbsent(key, CompletableFuture.completedFuture(value));
        if (future != null) {
            try {
                // Wait for computation to complete and return existing value
                // join() uses LockSupport.park() internally - no virtual thread pinning
                return future.join();
            } catch (Exception e) {
                // CompletionException from failed computeIfAbsent - treat as "no valid existing value"
                return null;
            }
        }
        return null;  // Successfully inserted new value
    }

    @Override
    public Optional<E> find(String key) {
        CompletableFuture<E> future = entryMap.get(key);
        if (isSuccessfullyCompleted(future)) {
            return Optional.ofNullable(future.join());
        }
        return Optional.empty();
    }

    @Override
    public Optional<E> remove(String name) {
        CompletableFuture<E> future = entryMap.remove(name);
        if (future != null) {
            try {
                return Optional.ofNullable(future.join());
            } catch (Exception e) {
                // CompletionException from failed computeIfAbsent - treat as "no value"
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<E> replace(String name, E newEntry) {
        CompletableFuture<E> future = entryMap.replace(name, CompletableFuture.completedFuture(newEntry));
        if (future != null) {
            try {
                return Optional.ofNullable(future.join());
            } catch (Exception e) {
                // CompletionException from failed computeIfAbsent - treat as "no value"
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<E> values() {
        return entryMap.values().stream()
            .filter(this::isSuccessfullyCompleted)
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .toList();
    }
}
