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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CompositeRegistryEventConsumerTest {

    @Test
    public void testCompositeRegistryEventConsumer() {
        List<RegistryEventConsumer<String>> consumers = new ArrayList<>();
        TestRegistryEventConsumer registryEventConsumer1 = new TestRegistryEventConsumer();
        TestRegistryEventConsumer registryEventConsumer2 = new TestRegistryEventConsumer();
        consumers.add(registryEventConsumer1);
        consumers.add(registryEventConsumer2);

        CompositeRegistryEventConsumer<String> compositeRegistryEventConsumer = new CompositeRegistryEventConsumer<>(
            consumers);

        TestRegistry testRegistry = new TestRegistry(compositeRegistryEventConsumer);

        String addedEntry1 = testRegistry.computeIfAbsent("name", () -> "entry1");
        assertThat(addedEntry1).isEqualTo("entry1");

        String addedEntry2 = testRegistry.computeIfAbsent("name2", () -> "entry2");
        assertThat(addedEntry2).isEqualTo("entry2");

        Optional<String> removedEntry = testRegistry.remove("name");
        assertThat(removedEntry).isNotEmpty().hasValue("entry1");

        Optional<String> replacedEntry = testRegistry.replace("name2", "entry3");
        assertThat(replacedEntry).isNotEmpty().hasValue("entry2");

        assertConsumer(registryEventConsumer1);
        assertConsumer(registryEventConsumer2);
    }

    public void assertConsumer(TestRegistryEventConsumer consumer) {
        assertThat(consumer.addedEvents).hasSize(2);
        assertThat(consumer.removedEvents).hasSize(1);
        assertThat(consumer.replacedEvents).hasSize(1);

        assertThat(consumer.addedEvents).extracting("addedEntry")
            .containsExactly("entry1", "entry2");

        assertThat(consumer.removedEvents).extracting("removedEntry")
            .containsExactly("entry1");

        assertThat(consumer.replacedEvents).extracting("oldEntry")
            .containsExactly("entry2");

        assertThat(consumer.replacedEvents).extracting("newEntry")
            .containsExactly("entry3");
    }

    private static class TestRegistry extends AbstractRegistry<String, String> {

        TestRegistry(RegistryEventConsumer<String> registryEventConsumer) {
            super("default", registryEventConsumer);
            this.configurations.put(DEFAULT_CONFIG, "default");
        }
    }

    static class TestRegistryEventConsumer implements RegistryEventConsumer<String> {

        List<EntryAddedEvent<String>> addedEvents = new ArrayList<>();
        List<EntryRemovedEvent<String>> removedEvents = new ArrayList<>();
        List<EntryReplacedEvent<String>> replacedEvents = new ArrayList<>();

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<String> entryAddedEvent) {
            addedEvents.add(entryAddedEvent);
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<String> entryRemoveEvent) {
            removedEvents.add(entryRemoveEvent);
        }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<String> entryReplacedEvent) {
            replacedEvents.add(entryReplacedEvent);
        }

    }
}