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
