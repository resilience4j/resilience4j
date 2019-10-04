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
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.ofDefaults(), new NoOpRateLimiterEventConsumer());

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
                .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithMultipleRegistryEventConsumer() {
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());
        registryEventConsumers.add(new NoOpRateLimiterEventConsumer());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.ofDefaults(), registryEventConsumers);

        getEventProcessor(rateLimiterRegistry.getEventPublisher())
                .ifPresent(eventProcessor -> assertThat(eventProcessor.hasConsumers()).isTrue());
    }

    @Test
    public void testCreateWithConfigurationMapWithSingleRegistryEventConsumer() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RateLimiterConfig.ofDefaults());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(configs, new NoOpRateLimiterEventConsumer());

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

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(configs, registryEventConsumers);

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

    private static Optional<EventProcessor<?>> getEventProcessor(Registry.EventPublisher<RateLimiter> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }

        return Optional.empty();
    }

    private static class NoOpRateLimiterEventConsumer implements RegistryEventConsumer<RateLimiter> {
        @Override
        public void onEntryAddedEvent(EntryAddedEvent<RateLimiter> entryAddedEvent) { }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<RateLimiter> entryRemoveEvent) { }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<RateLimiter> entryReplacedEvent) { }
    }
}
