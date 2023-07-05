/*
 *
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
 *
 *
 */
package io.github.resilience4j.micrometer.internal;

import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerConfig;
import io.github.resilience4j.micrometer.event.TimerEvent;
import io.github.resilience4j.micrometer.event.TimerOnFailureEvent;
import io.github.resilience4j.micrometer.event.TimerOnStartEvent;
import io.github.resilience4j.micrometer.event.TimerOnSuccessEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.resilience4j.micrometer.tagged.TagNames.KIND;
import static io.github.resilience4j.micrometer.tagged.TagNames.NAME;
import static io.micrometer.core.instrument.Timer.builder;
import static java.lang.System.nanoTime;
import static java.time.Duration.ofNanos;
import static java.util.Map.copyOf;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class TimerImpl implements Timer {

    private final String name;
    private final MeterRegistry registry;
    private final TimerConfig timerConfig;
    private final Map<String, String> tags;
    private final List<Tag> parsedTags;
    private final TimerEventProcessor eventProcessor;

    public TimerImpl(@NonNull String name, @NonNull MeterRegistry registry, @NonNull TimerConfig timerConfig, @NonNull Map<String, String> tags) {
        this.name = requireNonNull(name, "Name must not be null");
        this.registry = requireNonNull(registry, "Meter registry must not be null");
        this.timerConfig = requireNonNull(timerConfig, "Timer config must not be null");
        this.tags = copyOf(requireNonNull(tags, "Tags must not be null"));
        parsedTags = this.tags.entrySet().stream()
                .map(tagsEntry -> Tag.of(tagsEntry.getKey(), tagsEntry.getValue()))
                .collect(toList());
        eventProcessor = new TimerEventProcessor();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TimerConfig getTimerConfig() {
        return timerConfig;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public Context createContext() {
        return new ContextImpl(name, registry, parsedTags, timerConfig, eventProcessor);
    }

    public static class ContextImpl implements Context {

        private static final Logger LOGGER = LoggerFactory.getLogger(ContextImpl.class);
        private static final String RESULT = "result";
        private static final String KIND_FAILED = "failed";
        private static final String KIND_SUCCESSFUL = "successful";

        private final String name;
        private final MeterRegistry registry;
        private final List<Tag> tags;
        private final TimerConfig timerConfig;
        private final TimerEventProcessor eventProcessor;
        private final long start;

        public ContextImpl(String name, MeterRegistry registry, List<Tag> tags, TimerConfig timerConfig, TimerEventProcessor eventProcessor) {
            this.name = name;
            this.registry = registry;
            this.tags = tags;
            this.timerConfig = timerConfig;
            this.eventProcessor = eventProcessor;
            start = nanoTime();
            if (eventProcessor.hasConsumers()) {
                publishEvent(new TimerOnStartEvent(name));
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            recordCall(KIND_FAILED, () -> timerConfig.getFailureResultNameResolver().apply(throwable), duration -> new TimerOnFailureEvent(name, duration));
        }

        @Override
        public void onSuccess(Object output) {
            recordCall(KIND_SUCCESSFUL, () -> timerConfig.getSuccessResultNameResolver().apply(output), duration -> new TimerOnSuccessEvent(name, duration));
        }

        private void recordCall(String resultKind, Supplier<String> resultName, Function<Duration, TimerEvent> eventCreator) {
            Duration duration = ofNanos(nanoTime() - start);
            io.micrometer.core.instrument.Timer calls = builder(timerConfig.getMetricNames())
                    .description("Decorated operation calls")
                    .tag(NAME, name)
                    .tag(KIND, resultKind)
                    .tag(RESULT, resultName.get())
                    .tags(tags)
                    .register(registry);
            calls.record(duration);
            if (eventProcessor.hasConsumers()) {
                publishEvent(eventCreator.apply(duration));
            }
        }

        private void publishEvent(TimerEvent event) {
            try {
                eventProcessor.consumeEvent(event);
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to handle event {}", event.getEventType(), e);
            }
        }
    }
}

