/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.core.metrics;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

public interface MetricsPublisher<E> extends RegistryEventConsumer<E> {

    void publishMetrics(E entry);

    void removeMetrics(E entry);

    @Override
    default void onEntryAddedEvent(EntryAddedEvent<E> entryAddedEvent) {
        publishMetrics(entryAddedEvent.getAddedEntry());
    }

    @Override
    default void onEntryRemovedEvent(EntryRemovedEvent<E> entryRemoveEvent) {
        removeMetrics(entryRemoveEvent.getRemovedEntry());
    }

    @Override
    default void onEntryReplacedEvent(EntryReplacedEvent<E> entryReplacedEvent) {
        removeMetrics(entryReplacedEvent.getOldEntry());
        publishMetrics(entryReplacedEvent.getNewEntry());
    }

}
