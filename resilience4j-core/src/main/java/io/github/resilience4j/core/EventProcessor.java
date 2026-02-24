/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.core;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class EventProcessor<T> implements EventPublisher<T> {

    final Set<EventConsumer<T>> onEventConsumers = new CopyOnWriteArraySet<>();
    final ConcurrentHashMap<String, CopyOnWriteArraySet<EventConsumer<T>>> eventConsumerMap = new ConcurrentHashMap<>();

    /**
     * Checks if any consumers are currently registered.
     * <p>This method directly checks the actual consumer collections to provide
     * accurate information about the current state, rather than relying on a flag
     * that might become stale if consumers were ever to be removed in the future.
     *
     * @return true if at least one consumer is registered
     */
    public boolean hasConsumers() {
        return !onEventConsumers.isEmpty() || !eventConsumerMap.isEmpty();
    }

    /**
     * Registers a consumer for events of the given class name.
     *
     * @param className     the fully qualified class name of the event type
     * @param eventConsumer the event consumer to register
     * @throws NullPointerException if {@code className} or {@code eventConsumer} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public void registerConsumer(String className, EventConsumer<? extends T> eventConsumer) {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(eventConsumer, "eventConsumer");

        CopyOnWriteArraySet<EventConsumer<T>> set =
            eventConsumerMap.computeIfAbsent(className, k -> new CopyOnWriteArraySet<>());

        set.add((EventConsumer<T>) eventConsumer);
    }

    public <E extends T> boolean processEvent(E event) {
        boolean consumed = false;

        if (!onEventConsumers.isEmpty()) {
            for (EventConsumer<T> c : onEventConsumers) c.consumeEvent(event);
            consumed = true;
        }

        CopyOnWriteArraySet<EventConsumer<T>> set = eventConsumerMap.get(event.getClass().getName());
        if (set != null && !set.isEmpty()) {
            for (EventConsumer<T> c : set) c.consumeEvent(event);
            consumed = true;
        }
        return consumed;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code onEventConsumer} is {@code null}
     */
    @Override
    public void onEvent(EventConsumer<T> onEventConsumer) {
        Objects.requireNonNull(onEventConsumer, "eventConsumer must not be null");
        onEventConsumers.add(onEventConsumer);
    }
}
