/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.BDDAssertions.assertThat;


public class BulkheadRegistryTest {

	private BulkheadConfig config;
	private BulkheadRegistry registry;

	@Before
	public void setUp() {
		registry = BulkheadRegistry.ofDefaults();
		config = BulkheadConfig.custom()
				.maxConcurrentCalls(100)
				.maxWaitDuration(Duration.ofMillis(50))
				.build();
	}

	@Test
	public void shouldReturnCustomConfig() {
		BulkheadRegistry registry = BulkheadRegistry.of(config);

		BulkheadConfig bulkheadConfig = registry.getDefaultConfig();

		assertThat(bulkheadConfig).isSameAs(config);
	}

	@Test
	public void shouldReturnTheCorrectName() {
		Bulkhead bulkhead = registry.bulkhead("test");

		assertThat(bulkhead).isNotNull();
		assertThat(bulkhead.getName()).isEqualTo("test");
		assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(25);
		assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(25);
	}

	@Test
	public void shouldBeTheSameInstance() {
		Bulkhead bulkhead1 = registry.bulkhead("test", config);
		Bulkhead bulkhead2 = registry.bulkhead("test", config);

		assertThat(bulkhead1).isSameAs(bulkhead2);
		assertThat(registry.getAllBulkheads()).hasSize(1);
	}

	@Test
	public void shouldBeNotTheSameInstance() {
		Bulkhead bulkhead1 = registry.bulkhead("test1");
		Bulkhead bulkhead2 = registry.bulkhead("test2");

		assertThat(bulkhead1).isNotSameAs(bulkhead2);
		assertThat(registry.getAllBulkheads()).hasSize(2);
	}

	@Test
	public void noTagsByDefault() {
		Bulkhead retry = registry.bulkhead("testName");
		assertThat(retry.getTags()).hasSize(0);
	}

	@Test
	public void tagsOfRegistryAddedToInstance() {
		BulkheadConfig bulkheadConfig = BulkheadConfig.ofDefaults();
		Map<String, BulkheadConfig> bulkheadConfigs = Collections.singletonMap("default", bulkheadConfig);
		io.vavr.collection.Map<String, String> bulkheadTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfigs, bulkheadTags);
		Bulkhead bulkhead = bulkheadRegistry.bulkhead("testName");

		assertThat(bulkhead.getTags()).containsOnlyElementsOf(bulkheadTags);
	}

	@Test
	public void tagsAddedToInstance() {
		io.vavr.collection.Map<String, String> bulkheadTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		Bulkhead bulkhead = registry.bulkhead("testName", bulkheadTags);

		assertThat(bulkhead.getTags()).containsOnlyElementsOf(bulkheadTags);
	}

	@Test
	public void tagsOfRetriesShouldNotBeMixed() {
		BulkheadConfig config = BulkheadConfig.ofDefaults();
		io.vavr.collection.Map<String, String> bulkheadTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		Bulkhead bulkhead = registry.bulkhead("testName", config, bulkheadTags);
		io.vavr.collection.Map<String, String> bulkheadTags2 = io.vavr.collection.HashMap.of("key3","value3", "key4", "value4");
		Bulkhead bulkhead2 = registry.bulkhead("otherTestName", config, bulkheadTags2);

		Assertions.assertThat(bulkhead.getTags()).containsOnlyElementsOf(bulkheadTags);
		Assertions.assertThat(bulkhead2.getTags()).containsOnlyElementsOf(bulkheadTags2);
	}

	@Test
	public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
		BulkheadConfig bulkheadConfig = BulkheadConfig.ofDefaults();
		Map<String, BulkheadConfig> bulkheadConfigs = Collections.singletonMap("default", bulkheadConfig);
		io.vavr.collection.Map<String, String> registryTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		io.vavr.collection.Map<String, String> instanceTags = io.vavr.collection.HashMap.of("key1","value3", "key4", "value4");
		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfigs, registryTags);
		Bulkhead retry = bulkheadRegistry.bulkhead("testName", bulkheadConfig, instanceTags);

		io.vavr.collection.Map<String, String> expectedTags = io.vavr.collection.HashMap.of("key1","value3", "key2", "value2", "key4", "value4");
		Assertions.assertThat(retry.getTags()).containsOnlyElementsOf(expectedTags);
	}

	@Test
	public void testCreateWithConfigurationMap() {
		Map<String, BulkheadConfig> configs = new HashMap<>();
		configs.put("default", BulkheadConfig.ofDefaults());
		configs.put("custom", BulkheadConfig.ofDefaults());

		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(configs);

		assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testCreateWithConfigurationMapWithoutDefaultConfig() {
		Map<String, BulkheadConfig> configs = new HashMap<>();
		configs.put("custom", BulkheadConfig.ofDefaults());

		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(configs);

		assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testCreateWithSingleRegistryEventConsumer() {
		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(BulkheadConfig.ofDefaults(), new NoOpBulkheadEventConsumer());

		getEventProcessor(bulkheadRegistry.getEventPublisher())
				.ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
	}

	@Test
	public void testCreateWithMultipleRegistryEventConsumer() {
		List<RegistryEventConsumer<Bulkhead>> registryEventConsumers = new ArrayList<>();
		registryEventConsumers.add(new NoOpBulkheadEventConsumer());
		registryEventConsumers.add(new NoOpBulkheadEventConsumer());

		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(BulkheadConfig.ofDefaults(), registryEventConsumers);

		getEventProcessor(bulkheadRegistry.getEventPublisher())
				.ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
	}

	@Test
	public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
		Map<String, BulkheadConfig> configs = new HashMap<>();
		configs.put("custom", BulkheadConfig.ofDefaults());

		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(configs, new NoOpBulkheadEventConsumer());

		getEventProcessor(bulkheadRegistry.getEventPublisher())
				.ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
	}

	@Test
	public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
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
	public void testAddConfiguration() {
		BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
		bulkheadRegistry.addConfiguration("custom", BulkheadConfig.custom().build());

		assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

	private static Optional<EventProcessor<?>> getEventProcessor(Registry.EventPublisher<Bulkhead> ep) {
		return ep instanceof EventProcessor<?> ? Optional.of((EventProcessor<?>) ep) : Optional.empty();
	}

	private static class NoOpBulkheadEventConsumer implements RegistryEventConsumer<Bulkhead> {
		@Override
		public void onEntryAddedEvent(EntryAddedEvent<Bulkhead> entryAddedEvent) { }

		@Override
		public void onEntryRemovedEvent(EntryRemovedEvent<Bulkhead> entryRemoveEvent) { }

		@Override
		public void onEntryReplacedEvent(EntryReplacedEvent<Bulkhead> entryReplacedEvent) { }
	}
}
