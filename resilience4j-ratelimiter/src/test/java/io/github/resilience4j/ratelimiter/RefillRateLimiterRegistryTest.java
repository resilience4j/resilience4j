/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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
package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.Tuple;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RefillRateLimiterRegistryTest {

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<RateLimiter> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }

        return Optional.empty();
    }

    @Test
    public void shouldInitRegistryTags() {
        Map<String, RefillRateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RefillRateLimiterConfig.ofDefaults());
        RefillRateLimiterRegistry registry = RefillRateLimiterRegistry.of(configs,io.vavr.collection.HashMap.of("Tag1Key","Tag1Value"));
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsOnly(Tuple.of("Tag1Key","Tag1Value"));
    }

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, RefillRateLimiterConfig> configs = new HashMap<>();
        configs.put("default", RefillRateLimiterConfig.ofDefaults());
        configs.put("custom", RefillRateLimiterConfig.ofDefaults());

        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.of(configs);

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, RefillRateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RefillRateLimiterConfig.ofDefaults());

        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.of(configs);

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithCustomConfig() {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(50))
            .build();

        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.of(config);

        RateLimiterConfig rateLimiterConfig = rateLimiterRegistry.getDefaultConfig();
        assertThat(rateLimiterConfig).isSameAs(config);
    }

    @Test
    public void testCreateWithSingleRegistryEventConsumer() {
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry
            .of(RefillRateLimiterConfig.ofDefaults(), new NoOpRateLimiterEventConsumer());

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());

        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry
            .of(RefillRateLimiterConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, RefillRateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RefillRateLimiterConfig.ofDefaults());

        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry
            .of(configs, new NoOpRateLimiterEventConsumer());

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, RefillRateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RefillRateLimiterConfig.ofDefaults());

        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());

        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry
            .of(configs, registryEventConsumers);

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testAddConfiguration() {
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.ofDefaults();
        rateLimiterRegistry.addConfiguration("custom", RefillRateLimiterConfig.custom().build());

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testWithNotExistingConfig() {
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.ofDefaults();

        assertThatThrownBy(() -> rateLimiterRegistry.rateLimiter("test", "doesNotExist"))
            .isInstanceOf(ConfigurationNotFoundException.class);
    }

    @Test
    public void noTagsByDefault() {
        RateLimiter rateLimiter = RefillRateLimiterRegistry.ofDefaults().rateLimiter("testName");
        assertThat(rateLimiter.getTags()).hasSize(0);
    }

    @Test
    public void tagsOfRegistryAddedToInstance() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.ofDefaults();
        Map<String, RefillRateLimiterConfig> ratelimiterConfigs = Collections
            .singletonMap("default", rateLimiterConfig);
        io.vavr.collection.Map<String, String> rateLimiterTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry
            .of(ratelimiterConfigs, rateLimiterTags);
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");

        assertThat(rateLimiter.getTags()).containsOnlyElementsOf(rateLimiterTags);
    }

    @Test
    public void tagsAddedToInstance() {
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.ofDefaults();
        io.vavr.collection.Map<String, String> retryTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName", retryTags);

        assertThat(rateLimiter.getTags()).containsOnlyElementsOf(retryTags);
    }

    @Test
    public void tagsOfRetriesShouldNotBeMixed() {
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.ofDefaults();
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.ofDefaults();
        io.vavr.collection.Map<String, String> rateLimiterTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter("testName", rateLimiterConfig, rateLimiterTags);
        io.vavr.collection.Map<String, String> rateLimiterTags2 = io.vavr.collection.HashMap
            .of("key3", "value3", "key4", "value4");
        RateLimiter rateLimiter2 = rateLimiterRegistry
            .rateLimiter("otherTestName", rateLimiterConfig, rateLimiterTags2);

        assertThat(rateLimiter.getTags()).containsOnlyElementsOf(rateLimiterTags);
        assertThat(rateLimiter2.getTags()).containsOnlyElementsOf(rateLimiterTags2);
    }

    @Test
    public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.ofDefaults();
        Map<String, RefillRateLimiterConfig> rateLimiterConfigs = Collections
            .singletonMap("default", rateLimiterConfig);
        io.vavr.collection.Map<String, String> registryTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        io.vavr.collection.Map<String, String> instanceTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key4", "value4");
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry
            .of(rateLimiterConfigs, registryTags);
        RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter("testName", rateLimiterConfig, instanceTags);

        io.vavr.collection.Map<String, String> expectedTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(rateLimiter.getTags()).containsOnlyElementsOf(expectedTags);
    }

    @Test
    public void testCreateUsingBuilderWithDefaultConfig() {
        RefillRateLimiterRegistry rateLimiterRegistry =
            RefillRateLimiterRegistry.custom().withRateLimiterConfig(RefillRateLimiterConfig.ofDefaults()).build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");
        RateLimiter rateLimiter2 = rateLimiterRegistry.rateLimiter("otherTestName");
        assertThat(rateLimiter).isNotSameAs(rateLimiter2);

        assertThat(rateLimiterRegistry.getAllRateLimiters()).hasSize(2);
    }

    @Test
    public void testCreateUsingBuilderWithCustomConfig() {
        int limitForPeriod = 10;
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(10).build();

        RefillRateLimiterRegistry rateLimiterRegistry =
            RefillRateLimiterRegistry.custom().withRateLimiterConfig(rateLimiterConfig).build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");

        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod())
            .isEqualTo(limitForPeriod);
    }

    @Test
    public void testCreateUsingBuilderWithoutDefaultConfig() {
        int limitForPeriod = 10;
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(limitForPeriod).build();

        RefillRateLimiterRegistry rateLimiterRegistry =
            RefillRateLimiterRegistry.custom().addRateLimiterConfig("someSharedConfig", rateLimiterConfig).build();

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getDefaultConfig().getLimitForPeriod())
            .isEqualTo(50);
        assertThat(rateLimiterRegistry.getConfiguration("someSharedConfig")).isNotEmpty();

        RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter("name", "someSharedConfig");

        assertThat(rateLimiter.getRateLimiterConfig()).isEqualTo(rateLimiterConfig);
        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod())
            .isEqualTo(limitForPeriod);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddMultipleDefaultConfigUsingBuilderShouldThrowException() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(10).build();
        RefillRateLimiterRegistry.custom().addRateLimiterConfig("default", rateLimiterConfig).build();
    }

    @Test
    public void testCreateUsingBuilderWithDefaultAndCustomConfig() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(10).build();
        RefillRateLimiterConfig customRateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(20).build();

        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.custom()
            .withRateLimiterConfig(rateLimiterConfig)
            .addRateLimiterConfig("custom", customRateLimiterConfig)
            .build();

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getDefaultConfig().getLimitForPeriod())
            .isEqualTo(10);
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotEmpty();
    }

    @Test
    public void testCreateUsingBuilderWithNullConfig() {
        assertThatThrownBy(
            () -> RefillRateLimiterRegistry.custom().withRateLimiterConfig(null).build())
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    public void testCreateUsingBuilderWithMultipleRegistryEventConsumer() {
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.custom()
            .withRateLimiterConfig(RefillRateLimiterConfig.ofDefaults())
            .addRegistryEventConsumer(new NoOpRateLimiterEventConsumer())
            .addRegistryEventConsumer(new NoOpRateLimiterEventConsumer())
            .build();

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateUsingBuilderWithRegistryTags() {
        io.vavr.collection.Map<String, String> rateLimiterTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.custom()
            .withRateLimiterConfig(RefillRateLimiterConfig.ofDefaults())
            .withTags(rateLimiterTags)
            .build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");

        assertThat(rateLimiter.getTags()).containsOnlyElementsOf(rateLimiterTags);
    }

    @Test
    public void testCreateUsingBuilderWithRegistryStore() {
        RefillRateLimiterRegistry rateLimiterRegistry = RefillRateLimiterRegistry.custom()
            .withRateLimiterConfig(RefillRateLimiterConfig.ofDefaults())
            .withRegistryStore(new InMemoryRegistryStore())
            .build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");
        RateLimiter rateLimiter2 = rateLimiterRegistry.rateLimiter("otherTestName");

        assertThat(rateLimiter).isNotSameAs(rateLimiter2);
        assertThat(rateLimiterRegistry.getAllRateLimiters()).hasSize(2);
    }

}
