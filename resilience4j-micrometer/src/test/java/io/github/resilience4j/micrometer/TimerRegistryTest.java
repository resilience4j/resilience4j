/*
 *  Copyright 2023 Mariusz Kopylec
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
 */
package io.github.resilience4j.micrometer;

import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.resilience4j.micrometer.TimerRegistry.of;
import static io.github.resilience4j.micrometer.TimerRegistry.ofDefaults;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.BDDAssertions.then;

public class TimerRegistryTest {

    @Test
    public void shouldCreateDefaultTimerRegistry() {
        TimerRegistry registry = ofDefaults(new SimpleMeterRegistry());

        then(registry.getDefaultConfig().getMetricNames()).isEqualTo(TimerConfig.ofDefaults().getMetricNames());
        then(registry.getTags()).isEmpty();
        then(registry.getEventPublisher()).isNotNull();
        then(registry.getAllTimers()).isEmpty();
    }

    @Test
    public void shouldCreateCustomTimerRegistry() {
        TimerConfig defaultConfig = TimerConfig.custom()
                .metricNames("resilience4j.timer.operations")
                .build();
        TimerRegistry registry = of(defaultConfig, Map.of("custom", TimerConfig.ofDefaults()), Collections.emptyList(), Map.of("tag1", "value1"), new SimpleMeterRegistry());

        then(registry.getDefaultConfig().getMetricNames()).isEqualTo(defaultConfig.getMetricNames());
        then(registry.getConfiguration("custom")).hasValueSatisfying(config ->
                then(config.getMetricNames()).isEqualTo(TimerConfig.ofDefaults().getMetricNames())
        );
        then(registry.getTags()).containsExactlyInAnyOrderEntriesOf(Map.of("tag1", "value1"));
        then(registry.getEventPublisher()).isNotNull();
        then(registry.getAllTimers()).isEmpty();
    }

    @Test
    public void shouldCreateTimersFromRegistry() {
        TimerConfig defaultConfig = TimerConfig.custom()
                .metricNames("resilience4j.timer.operations")
                .build();
        TimerRegistry registry = of(defaultConfig, Map.of("custom", TimerConfig.ofDefaults()), Collections.emptyList(), Map.of("tag1", "value1"), new SimpleMeterRegistry());

        Timer defaultTimer = registry.timer("some operation");
        then(defaultTimer.getTimerConfig().getMetricNames()).isEqualTo(defaultConfig.getMetricNames());
        then(defaultTimer.getName()).isEqualTo("some operation");
        then(defaultTimer.getTags()).containsExactlyInAnyOrderEntriesOf(Map.of("tag1", "value1"));
        then(defaultTimer.getEventPublisher()).isNotNull();

        Timer customTimer = registry.timer("some other operation", "custom");
        then(customTimer.getTimerConfig().getMetricNames()).isEqualTo(TimerConfig.ofDefaults().getMetricNames());
        then(customTimer.getName()).isEqualTo("some other operation");
        then(customTimer.getTags()).containsExactlyInAnyOrderEntriesOf(Map.of("tag1", "value1"));
        then(customTimer.getEventPublisher()).isNotNull();
    }

    @Test
    public void shouldCreateTimerWithCustomTagsFromRegistry() {
        TimerRegistry registry = of(TimerConfig.ofDefaults(), emptyMap(), Collections.emptyList(), Map.of("tag1", "ignored value"), new SimpleMeterRegistry());

        Timer timer = registry.timer("some operation", Map.of("tag1", "primary value", "tag2", "value2"));
        then(timer.getTags()).containsExactlyInAnyOrderEntriesOf(Map.of("tag1", "primary value", "tag2", "value2"));
    }

    @Test
    public void shouldCreateTimerWithCustomConfigFromRegistry() {
        TimerConfig config = TimerConfig.custom()
                .metricNames("resilience4j.timer.operations")
                .build();
        TimerRegistry registry = ofDefaults(new SimpleMeterRegistry());

        Timer timer = registry.timer("some operation", config);
        then(timer.getTimerConfig().getMetricNames()).isEqualTo(config.getMetricNames());
    }

    @Test
    public void shouldAddEventConsumerToRegistry() {
        TimerRegistry registry = of(TimerConfig.ofDefaults(), emptyMap(), List.of(new NoOpTimerEventConsumer()), emptyMap(), new SimpleMeterRegistry());

        then(getEventProcessor(registry.getEventPublisher())).hasValueSatisfying(processor ->
                then(processor.hasConsumers()).isTrue()
        );
    }

    @Test
    public void shouldAddTimerConfigToRegistry() {
        TimerConfig config = TimerConfig.custom()
                .metricNames("resilience4j.timer.operations")
                .build();
        TimerRegistry registry = of(TimerConfig.ofDefaults(), Map.of("custom", config), new SimpleMeterRegistry());

        then(registry.getDefaultConfig().getMetricNames()).isEqualTo(TimerConfig.ofDefaults().getMetricNames());
        then(registry.getConfiguration("custom")).hasValueSatisfying(cfg ->
                then(cfg.getMetricNames()).isEqualTo(config.getMetricNames())
        );
    }

    private static class NoOpTimerEventConsumer implements RegistryEventConsumer<Timer> {

        @Override
        public void onEntryAddedEvent(EntryAddedEvent<Timer> entryAddedEvent) {
        }

        @Override
        public void onEntryRemovedEvent(EntryRemovedEvent<Timer> entryRemoveEvent) {
        }

        @Override
        public void onEntryReplacedEvent(EntryReplacedEvent<Timer> entryReplacedEvent) {
        }
    }

    private static Optional<EventProcessor<?>> getEventProcessor(Registry.EventPublisher<Timer> eventPublisher) {
        if (eventPublisher instanceof EventProcessor<?>) {
            return Optional.of((EventProcessor<?>) eventPublisher);
        }
        return Optional.empty();
    }
}
