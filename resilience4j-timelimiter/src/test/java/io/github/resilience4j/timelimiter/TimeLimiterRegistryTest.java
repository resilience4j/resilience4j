package io.github.resilience4j.timelimiter;

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


public class TimeLimiterRegistryTest {

    private static Optional<EventProcessor<?>> getEventProcessor(
        Registry.EventPublisher<TimeLimiter> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }

        return Optional.empty();
    }

    @Test
    public void testCreateWithCustomConfig() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .cancelRunningFuture(false)
            .timeoutDuration(Duration.ofMillis(500))
            .build();

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(config);

        TimeLimiterConfig timeLimiterConfig = timeLimiterRegistry.getDefaultConfig();
        assertThat(timeLimiterConfig).isSameAs(config);
    }

    @Test
    public void shouldInitRegistryTags() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.ofDefaults();
        Map<String, TimeLimiterConfig> timeLimiterConfigs = Collections
            .singletonMap("default", timeLimiterConfig);
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(timeLimiterConfigs, new NoOpTimeLimiterEventConsumer(), Map.of("Tag1Key","Tag1Value"));
        assertThat(registry.getTags()).isNotEmpty();
        assertThat(registry.getTags()).containsOnly(Map.entry("Tag1Key","Tag1Value"));
    }

    @Test
    public void noTagsByDefault() {
        TimeLimiter TimeLimiter = TimeLimiterRegistry.ofDefaults()
            .timeLimiter("testName");
        assertThat(TimeLimiter.getTags()).hasSize(0);
    }

    @Test
    public void tagsOfRegistryAddedToInstance() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.ofDefaults();
        Map<String, TimeLimiterConfig> timeLimiterConfigs = Collections
            .singletonMap("default", timeLimiterConfig);
        Map<String, String> timeLimiterTags = Map.of("key1", "value1", "key2", "value2");
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry
            .of(timeLimiterConfigs, timeLimiterTags);
        TimeLimiter TimeLimiter = timeLimiterRegistry.timeLimiter("testName");

        assertThat(TimeLimiter.getTags()).containsAllEntriesOf(timeLimiterTags);
    }

    @Test
    public void tagsAddedToInstance() {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        Map<String, String> timeLimiterTags = Map.of("key1", "value1", "key2", "value2");
        TimeLimiter TimeLimiter = timeLimiterRegistry
            .timeLimiter("testName", timeLimiterTags);

        assertThat(TimeLimiter.getTags()).containsAllEntriesOf(timeLimiterTags);
    }

    @Test
    public void tagsOfTimeLimitersShouldNotBeMixed() {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.ofDefaults();
        Map<String, String> timeLimiterTags = Map.of("key1", "value1", "key2", "value2");
        TimeLimiter TimeLimiter = timeLimiterRegistry
            .timeLimiter("testName", timeLimiterConfig, timeLimiterTags);
        Map<String, String> timeLimiterTags2 = Map.of("key3", "value3", "key4", "value4");
        TimeLimiter TimeLimiter2 = timeLimiterRegistry
            .timeLimiter("otherTestName", timeLimiterConfig, timeLimiterTags2);

        assertThat(TimeLimiter.getTags()).containsAllEntriesOf(timeLimiterTags);
        assertThat(TimeLimiter2.getTags()).containsAllEntriesOf(timeLimiterTags2);
    }

    @Test
    public void tagsOfInstanceTagsShouldOverrideRegistryTags() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.ofDefaults();
        Map<String, TimeLimiterConfig> timeLimiterConfigs = Collections
            .singletonMap("default", timeLimiterConfig);
        Map<String, String> timeLimiterTags = Map.of("key1", "value1", "key2", "value2");
        Map<String, String> instanceTags = Map.of("key1", "value3", "key4", "value4");
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry
            .of(timeLimiterConfigs, timeLimiterTags);
        TimeLimiter timeLimiter = timeLimiterRegistry
            .timeLimiter("testName", timeLimiterConfig, instanceTags);

        Map<String, String> expectedTags = Map.of("key1", "value3", "key2", "value2", "key4", "value4");
        assertThat(timeLimiter.getTags()).containsAllEntriesOf(expectedTags);
    }

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, TimeLimiterConfig> configs = new HashMap<>();
        configs.put("default", TimeLimiterConfig.ofDefaults());
        configs.put("custom", TimeLimiterConfig.ofDefaults());

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(configs);

        assertThat(timeLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(timeLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, TimeLimiterConfig> configs = new HashMap<>();
        configs.put("custom", TimeLimiterConfig.ofDefaults());

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(configs);

        assertThat(timeLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(timeLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithSingleRegistryEventConsumer() {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry
            .of(TimeLimiterConfig.ofDefaults(), new NoOpTimeLimiterEventConsumer());

        getEventProcessor(timeLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpTimeLimiterEventConsumer());
        registryEventConsumers.add(new NoOpTimeLimiterEventConsumer());

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry
            .of(TimeLimiterConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(timeLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, TimeLimiterConfig> configs = new HashMap<>();
        configs.put("custom", TimeLimiterConfig.ofDefaults());

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry
            .of(configs, new NoOpTimeLimiterEventConsumer());

        getEventProcessor(timeLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithMultiRegistryEventConsumer() {
        Map<String, TimeLimiterConfig> configs = new HashMap<>();
        configs.put("custom", TimeLimiterConfig.ofDefaults());

        List<RegistryEventConsumer<TimeLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpTimeLimiterEventConsumer());
        registryEventConsumers.add(new NoOpTimeLimiterEventConsumer());

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry
            .of(configs, registryEventConsumers);

        getEventProcessor(timeLimiterRegistry.getEventPublisher())
            .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testAddConfiguration() {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiterRegistry.addConfiguration("custom", TimeLimiterConfig.custom().build());

        assertThat(timeLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(timeLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testWithNotExistingConfig() {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

        assertThatThrownBy(() -> timeLimiterRegistry.timeLimiter("test", "doesNotExist"))
            .isInstanceOf(ConfigurationNotFoundException.class);
    }

    private static class NoOpTimeLimiterEventConsumer implements
        RegistryEventConsumer<TimeLimiter> {

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<TimeLimiter> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<TimeLimiter> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<TimeLimiter> entryReplacedEvent) {
        }
    }
}
