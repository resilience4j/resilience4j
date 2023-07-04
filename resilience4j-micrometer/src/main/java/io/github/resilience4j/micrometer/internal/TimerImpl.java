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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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

    public TimerImpl(@NonNull String name, @NonNull MeterRegistry registry, @NonNull TimerConfig timerConfig, @NonNull Map<String, String> tags) {
        this.name = requireNonNull(name, "Name must not be null");
        this.registry = requireNonNull(registry, "Meter registry must not be null");
        this.timerConfig = requireNonNull(timerConfig, "Timer config must not be null");
        this.tags = copyOf(requireNonNull(tags, "Tags must not be null"));
        parsedTags = this.tags.entrySet().stream()
                .map(tagsEntry -> Tag.of(tagsEntry.getKey(), tagsEntry.getValue()))
                .collect(toList());
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
        return new ContextImpl(name, registry, parsedTags, timerConfig);
    }

    public static class ContextImpl implements Context {

        private static final String RESULT = "result";
        private static final String KIND_FAILED = "failed";
        private static final String KIND_SUCCESSFUL = "successful";

        private final String name;
        private final MeterRegistry registry;
        private final List<Tag> tags;
        private final TimerConfig timerConfig;
        private final long start;

        public ContextImpl(String name, MeterRegistry registry, List<Tag> tags, TimerConfig timerConfig) {
            this.name = name;
            this.registry = registry;
            this.tags = tags;
            this.timerConfig = timerConfig;
            start = nanoTime();
        }

        @Override
        public void onFailure(Throwable throwable) {
            recordCall(KIND_FAILED, () -> timerConfig.getFailureResultNameResolver().apply(throwable));
        }

        @Override
        public void onSuccess(Object output) {
            recordCall(KIND_SUCCESSFUL, () -> timerConfig.getSuccessResultNameResolver().apply(output));
        }

        private void recordCall(String resultKind, Supplier<String> resultName) {
            Duration duration = ofNanos(nanoTime() - start);
            io.micrometer.core.instrument.Timer calls = builder(timerConfig.getMetricNames())
                    .description("Decorated operation calls")
                    .tag(NAME, name)
                    .tag(KIND, resultKind)
                    .tag(RESULT, resultName.get())
                    .tags(tags)
                    .register(registry);
            calls.record(duration);
        }
    }
}

