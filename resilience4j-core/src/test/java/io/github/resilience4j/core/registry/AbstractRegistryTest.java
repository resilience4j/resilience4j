/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.core.registry;

import io.github.resilience4j.core.RegistryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.core.registry.RegistryEvent.Type;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class AbstractRegistryTest {

    @Test
    void shouldInitRegistryTags(){
        TestRegistry testRegistry = new TestRegistry(Map.of("Tag1","Tag1Value"));
        assertThat(testRegistry.getTags()).isNotEmpty();
        assertThat(testRegistry.getTags()).containsOnly(Map.entry("Tag1","Tag1Value"));
    }


    @Test
    void shouldContainDefaultAndCustomConfiguration() {
        TestRegistry testRegistry = new TestRegistry();
        testRegistry.addConfiguration("custom", "test");
        assertThat(testRegistry.getConfiguration("custom")).hasValue("test");
        assertThat(testRegistry.getDefaultConfig()).isEqualTo("default");
    }

    @Test
    void shouldNotAllowToOverwriteDefaultConfiguration() {
        TestRegistry testRegistry = new TestRegistry();

        assertThatThrownBy(() -> testRegistry.addConfiguration("default", "test"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRemoveCustomConfiguration() {
        TestRegistry testRegistry = new TestRegistry();
        testRegistry.addConfiguration("customRemovable", "test");
        testRegistry.removeConfiguration("customRemovable");
        Assertions.assertThat(testRegistry.getConfiguration("customRemovable")).isEmpty();
    }

    @Test
    void shouldNotAllowToRemoveDefaultConfiguration() {
        TestRegistry testRegistry = new TestRegistry();

        assertThatThrownBy(() -> testRegistry.removeConfiguration("default"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldConsumeRegistryEvents() {
        List<RegistryEvent> consumedEvents = new ArrayList<>();
        List<EntryAddedEvent<String>> addedEvents = new ArrayList<>();
        List<EntryRemovedEvent<String>> removedEvents = new ArrayList<>();
        List<EntryReplacedEvent<String>> replacedEvents = new ArrayList<>();

        TestRegistry testRegistry = new TestRegistry();
        testRegistry.getEventPublisher().onEvent(consumedEvents::add);
        testRegistry.getEventPublisher().onEntryAdded(addedEvents::add);
        testRegistry.getEventPublisher().onEntryRemoved(removedEvents::add);
        testRegistry.getEventPublisher().onEntryReplaced(replacedEvents::add);

        String addedEntry1 = testRegistry.computeIfAbsent("name", () -> "entry1");
        assertThat(addedEntry1).isEqualTo("entry1");

        String addedEntry2 = testRegistry.computeIfAbsent("name2", () -> "entry2");
        assertThat(addedEntry2).isEqualTo("entry2");

        Optional<String> removedEntry = testRegistry.remove("name");
        assertThat(removedEntry).hasValue("entry1");

        Optional<String> replacedEntry = testRegistry.replace("name2", "entry3");
        assertThat(replacedEntry).hasValue("entry2");

        assertThat(consumedEvents).hasSize(4);
        assertThat(addedEvents).hasSize(2);
        assertThat(removedEvents).hasSize(1);
        assertThat(replacedEvents).hasSize(1);

        assertThat(consumedEvents).extracting("eventType")
            .containsExactly(Type.ADDED, Type.ADDED, Type.REMOVED, Type.REPLACED);

        assertThat(addedEvents).extracting("addedEntry")
            .containsExactly("entry1", "entry2");

        assertThat(removedEvents).extracting("removedEntry")
            .containsExactly("entry1");

        assertThat(replacedEvents).extracting("oldEntry")
            .containsExactly("entry2");

        assertThat(replacedEvents).extracting("newEntry")
            .containsExactly("entry3");

    }

    @Test
    void createWithRegistryEventConsumer() {
        List<EntryAddedEvent<String>> addedEvents = new ArrayList<>();
        List<EntryRemovedEvent<String>> removedEvents = new ArrayList<>();
        List<EntryReplacedEvent<String>> replacedEvents = new ArrayList<>();

        RegistryEventConsumer<String> registryEventConsumer = new RegistryEventConsumer<String>() {
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
        };

        TestRegistry testRegistry = new TestRegistry(registryEventConsumer);

        String addedEntry1 = testRegistry.computeIfAbsent("name", () -> "entry1");
        assertThat(addedEntry1).isEqualTo("entry1");

        String addedEntry2 = testRegistry.computeIfAbsent("name2", () -> "entry2");
        assertThat(addedEntry2).isEqualTo("entry2");

        Optional<String> removedEntry = testRegistry.remove("name");
        assertThat(removedEntry).hasValue("entry1");

        Optional<String> replacedEntry = testRegistry.replace("name2", "entry3");
        assertThat(replacedEntry).hasValue("entry2");

        assertThat(addedEvents).hasSize(2);
        assertThat(removedEvents).hasSize(1);
        assertThat(replacedEvents).hasSize(1);

        assertThat(addedEvents).extracting("addedEntry")
            .containsExactly("entry1", "entry2");

        assertThat(removedEvents).extracting("removedEntry")
            .containsExactly("entry1");

        assertThat(replacedEvents).extracting("oldEntry")
            .containsExactly("entry2");

        assertThat(replacedEvents).extracting("newEntry")
            .containsExactly("entry3");
    }

    @Test
    void shouldOnlyFindRegisteredObjects() {
        TestRegistry testRegistry = new TestRegistry();

        assertThat(testRegistry.find("test")).isEmpty();
        testRegistry.entryMap.putIfAbsent("test", "value");
        assertThat(testRegistry.find("test")).hasValue("value");
    }

    static class TestRegistry extends AbstractRegistry<String, String> {

        TestRegistry() {
            super("default");
            this.configurations.put(DEFAULT_CONFIG, "default");
        }

        TestRegistry(RegistryEventConsumer<String> registryEventConsumer) {
            super("default", registryEventConsumer);
            this.configurations.put(DEFAULT_CONFIG, "default");
        }

        TestRegistry(Map<String,String> tags) {
            super("default", tags);
            this.configurations.put(DEFAULT_CONFIG, "default");
        }

        TestRegistry(List<RegistryEventConsumer<String>> registryEventConsumer, Map<String,String> tags, RegistryStore<String> registryStore) {
            super("default", registryEventConsumer, tags, registryStore);
            this.configurations.put(DEFAULT_CONFIG, "default");
        }
    }

    @Test
    void shouldCreateRegistryWithRegistryStore() {
        List<EntryAddedEvent<String>> addedEvents = new ArrayList<>();
        List<EntryRemovedEvent<String>> removedEvents = new ArrayList<>();
        List<EntryReplacedEvent<String>> replacedEvents = new ArrayList<>();
        RegistryEventConsumer<String> registryEventConsumer = new RegistryEventConsumer<String>() {
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
        };
        List<RegistryEventConsumer<String>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(registryEventConsumer);
        TestRegistry testRegistry  = new TestRegistry(
            registryEventConsumers, Map.of("Tag1","Tag1Value"), new InMemoryRegistryStore<>());

        assertThat(testRegistry.getDefaultConfig()).as("Wrong Value").isEqualTo("default");
        assertThat(testRegistry.getDefaultConfig()).isEqualTo("default");
        assertThat(testRegistry.getTags()).isNotEmpty();
        assertThat(testRegistry.getTags()).containsOnly(Map.entry("Tag1","Tag1Value"));

        String addedEntry1 = testRegistry.computeIfAbsent("name", () -> "entry1");
        assertThat(addedEntry1).isEqualTo("entry1");

        String addedEntry2 = testRegistry.computeIfAbsent("name2", () -> "entry2");
        assertThat(addedEntry2).isEqualTo("entry2");

        Optional<String> removedEntry = testRegistry.remove("name");
        assertThat(removedEntry).hasValue("entry1");

        Optional<String> replacedEntry = testRegistry.replace("name2", "entry3");
        assertThat(replacedEntry).hasValue("entry2");

        assertThat(addedEvents).hasSize(2);
        assertThat(removedEvents).hasSize(1);
        assertThat(replacedEvents).hasSize(1);
    }
}
