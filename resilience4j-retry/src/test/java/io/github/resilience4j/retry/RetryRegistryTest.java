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
import io.github.resilience4j.core.registry.*;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class RetryRegistryTest {

    private RetryRegistry retryRegistry;

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<Retry> ep) {
        return ep instanceof EventProcessor<?> ? Optional.of((EventProcessor<?>) ep)
            : Optional.empty();
    }

    @Before
    public void setUp() {
        retryRegistry = RetryRegistry.ofDefaults();
    }

    @Test
    public void testCreateWithNullConfig() {
        assertThatThrownBy(() -> RetryRegistry.of((RetryConfig) null))
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    public void shouldInitRegistryTags() {
        RetryConfig retryConfig = RetryConfig.ofDefaults();
        Map<String, RetryConfig> retryConfigs = Collections.singletonMap("default", retryConfig);
        RetryRegistry registry = RetryRegistry.of(retryConfigs,new NoOpRetryEventConsumer(), Map.of("Tag1Key","Tag1Value"));
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsOnly(Map.entry("Tag1Key","Tag1Value"));
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
        Map<String, String> retryTags = Map.of("key1", "value1", "key2", "value2");
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfigs, retryTags);
        Retry retry = retryRegistry.retry("testName");

        assertThat(retry.getTags()).containsAllEntriesOf(retryTags);
    }

    @Test
    public void tagsAddedToInstance() {
        Map<String, String> retryTags = Map.of("key1", "value1", "key2", "value2");
        Retry retry = retryRegistry.retry("testName", retryTags);

        assertThat(retry.getTags()).containsAllEntriesOf(retryTags);
    }

    @Test
    public void tagsOfRetriesShouldNotBeMixed() {
        RetryConfig config = RetryConfig.ofDefaults();
        Map<String, String> retryTags = Map.of("key1", "value1", "key2", "value2");
        Retry retry = retryRegistry.retry("testName", config, retryTags);
        Map<String, String> retryTags2 = Map.of("key3", "value3", "key4", "value4");
        Retry retry2 = retryRegistry.retry("otherTestName", config, retryTags2);

        assertThat(retry.getTags()).containsAllEntriesOf(retryTags);
        assertThat(retry2.getTags()).containsAllEntriesOf(retryTags2);
    }

    @Test
    public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        RetryConfig retryConfig = RetryConfig.ofDefaults();
        Map<String, RetryConfig> retryConfigs = Collections.singletonMap("default", retryConfig);
        Map<String, String> registryTags = Map.of("key1", "value1", "key2", "value2");
        Map<String, String> instanceTags = Map.of("key1", "value3", "key4", "value4");
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfigs, registryTags);
        Retry retry = retryRegistry.retry("testName", retryConfig, instanceTags);

        Map<String, String> expectedTags = Map.of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(retry.getTags()).containsAllEntriesOf(expectedTags);
    }

    @Test
    public void canBuildRetryFromRegistryWithConfig() {
        RetryConfig config = RetryConfig.custom().maxAttempts(1000)
            .waitDuration(Duration.ofSeconds(300)).build();
        Retry retry = retryRegistry.retry("testName", config);

        assertThat(retry).isNotNull();
        assertThat(retryRegistry.getAllRetries()).hasSize(1);
    }

    @Test
    public void canBuildRetryFromRegistryWithConfigSupplier() {
        RetryConfig config = RetryConfig.custom().maxAttempts(1000)
            .waitDuration(Duration.ofSeconds(300)).build();
        Retry retry = retryRegistry.retry("testName", () -> config);

        assertThat(retry).isNotNull();
        assertThat(retryRegistry.getAllRetries()).hasSize(1);
    }

    @Test
    public void canBuildRetryRegistryWithConfig() {
        RetryConfig config = RetryConfig.custom().maxAttempts(1000)
            .waitDuration(Duration.ofSeconds(300)).build();
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
        RetryRegistry retryRegistry = RetryRegistry
            .of(RetryConfig.ofDefaults(), new NoOpRetryEventConsumer());

        getEventProcessor(retryRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<Retry>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpRetryEventConsumer());
        registryEventConsumers.add(new NoOpRetryEventConsumer());

        RetryRegistry retryRegistry = RetryRegistry
            .of(RetryConfig.ofDefaults(), registryEventConsumers);

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

    private static class NoOpRetryEventConsumer implements RegistryEventConsumer<Retry> {

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
        }
    }

    @Test
    public void testCreateUsingBuilderWithDefaultConfig() {
        RetryRegistry retryRegistry =
            RetryRegistry.custom().withRetryConfig(RetryConfig.ofDefaults()).build();
        Retry retry = retryRegistry.retry("testName");
        Retry retry2 = retryRegistry.retry("otherTestName");
        assertThat(retry).isNotSameAs(retry2);
        assertThat(retryRegistry.getAllRetries()).hasSize(2);
    }

    @Test
    public void testCreateUsingBuilderWithCustomConfig() {
        int maxAttempts = 1000;
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(maxAttempts).build();

        RetryRegistry retryRegistry =
            RetryRegistry.custom().withRetryConfig(retryConfig).build();
        Retry retry = retryRegistry.retry("testName");

        assertThat(retry.getRetryConfig().getMaxAttempts())
            .isEqualTo(maxAttempts);
    }

    @Test
    public void testCreateUsingBuilderWithoutDefaultConfig() {
        int maxAttempts = 1000;
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(maxAttempts).build();

        RetryRegistry retryRegistry =
            RetryRegistry.custom().addRetryConfig("someSharedConfig", retryConfig).build();

        assertThat(retryRegistry.getDefaultConfig()).isNotNull();
        assertThat(retryRegistry.getDefaultConfig().getMaxAttempts())
            .isEqualTo(3);
        assertThat(retryRegistry.getConfiguration("someSharedConfig")).isNotEmpty();

        Retry retry = retryRegistry
            .retry("name", "someSharedConfig");

        assertThat(retry.getRetryConfig()).isEqualTo(retryConfig);
        assertThat(retry.getRetryConfig().getMaxAttempts())
            .isEqualTo(maxAttempts);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddMultipleDefaultConfigUsingBuilderShouldThrowException() {
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(1000).build();
        RetryRegistry.custom().addRetryConfig("default", retryConfig).build();
    }

    @Test
    public void testCreateUsingBuilderWithDefaultAndCustomConfig() {
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(1000).build();
        RetryConfig customRetryConfig = RetryConfig.custom()
            .maxAttempts(300).build();

        RetryRegistry retryRegistry = RetryRegistry.custom()
            .withRetryConfig(retryConfig)
            .addRetryConfig("custom", customRetryConfig)
            .build();

        assertThat(retryRegistry.getDefaultConfig()).isNotNull();
        assertThat(retryRegistry.getDefaultConfig().getMaxAttempts())
            .isEqualTo(1000);
        assertThat(retryRegistry.getConfiguration("custom")).isNotEmpty();
    }

    @Test
    public void testCreateUsingBuilderWithNullConfig() {
        assertThatThrownBy(
            () -> RetryRegistry.custom().withRetryConfig(null).build())
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    public void testCreateUsingBuilderWithMultipleRegistryEventConsumer() {
        RetryRegistry retryRegistry = RetryRegistry.custom()
            .withRetryConfig(RetryConfig.ofDefaults())
            .addRegistryEventConsumer(new NoOpRetryEventConsumer())
            .addRegistryEventConsumer(new NoOpRetryEventConsumer())
            .build();

        getEventProcessor(retryRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateUsingBuilderWithRegistryTags() {
        Map<String, String> retryTags = Map.of("key1", "value1", "key2", "value2");
        RetryRegistry retryRegistry = RetryRegistry.custom()
            .withRetryConfig(RetryConfig.ofDefaults())
            .withTags(retryTags)
            .build();
        Retry retry = retryRegistry.retry("testName");

        assertThat(retry.getTags()).containsAllEntriesOf(retryTags);
    }

    @Test
    public void testCreateUsingBuilderWithRegistryStore() {
        RetryRegistry retryRegistry = RetryRegistry.custom()
            .withRetryConfig(RetryConfig.ofDefaults())
            .withRegistryStore(new InMemoryRegistryStore<>())
            .build();
        Retry retry = retryRegistry.retry("testName");
        Retry retry2 = retryRegistry.retry("otherTestName");

        assertThat(retry).isNotSameAs(retry2);
        assertThat(retryRegistry.getAllRetries()).hasSize(2);
    }
}
