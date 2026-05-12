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
package io.github.resilience4j.circuitbreaker.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.junit.jupiter.api.Test;


class InMemoryCircuitBreakerRegistryTest {

    @Test
    void addCircuitBreakerRegistry() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        circuitBreakerRegistry.addConfiguration("testConfig", CircuitBreakerConfig.ofDefaults());

        assertThat(circuitBreakerRegistry.getConfiguration("testConfig")).isNotNull();
    }

    @Test
    void getNotFoundCircuitBreakerRegistry() {
        InMemoryCircuitBreakerRegistry circuitBreakerRegistry = (InMemoryCircuitBreakerRegistry) CircuitBreakerRegistry
            .ofDefaults();

        assertThat(circuitBreakerRegistry.getConfiguration("testNotFound")).isEmpty();
    }

    @Test
    void updateDefaultCircuitBreakerRegistry() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        assertThatThrownBy(() -> circuitBreakerRegistry
            .addConfiguration("default", CircuitBreakerConfig.custom().build()))
            .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCircuitBreakerWithSharedConfiguration() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        circuitBreakerRegistry.addConfiguration("testConfig", CircuitBreakerConfig.ofDefaults());

        final CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("circuitBreaker",
                circuitBreakerRegistry.getConfiguration("testConfig").get());

        assertThat(circuitBreaker).isNotNull();
    }


    @Test
    void createCircuitBreakerWitMapConstructor() {
        Map<String, CircuitBreakerConfig> map = new HashMap<>();
        map.put("testBreaker", CircuitBreakerConfig.ofDefaults());
        CircuitBreakerRegistry circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry(map);
        circuitBreakerRegistry.addConfiguration("testConfig", CircuitBreakerConfig.ofDefaults());

        final CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("circuitBreaker",
                circuitBreakerRegistry.getConfiguration("testConfig").get());

        assertThat(circuitBreaker).isNotNull();
    }

    @Test
    void createCircuitBreakerWithConfigName() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        circuitBreakerRegistry.addConfiguration("testConfig",
            CircuitBreakerConfig.custom().slidingWindowSize(5).build());

        final CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("circuitBreaker",
                "testConfig");

        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(5);
    }

    @Test
    void createCircuitBreakerWithConfigNameNotFound() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        assertThatThrownBy(() -> circuitBreakerRegistry.circuitBreaker("circuitBreaker",
            "testConfig")).isInstanceOf(ConfigurationNotFoundException.class);
    }

    @Test
    void shouldCreateCircuitBreakerRegistryWithRegistryStore() {
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer = getNoOpsRegistryEventConsumer();
        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(registryEventConsumer);
        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        final CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.ofDefaults();
        configs.put("default", defaultConfig);
        final InMemoryCircuitBreakerRegistry inMemoryCircuitBreakerRegistry =
            new InMemoryCircuitBreakerRegistry(configs, registryEventConsumers,
                Map.of("Tag1", "Tag1Value"), new InMemoryRegistryStore<>());

        assertThat(inMemoryCircuitBreakerRegistry).isNotNull();
        assertThat(inMemoryCircuitBreakerRegistry.getDefaultConfig()).isEqualTo(defaultConfig);
        assertThat(inMemoryCircuitBreakerRegistry.getConfiguration("testNotFound")).isEmpty();
        inMemoryCircuitBreakerRegistry.addConfiguration("testConfig", defaultConfig);
        assertThat(inMemoryCircuitBreakerRegistry.getConfiguration("testConfig")).isNotNull();
    }

    private RegistryEventConsumer<CircuitBreaker> getNoOpsRegistryEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
            }
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
            }
        };
    }
}