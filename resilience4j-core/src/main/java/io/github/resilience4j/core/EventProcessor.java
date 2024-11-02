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

import io.github.resilience4j.core.lang.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;

public class EventProcessor<T> implements EventPublisher<T> {

    final Set<EventConsumer<T>> onEventConsumers = new CopyOnWriteArraySet<>();
    final ConcurrentMap<String, Set<EventConsumer<T>>> eventConsumerMap = new ConcurrentHashMap<>();
    private boolean consumerRegistered;

    public boolean hasConsumers() {
        return consumerRegistered;
    }

    private final ReentrantLock lock = new ReentrantLock();

    @SuppressWarnings("unchecked")
    public void registerConsumer(String className, EventConsumer<? extends T> eventConsumer) {
        lock.lock();

        try {
            this.eventConsumerMap.compute(className, (k, consumers) -> {
                if (consumers == null) {
                    consumers = new CopyOnWriteArraySet<>();
                    consumers.add((EventConsumer<T>) eventConsumer);
                    return consumers;
                } else {
                    consumers.add((EventConsumer<T>) eventConsumer);
                    return consumers;
                }
            });
            this.consumerRegistered = true;
        } finally {
            lock.unlock();
        }
    }

    public <E extends T> boolean processEvent(E event) {
        boolean consumed = false;
        if (!onEventConsumers.isEmpty()) {
            for (EventConsumer<T> onEventConsumer : onEventConsumers) {
                onEventConsumer.consumeEvent(event);
            }
            consumed = true;
        }

        if (!eventConsumerMap.isEmpty()) {
            final Set<EventConsumer<T>> consumers = this.eventConsumerMap.get(event.getClass().getName());
            if (consumers != null && !consumers.isEmpty()) {
                for (EventConsumer<T> consumer : consumers) {
                    consumer.consumeEvent(event);
                }
                consumed = true;
            }
        }
        return consumed;
    }

    @Override
    public void onEvent(@Nullable EventConsumer<T> onEventConsumer) {
        lock.lock();

        try {
            this.onEventConsumers.add(onEventConsumer);
            this.consumerRegistered = true;
        } finally {
            lock.unlock();
        }
    }
}
