package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.*;
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
    public void shouldInitRegistryTags() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RateLimiterConfig.ofDefaults());
        RateLimiterRegistry registry = RateLimiterRegistry.of(configs, Map.of("Tag1Key","Tag1Value"));
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsOnly(Map.entry("Tag1Key","Tag1Value"));
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
        Map<String, String> rateLimiterTags = Map.of("key1", "value1", "key2", "value2");
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(ratelimiterConfigs, rateLimiterTags);
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");

        assertThat(rateLimiter.getTags()).containsAllEntriesOf(rateLimiterTags);
    }

    @Test
    public void tagsAddedToInstance() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        Map<String, String> retryTags = Map.of("key1", "value1", "key2", "value2");
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName", retryTags);

        assertThat(rateLimiter.getTags()).containsAllEntriesOf(retryTags);
    }

    @Test
    public void tagsOfRetriesShouldNotBeMixed() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.ofDefaults();
        Map<String, String> rateLimiterTags = Map.of("key1", "value1", "key2", "value2");
        RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter("testName", rateLimiterConfig, rateLimiterTags);
        Map<String, String> rateLimiterTags2 = Map.of("key3", "value3", "key4", "value4");
        RateLimiter rateLimiter2 = rateLimiterRegistry
            .rateLimiter("otherTestName", rateLimiterConfig, rateLimiterTags2);

        assertThat(rateLimiter.getTags()).containsAllEntriesOf(rateLimiterTags);
        assertThat(rateLimiter2.getTags()).containsAllEntriesOf(rateLimiterTags2);
    }

    @Test
    public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.ofDefaults();
        Map<String, RateLimiterConfig> rateLimiterConfigs = Collections
            .singletonMap("default", rateLimiterConfig);
        Map<String, String> registryTags = Map.of("key1", "value1", "key2", "value2");
        Map<String, String> instanceTags = Map.of("key1", "value3", "key4", "value4");
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(rateLimiterConfigs, registryTags);
        RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter("testName", rateLimiterConfig, instanceTags);

        Map<String, String> expectedTags = Map.of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(rateLimiter.getTags()).containsAllEntriesOf(expectedTags);
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

    @Test
    public void testCreateUsingBuilderWithDefaultConfig() {
        RateLimiterRegistry rateLimiterRegistry =
            RateLimiterRegistry.custom().withRateLimiterConfig(RateLimiterConfig.ofDefaults()).build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");
        RateLimiter rateLimiter2 = rateLimiterRegistry.rateLimiter("otherTestName");
        assertThat(rateLimiter).isNotSameAs(rateLimiter2);

        assertThat(rateLimiterRegistry.getAllRateLimiters()).hasSize(2);
    }

    @Test
    public void testCreateUsingBuilderWithCustomConfig() {
        int limitForPeriod = 10;
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(10).build();

        RateLimiterRegistry rateLimiterRegistry =
            RateLimiterRegistry.custom().withRateLimiterConfig(rateLimiterConfig).build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");

        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod())
            .isEqualTo(limitForPeriod);
    }

    @Test
    public void testCreateUsingBuilderWithoutDefaultConfig() {
        int limitForPeriod = 10;
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(limitForPeriod).build();

        RateLimiterRegistry rateLimiterRegistry =
            RateLimiterRegistry.custom().addRateLimiterConfig("someSharedConfig", rateLimiterConfig).build();

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
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(10).build();
        RateLimiterRegistry.custom().addRateLimiterConfig("default", rateLimiterConfig).build();
    }

    @Test
    public void testCreateUsingBuilderWithDefaultAndCustomConfig() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(10).build();
        RateLimiterConfig customRateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(20).build();

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.custom()
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
            () -> RateLimiterRegistry.custom().withRateLimiterConfig(null).build())
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    public void testCreateUsingBuilderWithMultipleRegistryEventConsumer() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.custom()
            .withRateLimiterConfig(RateLimiterConfig.ofDefaults())
            .addRegistryEventConsumer(new NoOpRateLimiterEventConsumer())
            .addRegistryEventConsumer(new NoOpRateLimiterEventConsumer())
            .build();

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateUsingBuilderWithRegistryTags() {
        Map<String, String> rateLimiterTags = Map.of("key1", "value1", "key2", "value2");
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.custom()
            .withRateLimiterConfig(RateLimiterConfig.ofDefaults())
            .withTags(rateLimiterTags)
            .build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");

        assertThat(rateLimiter.getTags()).containsAllEntriesOf(rateLimiterTags);
    }

    @Test
    public void testCreateUsingBuilderWithRegistryStore() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.custom()
            .withRateLimiterConfig(RateLimiterConfig.ofDefaults())
            .withRegistryStore(new InMemoryRegistryStore<>())
            .build();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testName");
        RateLimiter rateLimiter2 = rateLimiterRegistry.rateLimiter("otherTestName");

        assertThat(rateLimiter).isNotSameAs(rateLimiter2);
        assertThat(rateLimiterRegistry.getAllRateLimiters()).hasSize(2);
    }
}
