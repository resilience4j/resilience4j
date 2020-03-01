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

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Default Implementation Of RegistryStore using ConcurrentHashMap
 */
public class InMemoryRegistryStore<E> implements RegistryStore<E> {

    private final ConcurrentMap<String, E> entryMap;

    public InMemoryRegistryStore() {
        this.entryMap = new ConcurrentHashMap<>();
    }

    @Override
    public E computeIfAbsent(String key,
                             Function<? super String, ? extends E> mappingFunction) {
        return entryMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public E putIfAbsent(String key, E value) { return entryMap.putIfAbsent(key, value); }

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
        return entryMap.values();
    }
}
