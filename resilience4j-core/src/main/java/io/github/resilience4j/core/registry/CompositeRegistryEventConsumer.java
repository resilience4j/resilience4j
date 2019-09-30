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

package io.github.resilience4j.core.registry;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class CompositeRegistryEventConsumer<E> implements RegistryEventConsumer<E> {

    private final List<RegistryEventConsumer<E>> delegates;

    public CompositeRegistryEventConsumer(List<RegistryEventConsumer<E>> delegates) {
        this.delegates = new ArrayList<>(requireNonNull(delegates));
    }

    @Override
    public void onEntryAddedEvent(EntryAddedEvent<E> entryAddedEvent) {
        delegates.forEach(consumer -> consumer.onEntryAddedEvent(entryAddedEvent));
    }

    @Override
    public void onEntryRemovedEvent(EntryRemovedEvent<E> entryRemoveEvent) {
        delegates.forEach(consumer -> consumer.onEntryRemovedEvent(entryRemoveEvent));
    }

    @Override
    public void onEntryReplacedEvent(EntryReplacedEvent<E> entryReplacedEvent) {
        delegates.forEach(consumer -> consumer.onEntryReplacedEvent(entryReplacedEvent));
    }

}
