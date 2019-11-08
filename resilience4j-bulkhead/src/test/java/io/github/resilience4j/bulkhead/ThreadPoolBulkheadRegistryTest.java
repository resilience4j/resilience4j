/*
 *
 *  Copyright 2017 Robert Winkler, Mahmoud Romeh
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

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.assertThat;


public class ThreadPoolBulkheadRegistryTest {

    private ThreadPoolBulkheadConfig config;
    private ThreadPoolBulkheadRegistry registry;

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<ThreadPoolBulkhead> ep) {
        return ep instanceof EventProcessor<?> ? Optional.of((EventProcessor<?>) ep)
            : Optional.empty();
    }

    @Before
    public void setUp() {
        registry = ThreadPoolBulkheadRegistry.ofDefaults();
        config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(100)
            .build();
    }

    @Test
    public void shouldReturnCustomConfig() {
        ThreadPoolBulkheadRegistry registry = ThreadPoolBulkheadRegistry.of(config);

        ThreadPoolBulkheadConfig bulkheadConfig = registry.getDefaultConfig();

        assertThat(bulkheadConfig).isSameAs(config);
    }

    @Test
    public void shouldReturnTheCorrectName() {
        ThreadPoolBulkhead bulkhead = registry.bulkhead("test");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getName()).isEqualTo("test");
        assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize())
            .isEqualTo(Runtime.getRuntime().availableProcessors());
    }

    @Test
    public void shouldBeTheSameInstance() {
        ThreadPoolBulkhead bulkhead1 = registry.bulkhead("test", config);
        ThreadPoolBulkhead bulkhead2 = registry.bulkhead("test", config);

        assertThat(bulkhead1).isSameAs(bulkhead2);
        assertThat(registry.getAllBulkheads()).hasSize(1);
    }

    @Test
    public void shouldBeNotTheSameInstance() {
        ThreadPoolBulkhead bulkhead1 = registry.bulkhead("test1");
        ThreadPoolBulkhead bulkhead2 = registry.bulkhead("test2");

        assertThat(bulkhead1).isNotSameAs(bulkhead2);
        assertThat(registry.getAllBulkheads()).hasSize(2);
    }

//	@Test
//	public void tagsOfRegistryAddedToInstance() {
//		ThreadPoolBulkhead retryConfig = ThreadPoolBulkhead.ofDefaults();
//		Map<String, RetryConfig> retryConfigs = Collections.singletonMap("default", retryConfig);
//		io.vavr.collection.Map<String, String> retryTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
//		RetryRegistry retryRegistry = RetryRegistry.of(retryConfigs, retryTags);
//		Retry retry = retryRegistry.retry("testName");
//
//		Assertions.assertThat(retry.getTags()).containsOnlyElementsOf(retryTags);
//	}

    @Test
    public void noTagsByDefault() {
        ThreadPoolBulkhead bulkhead = registry.bulkhead("testName");
        assertThat(bulkhead.getTags()).hasSize(0);
    }

    @Test
    public void tagsAddedToInstance() {
        io.vavr.collection.Map<String, String> bulkheadTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        ThreadPoolBulkhead bulkhead = registry.bulkhead("testName", bulkheadTags);

        assertThat(bulkhead.getTags()).containsOnlyElementsOf(bulkheadTags);
    }

    @Test
    public void tagsOfRetriesShouldNotBeMixed() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.ofDefaults();
        io.vavr.collection.Map<String, String> bulkheadTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        ThreadPoolBulkhead bulkhead = registry.bulkhead("testName", config, bulkheadTags);
        io.vavr.collection.Map<String, String> bulkheadTags2 = io.vavr.collection.HashMap
            .of("key3", "value3", "key4", "value4");
        ThreadPoolBulkhead bulkhead2 = registry.bulkhead("otherTestName", config, bulkheadTags2);

        assertThat(bulkhead.getTags()).containsOnlyElementsOf(bulkheadTags);
        assertThat(bulkhead2.getTags()).containsOnlyElementsOf(bulkheadTags2);
    }

    @Test
    public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        ThreadPoolBulkheadConfig bulkheadConfig = ThreadPoolBulkheadConfig.ofDefaults();
        Map<String, ThreadPoolBulkheadConfig> bulkheadConfigs = Collections
            .singletonMap("default", bulkheadConfig);
        io.vavr.collection.Map<String, String> registryTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        io.vavr.collection.Map<String, String> instanceTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key4", "value4");
        ThreadPoolBulkheadRegistry bulkheadRegistry = ThreadPoolBulkheadRegistry
            .of(bulkheadConfigs, registryTags);
        ThreadPoolBulkhead bulkhead = bulkheadRegistry
            .bulkhead("testName", bulkheadConfig, instanceTags);

        io.vavr.collection.Map<String, String> expectedTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(bulkhead.getTags()).containsOnlyElementsOf(expectedTags);
    }

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
        configs.put("default", ThreadPoolBulkheadConfig.ofDefaults());
        configs.put("custom", ThreadPoolBulkheadConfig.ofDefaults());

        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry
            .of(configs);

        assertThat(threadPoolBulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(threadPoolBulkheadRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
        configs.put("custom", ThreadPoolBulkheadConfig.ofDefaults());

        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry
            .of(configs);

        assertThat(threadPoolBulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(threadPoolBulkheadRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithSingleRegistryEventConsumer() {
        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry =
            ThreadPoolBulkheadRegistry.of(ThreadPoolBulkheadConfig.ofDefaults(),
                new NoOpThreadPoolBulkheadEventConsumer());

        getEventProcessor(threadPoolBulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<ThreadPoolBulkhead>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpThreadPoolBulkheadEventConsumer());
        registryEventConsumers.add(new NoOpThreadPoolBulkheadEventConsumer());

        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry =
            ThreadPoolBulkheadRegistry
                .of(ThreadPoolBulkheadConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(threadPoolBulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
        configs.put("custom", ThreadPoolBulkheadConfig.ofDefaults());

        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry =
            ThreadPoolBulkheadRegistry.of(configs, new NoOpThreadPoolBulkheadEventConsumer());

        getEventProcessor(threadPoolBulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
        configs.put("custom", ThreadPoolBulkheadConfig.ofDefaults());
        List<RegistryEventConsumer<ThreadPoolBulkhead>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpThreadPoolBulkheadEventConsumer());
        registryEventConsumers.add(new NoOpThreadPoolBulkheadEventConsumer());

        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry =
            ThreadPoolBulkheadRegistry.of(configs, registryEventConsumers);

        getEventProcessor(threadPoolBulkheadRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testWithNotExistingConfig() {
        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry
            .ofDefaults();

        assertThatThrownBy(() -> threadPoolBulkheadRegistry.bulkhead("test", "doesNotExist"))
            .isInstanceOf(ConfigurationNotFoundException.class);
    }

    @Test
    public void testAddConfiguration() {
        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry
            .ofDefaults();
        threadPoolBulkheadRegistry
            .addConfiguration("custom", ThreadPoolBulkheadConfig.custom().build());

        assertThat(threadPoolBulkheadRegistry.getDefaultConfig()).isNotNull();
        assertThat(threadPoolBulkheadRegistry.getConfiguration("custom")).isNotNull();
    }

    private static class NoOpThreadPoolBulkheadEventConsumer implements
        RegistryEventConsumer<ThreadPoolBulkhead> {

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<ThreadPoolBulkhead> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<ThreadPoolBulkhead> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(
            EntryReplacedEvent<ThreadPoolBulkhead> entryReplacedEvent) {
        }
    }

}
