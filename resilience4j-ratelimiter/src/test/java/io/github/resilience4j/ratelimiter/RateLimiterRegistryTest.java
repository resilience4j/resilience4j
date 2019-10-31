package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class RateLimiterRegistryTest {

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<RateLimiter> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }

        return Optional.empty();
    }

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("default", RateLimiterConfig.ofDefaults());
        configs.put("custom", RateLimiterConfig.ofDefaults());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(configs);

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RateLimiterConfig.ofDefaults());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(configs);

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithCustomConfig() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(50))
            .build();

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);

        RateLimiterConfig rateLimiterConfig = rateLimiterRegistry.getDefaultConfig();
        assertThat(rateLimiterConfig).isSameAs(config);
    }

    @Test
    public void testCreateWithSingleRegistryEventConsumer() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(RateLimiterConfig.ofDefaults(), new NoOpRateLimiterEventConsumer());

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(RateLimiterConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RateLimiterConfig.ofDefaults());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(configs, new NoOpRateLimiterEventConsumer());

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RateLimiterConfig.ofDefaults());

        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(configs, registryEventConsumers);

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testAddConfiguration() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        rateLimiterRegistry.addConfiguration("custom", RateLimiterConfig.custom().build());

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testWithNotExistingConfig() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        assertThatThrownBy(() -> rateLimiterRegistry.rateLimiter("test", "doesNotExist"))
            .isInstanceOf(ConfigurationNotFoundException.class);
    }

    @Test
    public void noTagsByDefault() {
        RateLimiter rateLimiter = RateLimiterRegistry.ofDefaults().rateLimiter("testName");
        assertThat(rateLimiter.getTags()).hasSize(0);
    }

    @Test
    public void tagsOfRegistryAddedToInstance() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.ofDefaults();
        Map<String, RateLimiterConfig> ratelimiterConfigs = Collections
            .singletonMap("default", rateLimiterConfig);
        io.vavr.collection.Map<String, String> rateLimiterTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(ratelimiterConfigs, rateLimiterTags);
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");

        assertThat(rateLimiter.getTags()).containsOnlyElementsOf(rateLimiterTags);
    }

    @Test
    public void tagsAddedToInstance() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        io.vavr.collection.Map<String, String> retryTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        RateLimiter circuitBreaker = rateLimiterRegistry.rateLimiter("testName", retryTags);

        assertThat(circuitBreaker.getTags()).containsOnlyElementsOf(retryTags);
    }

    @Test
    public void tagsOfRetriesShouldNotBeMixed() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.ofDefaults();
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
        RateLimiterConfig circuitBreakerConfig = RateLimiterConfig.ofDefaults();
        Map<String, RateLimiterConfig> rateLimiterConfigs = Collections
            .singletonMap("default", circuitBreakerConfig);
        io.vavr.collection.Map<String, String> registryTags = io.vavr.collection.HashMap
            .of("key1", "value1", "key2", "value2");
        io.vavr.collection.Map<String, String> instanceTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key4", "value4");
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(rateLimiterConfigs, registryTags);
        RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter("testName", circuitBreakerConfig, instanceTags);

        io.vavr.collection.Map<String, String> expectedTags = io.vavr.collection.HashMap
            .of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(rateLimiter.getTags()).containsOnlyElementsOf(expectedTags);
    }

    private static class NoOpRateLimiterEventConsumer implements
        RegistryEventConsumer<RateLimiter> {

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<RateLimiter> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<RateLimiter> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<RateLimiter> entryReplacedEvent) {
        }
    }
}
