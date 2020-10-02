/*
 * Copyright 2019 authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register TimeLimiter exposed events.
 */
public class TaggedTimeLimiterMetrics extends AbstractTimeLimiterMetrics implements MeterBinder {

    private final TimeLimiterRegistry timeLimiterRegistry;

    private TaggedTimeLimiterMetrics(TimeLimiterMetricNames names, TimeLimiterRegistry timeLimiterRegistry) {
        super(names);
        this.timeLimiterRegistry = requireNonNull(timeLimiterRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of time limiters.
     *
     * @param timeLimiterRegistry the source of time limiters
     * @return The {@link TaggedTimeLimiterMetrics} instance.
     */
    public static TaggedTimeLimiterMetrics ofTimeLimiterRegistry(
        TimeLimiterRegistry timeLimiterRegistry) {
        return new TaggedTimeLimiterMetrics(TimeLimiterMetricNames.ofDefaults(), timeLimiterRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of time limiters.
     *
     * @param names               custom metric names
     * @param timeLimiterRegistry the source of time limiters
     * @return The {@link TaggedTimeLimiterMetrics} instance.
     */
    public static TaggedTimeLimiterMetrics ofTimeLimiterRegistry(TimeLimiterMetricNames names,
                                                                 TimeLimiterRegistry timeLimiterRegistry) {
        return new TaggedTimeLimiterMetrics(names, timeLimiterRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (TimeLimiter timeLimiter : timeLimiterRegistry.getAllTimeLimiters()) {
            addMetrics(registry, timeLimiter);
        }
        timeLimiterRegistry.getEventPublisher()
            .onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        timeLimiterRegistry.getEventPublisher()
            .onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        timeLimiterRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

}
