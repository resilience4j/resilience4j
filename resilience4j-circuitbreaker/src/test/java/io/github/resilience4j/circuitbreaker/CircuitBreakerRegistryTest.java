/*
 *
 *  Copyright 2026 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.assertThat;


class CircuitBreakerRegistryTest {

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<CircuitBreaker> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }

        return Optional.empty();
    }

    @Test
    void shouldInitRegistryTags() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        Map<String, CircuitBreakerConfig> circuitBreakerConfigs = Collections
            .singletonMap("default", circuitBreakerConfig);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfigs,new NoOpCircuitBreakerEventConsumer(),Map.of("Tag1Key","Tag1Value"));
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsOnly(Map.entry("Tag1Key","Tag1Value"));
    }

    @Test
    void shouldReturnTheCorrectName() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("testName");
    }

    @Test
    void shouldBeTheSameCircuitBreaker() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker).isSameAs(circuitBreaker2);
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(1);
    }

    @Test
    void shouldBeNotTheSameCircuitBreaker() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");
        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);

        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
    }

    @Test
    void noTagsByDefault() {
        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults()
            .circuitBreaker("testName");
        assertThat(circuitBreaker.getTags()).isEmpty();
    }

    @Test
    void tagsOfRegistryAddedToInstance() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        Map<String, CircuitBreakerConfig> circuitBreakerConfigs = Collections
            .singletonMap("default", circuitBreakerConfig);
        Map<String, String> circuitBreakerTags = Map.of("key1", "value1", "key2", "value2");
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(circuitBreakerConfigs, circuitBreakerTags);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        assertThat(circuitBreaker.getTags()).containsAllEntriesOf(circuitBreakerTags);
    }

    @Test
    void tagsAddedToInstance() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        Map<String, String> retryTags = Map.of("key1", "value1", "key2", "value2");
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("testName", retryTags);

        assertThat(circuitBreaker.getTags()).containsAllEntriesOf(retryTags);
    }

    @Test
    void tagsOfRetriesShouldNotBeMixed() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        Map<String, String> circuitBreakerTags = Map.of("key1", "value1", "key2", "value2");
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("testName", circuitBreakerConfig, circuitBreakerTags);
        Map<String, String> circuitBreakerTags2 = Map.of("key3", "value3", "key4", "value4");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry
            .circuitBreaker("otherTestName", circuitBreakerConfig, circuitBreakerTags2);

        assertThat(circuitBreaker.getTags()).containsAllEntriesOf(circuitBreakerTags);
        assertThat(circuitBreaker2.getTags()).containsAllEntriesOf(circuitBreakerTags2);
    }

    @Test
    void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        Map<String, CircuitBreakerConfig> circuitBreakerConfigs = Collections
            .singletonMap("default", circuitBreakerConfig);
        Map<String, String> circuitBreakerTags = Map.of("key1", "value1", "key2", "value2");
        Map<String, String> instanceTags = Map.of("key1", "value3", "key4", "value4");
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(circuitBreakerConfigs, circuitBreakerTags);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("testName", circuitBreakerConfig, instanceTags);

        Map<String, String> expectedTags = Map.of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(circuitBreaker.getTags()).containsAllEntriesOf(expectedTags);
    }

    @Test
    void createWithDefaultConfiguration() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(CircuitBreakerConfig.ofDefaults());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");
        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);

        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
    }

    @Test
    void createWithCustomConfiguration() {
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate).build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(circuitBreakerConfig);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(failureRate);
    }

    @Test
    void createWithConfigurationMap() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(30f).build();
        CircuitBreakerConfig customCircuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(40f).build();
        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        configs.put("default", circuitBreakerConfig);
        configs.put("custom", customCircuitBreakerConfig);

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(configs);

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getDefaultConfig().getFailureRateThreshold())
            .isEqualTo(30f);
        assertThat(circuitBreakerRegistry.getConfiguration("custom")).isPresent();
    }

    @Test
    void createWithSingleRegistryEventConsumer() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(CircuitBreakerConfig.ofDefaults(), new NoOpCircuitBreakerEventConsumer());

        getEventProcessor(circuitBreakerRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpCircuitBreakerEventConsumer());
        registryEventConsumers.add(new NoOpCircuitBreakerEventConsumer());

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(CircuitBreakerConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(circuitBreakerRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        configs.put("custom", CircuitBreakerConfig.ofDefaults());

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(configs, new NoOpCircuitBreakerEventConsumer());

        getEventProcessor(circuitBreakerRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        configs.put("custom", CircuitBreakerConfig.ofDefaults());

        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpCircuitBreakerEventConsumer());
        registryEventConsumers.add(new NoOpCircuitBreakerEventConsumer());

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(configs, registryEventConsumers);

        getEventProcessor(circuitBreakerRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void addConfiguration() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate).build();
        circuitBreakerRegistry.addConfiguration("someSharedConfig", circuitBreakerConfig);

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getDefaultConfig().getFailureRateThreshold())
            .isEqualTo(50f);
        assertThat(circuitBreakerRegistry.getConfiguration("someSharedConfig")).isPresent();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("name", "someSharedConfig");
        assertThat(circuitBreaker.getCircuitBreakerConfig()).isEqualTo(circuitBreakerConfig);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(failureRate);
    }

    @Test
    void createWithConfigurationMapWithoutDefaultConfig() {
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate).build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(Collections.singletonMap("someSharedConfig", circuitBreakerConfig));

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getDefaultConfig().getFailureRateThreshold())
            .isEqualTo(50f);
        assertThat(circuitBreakerRegistry.getConfiguration("someSharedConfig")).isPresent();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("name", "someSharedConfig");

        assertThat(circuitBreaker.getCircuitBreakerConfig()).isEqualTo(circuitBreakerConfig);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(failureRate);
    }

    @Test
    void createWithNullConfig() {
        assertThatThrownBy(() -> CircuitBreakerRegistry.of((CircuitBreakerConfig) null))
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    void createUsingBuilderWithDefaultConfig() {
        CircuitBreakerRegistry circuitBreakerRegistry =
            CircuitBreakerRegistry.custom().withCircuitBreakerConfig(CircuitBreakerConfig.ofDefaults()).build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");
        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);

        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
    }

    @Test
    void createUsingBuilderWithCustomConfig() {
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate).build();

        CircuitBreakerRegistry circuitBreakerRegistry =
            CircuitBreakerRegistry.custom().withCircuitBreakerConfig(circuitBreakerConfig).build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(failureRate);
    }

    @Test
    void createUsingBuilderWithoutDefaultConfig() {
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate).build();

        CircuitBreakerRegistry circuitBreakerRegistry =
            CircuitBreakerRegistry.custom().addCircuitBreakerConfig("someSharedConfig", circuitBreakerConfig).build();

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getDefaultConfig().getFailureRateThreshold())
            .isEqualTo(50f);
        assertThat(circuitBreakerRegistry.getConfiguration("someSharedConfig")).isPresent();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("name", "someSharedConfig");

        assertThat(circuitBreaker.getCircuitBreakerConfig()).isEqualTo(circuitBreakerConfig);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(failureRate);
    }

    @Test
    void addMultipleDefaultConfigUsingBuilderShouldThrowException() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(30f).build();

        assertThatThrownBy(() -> CircuitBreakerRegistry.custom().addCircuitBreakerConfig("default", circuitBreakerConfig).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createUsingBuilderWithDefaultAndCustomConfig() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(30f).build();
        CircuitBreakerConfig customCircuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(40f).build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.custom()
            .withCircuitBreakerConfig(circuitBreakerConfig)
            .addCircuitBreakerConfig("custom", customCircuitBreakerConfig)
            .build();

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getDefaultConfig().getFailureRateThreshold())
            .isEqualTo(30f);
        assertThat(circuitBreakerRegistry.getConfiguration("custom")).isPresent();
    }

    @Test
    void createUsingBuilderWithNullConfig() {
        assertThatThrownBy(
            () -> CircuitBreakerRegistry.custom().withCircuitBreakerConfig(null).build())
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    void createUsingBuilderWithMultipleRegistryEventConsumer() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.custom()
            .withCircuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .addRegistryEventConsumer(new NoOpCircuitBreakerEventConsumer())
            .addRegistryEventConsumer(new NoOpCircuitBreakerEventConsumer())
            .build();

        getEventProcessor(circuitBreakerRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    void createUsingBuilderWithRegistryTags() {
        Map<String, String> circuitBreakerTags = Map.of("key1", "value1", "key2", "value2");
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.custom()
            .withCircuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .withTags(circuitBreakerTags)
            .build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        assertThat(circuitBreaker.getTags()).containsAllEntriesOf(circuitBreakerTags);
    }

    @Test
    void createUsingBuilderWithRegistryStore() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.custom()
            .withCircuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .withRegistryStore(new InMemoryRegistryStore<>())
            .build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");

        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
    }

    @Test
    void createUsingBuilderWithCircuitBreakerInitialStateOpen(){
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .initialState(CircuitBreaker.State.OPEN).build();
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.custom().addCircuitBreakerConfig("testName", circuitBreakerConfig).build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName", "testName");

        assertThat(circuitBreakerRegistry.getDefaultConfig().getInitialState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreakerRegistry.getConfiguration("testName").get().getInitialState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    }

    @Test
    void createUsingBuilderWithCircuitBreakerInitialStateMetricOnly(){
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .initialState(CircuitBreaker.State.METRICS_ONLY).build();
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.custom().addCircuitBreakerConfig("testName", circuitBreakerConfig).build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName", "testName");

        assertThat(circuitBreakerRegistry.getDefaultConfig().getInitialState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreakerRegistry.getConfiguration("testName").get().getInitialState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);

    }

    @Test
    void createUsingBuilderWithCircuitBreakerInitialStateDisabled(){
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .initialState(CircuitBreaker.State.DISABLED).build();
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.custom().addCircuitBreakerConfig("testName", circuitBreakerConfig).build();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName", "testName");

        assertThat(circuitBreakerRegistry.getDefaultConfig().getInitialState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreakerRegistry.getConfiguration("testName").get().getInitialState()).isEqualTo(CircuitBreaker.State.DISABLED);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.DISABLED);

    }


    private static class NoOpCircuitBreakerEventConsumer implements
        RegistryEventConsumer<CircuitBreaker> {

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
        }
    }
}
