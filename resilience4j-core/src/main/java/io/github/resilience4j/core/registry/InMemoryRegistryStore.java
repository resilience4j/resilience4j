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
        
        // TODO: This logic should be simplified to computeIfAbsent when JDK 25 is adopted
        CompletableFuture<E> created = new CompletableFuture<>();
        CompletableFuture<E> future = entryMap.putIfAbsent(key, created);

        if (future == null) { // I am the winner
            future = created;
            try {
                E value = mappingFunction.apply(key);     // ***Compute outside map*** (no locks)
                future.complete(value);
            } catch (Throwable t) {
                // Only cleanup if I'm the first to complete exceptionally → retry possible
                if (future.completeExceptionally(t)) {
                    entryMap.remove(key, future);
                }
                throw t;
            }
        }
        return future.join(); // Losers wait (VT friendly)
    }

    @Override
    public E putIfAbsent(String key, E value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        
        CompletableFuture<E> future = entryMap.putIfAbsent(key, CompletableFuture.completedFuture(value));
        if (isSuccessfullyCompleted(future)) {
            return future.join();
        }
        return null;
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
        if (isSuccessfullyCompleted(future)) {
            return Optional.ofNullable(future.join());
        }
        return Optional.empty();
    }

    @Override
    public Optional<E> replace(String name, E newEntry) {
        CompletableFuture<E> future = entryMap.replace(name, CompletableFuture.completedFuture(newEntry));
        if (isSuccessfullyCompleted(future)) {
            return Optional.ofNullable(future.join());
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
