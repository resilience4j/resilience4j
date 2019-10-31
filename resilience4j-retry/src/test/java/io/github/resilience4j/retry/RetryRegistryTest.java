/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.retry;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class RetryRegistryTest {

	private RetryRegistry retryRegistry;

	@Before
	public void setUp() {
		retryRegistry = RetryRegistry.ofDefaults();
	}

	@Test
	public void testCreateWithNullConfig() {
		assertThatThrownBy(() -> RetryRegistry.of((RetryConfig) null)).isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
	}

	@Test
	public void shouldReturnTheCorrectName() {
		Retry retry = retryRegistry.retry("testName");

		assertThat(retry).isNotNull();
		assertThat(retry.getName()).isEqualTo("testName");
	}

	@Test
	public void shouldBeTheSameRetry() {
		Retry retry = retryRegistry.retry("testName");
		Retry retry2 = retryRegistry.retry("testName");

		assertThat(retry).isSameAs(retry2);
		assertThat(retryRegistry.getAllRetries()).hasSize(1);
	}

	@Test
	public void shouldBeNotTheSameRetry() {
		Retry retry = retryRegistry.retry("testName");
		Retry retry2 = retryRegistry.retry("otherTestName");

		assertThat(retry).isNotSameAs(retry2);
		assertThat(retryRegistry.getAllRetries()).hasSize(2);
	}

	@Test
	public void noTagsByDefault() {
		Retry retry = retryRegistry.retry("testName");
		assertThat(retry.getTags()).hasSize(0);
	}

	@Test
	public void tagsOfRegistryAddedToInstance() {
		RetryConfig retryConfig = RetryConfig.ofDefaults();
		Map<String, RetryConfig> retryConfigs = Collections.singletonMap("default", retryConfig);
		io.vavr.collection.Map<String, String> retryTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		RetryRegistry retryRegistry = RetryRegistry.of(retryConfigs, retryTags);
		Retry retry = retryRegistry.retry("testName");

		assertThat(retry.getTags()).containsOnlyElementsOf(retryTags);
	}

	@Test
	public void tagsAddedToInstance() {
		io.vavr.collection.Map<String, String> retryTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		Retry retry = retryRegistry.retry("testName", retryTags);

		assertThat(retry.getTags()).containsOnlyElementsOf(retryTags);
	}

	@Test
	public void tagsOfRetriesShouldNotBeMixed() {
		RetryConfig config = RetryConfig.ofDefaults();
		io.vavr.collection.Map<String, String> retryTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		Retry retry = retryRegistry.retry("testName", config, retryTags);
		io.vavr.collection.Map<String, String> retryTags2 = io.vavr.collection.HashMap.of("key3","value3", "key4", "value4");
		Retry retry2 = retryRegistry.retry("otherTestName", config, retryTags2);

		assertThat(retry.getTags()).containsOnlyElementsOf(retryTags);
		assertThat(retry2.getTags()).containsOnlyElementsOf(retryTags2);
	}

	@Test
	public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
		RetryConfig retryConfig = RetryConfig.ofDefaults();
		Map<String, RetryConfig> retryConfigs = Collections.singletonMap("default", retryConfig);
		io.vavr.collection.Map<String, String> registryTags = io.vavr.collection.HashMap.of("key1","value1", "key2", "value2");
		io.vavr.collection.Map<String, String> instanceTags = io.vavr.collection.HashMap.of("key1","value3", "key4", "value4");
		RetryRegistry retryRegistry = RetryRegistry.of(retryConfigs, registryTags);
		Retry retry = retryRegistry.retry("testName", retryConfig, instanceTags);

		io.vavr.collection.Map<String, String> expectedTags = io.vavr.collection.HashMap.of("key1","value3", "key2", "value2", "key4", "value4");
		assertThat(retry.getTags()).containsOnlyElementsOf(expectedTags);
	}

	@Test
	public void canBuildRetryFromRegistryWithConfig() {
		RetryConfig config = RetryConfig.custom().maxAttempts(1000).waitDuration(Duration.ofSeconds(300)).build();
		Retry retry = retryRegistry.retry("testName", config);

		assertThat(retry).isNotNull();
		assertThat(retryRegistry.getAllRetries()).hasSize(1);
	}

	@Test
	public void canBuildRetryFromRegistryWithConfigSupplier() {
		RetryConfig config = RetryConfig.custom().maxAttempts(1000).waitDuration(Duration.ofSeconds(300)).build();
		Retry retry = retryRegistry.retry("testName", () -> config);

		assertThat(retry).isNotNull();
		assertThat(retryRegistry.getAllRetries()).hasSize(1);
	}

	@Test
	public void canBuildRetryRegistryWithConfig() {
		RetryConfig config = RetryConfig.custom().maxAttempts(1000).waitDuration(Duration.ofSeconds(300)).build();
		retryRegistry = RetryRegistry.of(config);
		Retry retry = retryRegistry.retry("testName", () -> config);

		assertThat(retry).isNotNull();
		assertThat(retryRegistry.getAllRetries()).hasSize(1);
	}

	@Test
	public void testCreateWithConfigurationMap() {
		Map<String, RetryConfig> configs = new HashMap<>();
		configs.put("default", RetryConfig.ofDefaults());
		configs.put("custom", RetryConfig.ofDefaults());

		RetryRegistry retryRegistry = RetryRegistry.of(configs);

		assertThat(retryRegistry.getDefaultConfig()).isNotNull();
		assertThat(retryRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testCreateWithConfigurationMapWithoutDefaultConfig() {
		Map<String, RetryConfig> configs = new HashMap<>();
		configs.put("custom", RetryConfig.ofDefaults());

		RetryRegistry retryRegistry = RetryRegistry.of(configs);

		assertThat(retryRegistry.getDefaultConfig()).isNotNull();
		assertThat(retryRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testCreateWithSingleRegistryEventConsumer() {
		RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.ofDefaults(), new NoOpRetryEventConsumer());

		getEventProcessor(retryRegistry.getEventPublisher())
				.ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
	}

	@Test
	public void testCreateWithMultipleRegistryEventConsumer() {
		List<RegistryEventConsumer<Retry>> registryEventConsumers = new ArrayList<>();
		registryEventConsumers.add(new NoOpRetryEventConsumer());
		registryEventConsumers.add(new NoOpRetryEventConsumer());

		RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.ofDefaults(), registryEventConsumers);

		getEventProcessor(retryRegistry.getEventPublisher())
				.ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
	}

	@Test
	public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
		Map<String, RetryConfig> configs = new HashMap<>();
		configs.put("custom", RetryConfig.ofDefaults());

		RetryRegistry retryRegistry = RetryRegistry.of(configs, new NoOpRetryEventConsumer());

		getEventProcessor(retryRegistry.getEventPublisher())
				.ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
	}

	@Test
	public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
		Map<String, RetryConfig> configs = new HashMap<>();
		configs.put("custom", RetryConfig.ofDefaults());
		List<RegistryEventConsumer<Retry>> registryEventConsumers = new ArrayList<>();
		registryEventConsumers.add(new NoOpRetryEventConsumer());
		registryEventConsumers.add(new NoOpRetryEventConsumer());

		RetryRegistry retryRegistry = RetryRegistry.of(configs, registryEventConsumers);

		getEventProcessor(retryRegistry.getEventPublisher())
				.ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
	}

	@Test
	public void testWithNotExistingConfig() {
		RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

		assertThatThrownBy(() -> retryRegistry.retry("test", "doesNotExist"))
				.isInstanceOf(ConfigurationNotFoundException.class);
	}

	@Test
	public void testAddConfiguration() {
		RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
		retryRegistry.addConfiguration("custom", RetryConfig.custom().build());

		assertThat(retryRegistry.getDefaultConfig()).isNotNull();
		assertThat(retryRegistry.getConfiguration("custom")).isNotNull();
	}

	private static Optional<EventProcessor<?>> getEventProcessor(Registry.EventPublisher<Retry> ep) {
		return ep instanceof EventProcessor<?> ? Optional.of((EventProcessor<?>) ep) : Optional.empty();
	}

	private static class NoOpRetryEventConsumer implements RegistryEventConsumer<Retry> {
		@Override
		public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) { }

		@Override
		public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) { }

		@Override
		public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) { }
	}
}
