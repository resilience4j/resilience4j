/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import javaslang.collection.Array;
import javaslang.collection.Seq;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bulkhead instance manager;
 * Constructs/returns bulkhead instances.
 */
public final class InMemoryBulkheadRegistry implements BulkheadRegistry {

    /**
     * The bulkheads, indexed by name
     */
    private final ConcurrentMap<String, Bulkhead> bulkheads;

    public InMemoryBulkheadRegistry() {
        this.bulkheads = new ConcurrentHashMap<>();
    }

    @Override
    public Seq<Bulkhead> getAllBulkheads() {
        return Array.ofAll(bulkheads.values());
    }

    @Override
    public Bulkhead bulkhead(String name, int depth) {
        return bulkheads.computeIfAbsent(
                   Objects.requireNonNull(name, "Name must not be null"),
                   (k) -> Bulkhead.of(name, depth)
               );
    }

}
