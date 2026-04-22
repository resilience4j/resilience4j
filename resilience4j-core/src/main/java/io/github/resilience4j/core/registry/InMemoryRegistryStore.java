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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Default Implementation Of RegistryStore using ConcurrentHashMap.
 *
 * <p>Starting with JDK 25 and JEP 491 (Synchronize Virtual Threads without Pinning),
 * virtual threads no longer get pinned when executing synchronized blocks or methods.
 * This means the previous FutureHashMap pattern is no longer necessary, and we can
 * use the simple and straightforward ConcurrentHashMap.computeIfAbsent() method.
 *
 * <p>JEP 491 allows the JVM to release the carrier thread when a virtual thread
 * blocks on a monitor (synchronized), making virtual threads truly efficient
 * for all blocking operations without special workarounds.
 */
public class InMemoryRegistryStore<E> implements RegistryStore<E> {

    private final ConcurrentHashMap<String, E> entryMap;

    public InMemoryRegistryStore() {
        this.entryMap = new ConcurrentHashMap<>();
    }

    @Override
    public E computeIfAbsent(String key,
                             Function<? super String, ? extends E> mappingFunction) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(mappingFunction, "Mapping function cannot be null");

        // With JEP 491 (JDK 25+), synchronized blocks no longer pin virtual threads.
        // Simple computeIfAbsent is now efficient for both platform and virtual threads.
        return entryMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    @Nullable
    public E putIfAbsent(String key, E value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        return entryMap.putIfAbsent(key, value);
    }

    @Override
    public Optional<E> find(String key) {
        return Optional.ofNullable(entryMap.get(key));
    }

    @Override
    public Optional<E> remove(String name) {
        return Optional.ofNullable(entryMap.remove(name));
    }

    @Override
    public Optional<E> replace(String name, E newEntry) {
        return Optional.ofNullable(entryMap.replace(name, newEntry));
    }

    @Override
    public Collection<E> values() {
        return entryMap.values().stream()
            .filter(Objects::nonNull)
            .toList();
    }
}
