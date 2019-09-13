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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.BDDAssertions.assertThat;
import static org.mockito.Mockito.mock;


public class BulkheadRegistryTest {

	private BulkheadConfig config;
	private BulkheadRegistry registry;
	private Logger LOGGER;

	@Before
	public void setUp() {
		LOGGER = mock(Logger.class);
		// registry with default config
		registry = BulkheadRegistry.ofDefaults();
		// registry with custom config
		config = BulkheadConfig.custom()
				.maxConcurrentCalls(100)
				.maxWaitDuration(Duration.ofMillis(50))
				.build();
	}

	@Test
	public void shouldReturnCustomConfig() {
		// give
		BulkheadRegistry registry = BulkheadRegistry.of(config);
		// when
		BulkheadConfig bulkheadConfig = registry.getDefaultConfig();
		// then
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

	private static Optional<EventProcessor<?>> getEventProcessor(Registry.EventPublisher<Bulkhead> eventPublisher) {
		if (eventPublisher instanceof EventProcessor<?>) {
			return Optional.of((EventProcessor<?>) eventPublisher);
		}

		return Optional.empty();
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
