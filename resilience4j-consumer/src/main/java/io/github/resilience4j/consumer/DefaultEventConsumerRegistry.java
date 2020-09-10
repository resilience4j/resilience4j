/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.consumer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultEventConsumerRegistry<T> implements EventConsumerRegistry<T> {

    /**
     * The CircularEventConsumers, indexed by name of the backend.
     */
    private final ConcurrentMap<String, CircularEventConsumer<T>> registry;

    /**
     * The constructor with default circuitBreaker properties.
     */
    public DefaultEventConsumerRegistry() {
        this.registry = new ConcurrentHashMap<>();
    }

    @Override
    public CircularEventConsumer<T> createEventConsumer(String id, int bufferSize) {
        CircularEventConsumer<T> eventConsumer = new CircularEventConsumer<>(bufferSize);
        registry.put(id, eventConsumer);
        return eventConsumer;
    }

    @Override
    public CircularEventConsumer<T> getEventConsumer(String id) {
        return registry.get(id);
    }

    @Override
    public List<CircularEventConsumer<T>> getAllEventConsumer() {
        return List.copyOf(registry.values());
    }
}
