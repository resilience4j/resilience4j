/*
 *
 *  Copyright 2026 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.assertThat;


class BulkheadRegistryTest {

    private BulkheadConfig config;
    private BulkheadRegistry registry;

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<Bulkhead> ep) {
        return ep instanceof EventProcessor<?> ? Optional.of((EventProcessor<?>) ep)
            : Optional.empty();
    }

    @BeforeEach
    void setUp() {
        registry = BulkheadRegistry.ofDefaults();
        config = BulkheadConfig.custom()
            .maxConcurrentCalls(100)
            .maxWaitDuration(Duration.ofMillis(50))
            .build();
    }

    @Test
    void shouldInitRegistryTags() {
        BulkheadRegistry registry = BulkheadRegistry.of(config,Map.of("Tag1Key","Tag1Value"));
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsOnly(Map.entry("Tag1Key","Tag1Value"));
    }

    @Test
    void shouldReturnCustomConfig() {
        BulkheadRegistry registry = BulkheadRegistry.of(config);

        BulkheadConfig bulkheadConfig = registry.getDefaultConfig();

        assertThat(bulkheadConfig).isSameAs(config);
    }

    @Test
    void shouldReturnTheCorrectName() {
        Bulkhead bulkhead = registry.bulkhead("test");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getName()).isEqualTo("test");
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(25);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(25);
    }

    @Test
    void shouldBeTheSameInstance() {
        Bulkhead bulkhead1 = registry.bulkhead("test", config);
        Bulkhead bulkhead2 = registry.bulkhead("test", config);

        assertThat(bulkhead1).isSameAs(bulkhead2);
        assertThat(registry.getAllBulkheads()).hasSize(1);
    }

    @Test
    void shouldBeNotTheSameInstance() {
        Bulkhead bulkhead1 = registry.bulkhead("test1");
        Bulkhead bulkhead2 = registry.bulkhead("test2");

        assertThat(bulkhead1).isNotSameAs(bulkhead2);
        assertThat(registry.getAllBulkheads()).hasSize(2);
    }

    @Test
    void noTagsByDefault() {
        Bulkhead retry = registry.bulkhead("testName");
        assertThat(retry.getTags()).isEmpty();
    }

    @Test
    void tagsOfRegistryAddedToInstance() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.ofDefaults();
        Map<String, BulkheadConfig> bulkheadConfigs = Collections
            .singletonMap("default", bulkheadConfig);
        Map<String, String> bulkheadTags = Map.of("key1", "value1", "key2", "value2");
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfigs, bulkheadTags);
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("testName");

        assertThat(bulkhead.getTags()).containsAllEntriesOf(bulkheadTags);
    }

    @Test
    void tagsAddedToInstance() {
        Map<String, String> bulkheadTags = Map.of("key1", "value1", "key2", "value2");
        Bulkhead bulkhead = registry.bulkhead("testName", bulkheadTags);

        assertThat(bulkhead.getTags()).containsAllEntriesOf(bulkheadTags);
    }

    @Test
    void tagsOfRetriesShouldNotBeMixed() {
        BulkheadConfig config = BulkheadConfig.ofDefaults();
        Map<String, String> bulkheadTags = Map.of("key1", "value1", "key2", "value2");
        Bulkhead bulkhead = registry.bulkhead("testName", config, bulkheadTags);
        Map<String, String> bulkheadTags2 = Map.of("key3", "value3", "key4", "value4");
        Bulkhead bulkhead2 = registry.bulkhead("otherTestName", config, bulkheadTags2);

        Assertions.assertThat(bulkhead.getTags()).containsAllEntriesOf(bulkheadTags);
        Assertions.assertThat(bulkhead2.getTags()).containsAllEntriesOf(bulkheadTags2);
    }

    @Test
    void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.ofDefaults();
        Map<String, BulkheadConfig> bulkheadConfigs = Collections
            .singletonMap("default", bulkheadConfig);
        Map<String, String> registryTags = Map.of("key1", "value1", "key2", "value2");
        Map<String, String> instanceTags = Map.of("key1", "value3", "key4", "value4");
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfigs, registryTags);
        Bulkhead retry = bulkheadRegistry.bulkhead("testName", bulkheadConfig, instanceTags);

        Map<String, String> expectedTags = Map.of("key1", "value3", "key2", "value2", "key4", "value4");
        Assertions.assertThat(retry.getTags()).containsAllEntriesOf(expectedTags);
    }

    @Test
    void createWithConfigurationMap() {
        Map<String, BulkheadConfig> configs = new HashMap<>();
        configs.put("default", BulkheadConfig.ofDefaults());
        configs.put("custom", BulkheadConfig.ofDefaults());

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(configs);

        assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    void createWithConfigurationMapWithoutDefaultConfig() {
        Map<String, BulkheadConfig> configs = new HashMap<>();
        configs.put("custom", BulkheadConfig.ofDefaults());

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(configs);

        assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    void createWithSingleRegistryEventConsumer() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry
            .of(BulkheadConfig.ofDefaults(), new NoOpBulkheadEventConsumer());

        getEventProcessor(bulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpBulkheadEventConsumer());
        registryEventConsumers.add(new NoOpBulkheadEventConsumer());

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry
            .of(BulkheadConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(bulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, BulkheadConfig> configs = new HashMap<>();
        configs.put("custom", BulkheadConfig.ofDefaults());

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry
            .of(configs, new NoOpBulkheadEventConsumer());

        getEventProcessor(bulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, BulkheadConfig> configs = new HashMap<>();
        configs.put("custom", BulkheadConfig.ofDefaults());

        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpBulkheadEventConsumer());
        registryEventConsumers.add(new NoOpBulkheadEventConsumer());

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(configs, registryEventConsumers);

        getEventProcessor(bulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void addConfiguration() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        bulkheadRegistry.addConfiguration("custom", BulkheadConfig.custom().build());

        assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
    }

    private static class NoOpBulkheadEventConsumer implements RegistryEventConsumer<Bulkhead> {

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<Bulkhead> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<Bulkhead> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<Bulkhead> entryReplacedEvent) {
        }
    }

    @Test
    void createUsingBuilderWithDefaultConfig() {
        BulkheadRegistry bulkheadRegistry =
            BulkheadRegistry.custom().withBulkheadConfig(BulkheadConfig.ofDefaults()).build();
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("testName");
        Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("otherTestName");
        assertThat(bulkhead).isNotSameAs(bulkhead2);

        assertThat(bulkheadRegistry.getAllBulkheads()).hasSize(2);
    }

    @Test
    void createUsingBuilderWithCustomConfig() {
        int maxConcurrentCalls = 100;
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrentCalls).build();

        BulkheadRegistry bulkheadRegistry =
            BulkheadRegistry.custom().withBulkheadConfig(bulkheadConfig).build();
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("testName");

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls())
            .isEqualTo(maxConcurrentCalls);
    }

    @Test
    void createUsingBuilderWithoutDefaultConfig() {
        int maxConcurrentCalls = 100;
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrentCalls).build();

        BulkheadRegistry bulkheadRegistry =
            BulkheadRegistry.custom().addBulkheadConfig("someSharedConfig", bulkheadConfig).build();

        assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(bulkheadRegistry.getDefaultConfig().getMaxConcurrentCalls())
            .isEqualTo(25);
        assertThat(bulkheadRegistry.getConfiguration("someSharedConfig")).isPresent();

        Bulkhead bulkhead = bulkheadRegistry
            .bulkhead("name", "someSharedConfig");

        assertThat(bulkhead.getBulkheadConfig()).isEqualTo(bulkheadConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls())
            .isEqualTo(maxConcurrentCalls);
    }

    @Test
    void addMultipleDefaultConfigUsingBuilderShouldThrowException() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(100).build();
        assertThatThrownBy(() -> BulkheadRegistry.custom().addBulkheadConfig("default", bulkheadConfig).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createUsingBuilderWithDefaultAndCustomConfig() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(100).build();
        BulkheadConfig customBulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(200).build();

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.custom()
            .withBulkheadConfig(bulkheadConfig)
            .addBulkheadConfig("custom", customBulkheadConfig)
            .build();

        assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(bulkheadRegistry.getDefaultConfig().getMaxConcurrentCalls())
            .isEqualTo(100);
        assertThat(bulkheadRegistry.getConfiguration("custom")).isPresent();
    }

    @Test
    void createUsingBuilderWithNullConfig() {
        assertThatThrownBy(
            () -> BulkheadRegistry.custom().withBulkheadConfig(null).build())
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    void createUsingBuilderWithMultipleRegistryEventConsumer() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.custom()
            .withBulkheadConfig(BulkheadConfig.ofDefaults())
            .addRegistryEventConsumer(new NoOpBulkheadEventConsumer())
            .addRegistryEventConsumer(new NoOpBulkheadEventConsumer())
            .build();

        getEventProcessor(bulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createUsingBuilderWithRegistryTags() {
        Map<String, String> bulkheadTags = Map
            .of("key1", "value1", "key2", "value2");
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.custom()
            .withBulkheadConfig(BulkheadConfig.ofDefaults())
            .withTags(bulkheadTags)
            .build();
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("testName");

        assertThat(bulkhead.getTags()).containsAllEntriesOf(bulkheadTags);
    }

    @Test
    void createUsingBuilderWithRegistryStore() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.custom()
            .withBulkheadConfig(BulkheadConfig.ofDefaults())
            .withRegistryStore(new InMemoryRegistryStore<>())
            .build();
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("testName");
        Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("otherTestName");

        assertThat(bulkhead).isNotSameAs(bulkhead2);
        assertThat(bulkheadRegistry.getAllBulkheads()).hasSize(2);
    }
}
