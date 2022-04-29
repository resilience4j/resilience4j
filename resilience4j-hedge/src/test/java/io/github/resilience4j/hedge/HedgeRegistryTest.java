/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

public class HedgeRegistryTest {

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<Hedge> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }

        return Optional.empty();
    }

    @Test
    public void testCreateWithCustomConfig() {
        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredDuration(Duration.ofMillis(500))
            .build();

        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().withDefaultConfig(config).build();

        HedgeConfig hedgeConfig = hedgeRegistry.getDefaultConfig();
        assertThat(hedgeConfig).isSameAs(config);
    }

    @Test
    public void shouldInitRegistryTags() {
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        Map<String, HedgeConfig> hedgeConfigs = Collections
            .singletonMap("default", hedgeConfig);
        HedgeRegistry registry = HedgeRegistry.builder()
            .withConfigs(hedgeConfigs)
            .withConsumer(new NoOpHedgeEventConsumer())
            .withTags(Map.of("Tag1Key", "Tag1Value"))
            .build();
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsAllEntriesOf(Map.of("Tag1Key", "Tag1Value"));
    }

    @Test
    public void noTagsByDefault() {
        Hedge Hedge = HedgeRegistry.builder().build()
            .hedge("testName");
        assertThat(Hedge.getTags()).isEmpty();
    }

    @Test
    public void tagsOfRegistryAddedToInstance() {
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        Map<String, HedgeConfig> hedgeConfigs = Collections
            .singletonMap("default", hedgeConfig);
        Map<String, String> hedgeTags = Map.of("key1", "value1", "key2", "value2");
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().withConfigs(hedgeConfigs).withTags(hedgeTags).build();

        Hedge Hedge = hedgeRegistry.hedge("testName");

        assertThat(Hedge.getTags()).containsAllEntriesOf(hedgeTags);
    }

    @Test
    public void tagsAddedToInstance() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().build();
        Map<String, String> hedgeTags = Map.of("key1", "value1", "key2", "value2");
        Hedge Hedge = hedgeRegistry.hedge("testName", hedgeTags);

        assertThat(Hedge.getTags()).containsAllEntriesOf(hedgeTags);
    }

    @Test
    public void tagsOfHedgesShouldNotBeMixed() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().build();
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        Map<String, String> hedgeTags = Map.of("key1", "value1", "key2", "value2");
        Hedge Hedge = hedgeRegistry
            .hedge("testName", hedgeConfig, hedgeTags);
        Map<String, String> hedgeTags2 = Map.of("key3", "value3", "key4", "value4");
        Hedge Hedge2 = hedgeRegistry.hedge("otherTestName", hedgeConfig, hedgeTags2);

        assertThat(Hedge.getTags()).containsAllEntriesOf(hedgeTags);
        assertThat(Hedge2.getTags()).containsAllEntriesOf(hedgeTags2);
    }

    @Test
    public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        HedgeConfig hedgeConfig = HedgeConfig.ofDefaults();
        Map<String, HedgeConfig> hedgeConfigs = Collections
            .singletonMap("default", hedgeConfig);
        Map<String, String> hedgeTags = Map.of("key1", "value1", "key2", "value2");
        Map<String, String> instanceTags = Map.of("key1", "value3", "key4", "value4");
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder()
            .withConfigs(hedgeConfigs)
            .withTags(hedgeTags)
            .build();
        Hedge hedge = hedgeRegistry.hedge("testName", hedgeConfig, instanceTags);

        Map<String, String> expectedTags = Map.of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(hedge.getTags()).containsAllEntriesOf(expectedTags);
    }

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("default", HedgeConfig.ofDefaults());
        configs.put("custom", HedgeConfig.ofDefaults());

        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().withConfigs(configs).build();

        assertThat(hedgeRegistry.getDefaultConfig()).isNotNull();
        assertThat(hedgeRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("custom", HedgeConfig.ofDefaults());

        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().withConfigs(configs).build();

        assertThat(hedgeRegistry.getDefaultConfig()).isNotNull();
        assertThat(hedgeRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithSingleRegistryEventConsumer() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder()
            .withDefaultConfig(HedgeConfig.ofDefaults())
            .withConsumer(new NoOpHedgeEventConsumer())
            .build();

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<Hedge>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpHedgeEventConsumer());
        registryEventConsumers.add(new NoOpHedgeEventConsumer());

        HedgeRegistry hedgeRegistry = HedgeRegistry.builder()
            .withDefaultConfig(HedgeConfig.ofDefaults())
            .withConsumers(registryEventConsumers)
            .build();

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("custom", HedgeConfig.ofDefaults());

        HedgeRegistry hedgeRegistry = HedgeRegistry.builder()
            .withConfigs(configs)
            .withConsumer(new NoOpHedgeEventConsumer())
            .build();

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, HedgeConfig> configs = new HashMap<>();
        configs.put("custom", HedgeConfig.ofDefaults());

        List<RegistryEventConsumer<Hedge>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpHedgeEventConsumer());
        registryEventConsumers.add(new NoOpHedgeEventConsumer());

        HedgeRegistry hedgeRegistry = HedgeRegistry.builder()
            .withConfigs(configs)
            .withConsumers(registryEventConsumers)
            .build();

        getEventProcessor(hedgeRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testAddConfiguration() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().build();
        hedgeRegistry.addConfiguration("custom", HedgeConfig.custom().build());

        assertThat(hedgeRegistry.getDefaultConfig()).isNotNull();
        assertThat(hedgeRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testWithNotExistingConfig() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().build();

        assertThatThrownBy(() -> hedgeRegistry.hedge("test", "doesNotExist"))
            .isInstanceOf(ConfigurationNotFoundException.class);
    }

    @Test
    public void shouldUseCorrectConfig() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().build();
        HedgeConfig config = HedgeConfig.ofDefaults();

        Hedge hedge = hedgeRegistry.hedge("test", config);

        then(hedge.getName()).isEqualTo("test");
        then(hedge.getHedgeConfig()).isEqualTo(config);
    }

    @Test
    public void shouldUseCorrectConfigFromStringConfigName() {
        HedgeRegistry hedgeRegistry = HedgeRegistry.builder().build();
        HedgeConfig config = HedgeConfig.ofDefaults();
        hedgeRegistry.addConfiguration("test1", config);

        Hedge hedge = hedgeRegistry.hedge("test", "test1");

        then(hedge.getName()).isEqualTo("test");
        then(hedge.getHedgeConfig()).isEqualTo(config);
    }

    private static class NoOpHedgeEventConsumer implements RegistryEventConsumer<Hedge> {

        @Override
        public void onEntryAddedEvent(@NonNull EntryAddedEvent<Hedge> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(@NonNull EntryRemovedEvent<Hedge> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(@NonNull EntryReplacedEvent<Hedge> entryReplacedEvent) {
        }
    }
}
