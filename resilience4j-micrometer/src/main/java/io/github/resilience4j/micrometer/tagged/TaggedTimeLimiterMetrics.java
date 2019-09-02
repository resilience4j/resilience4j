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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register TimeLimiter exposed events.
 */
public class TaggedTimeLimiterMetrics extends AbstractMetrics implements MeterBinder {

    private static final String KIND_FAILED = "failed";
    private static final String KIND_SUCCESSFUL = "successful";
    private static final String KIND_TIMEOUT = "timeout";

    /**
     * Creates a new binder that uses given {@code registry} as source of time limiters.
     *
     * @param timeLimiterRegistry the source of time limiters
     * @return The {@link TaggedTimeLimiterMetrics} instance.
     */
    public static TaggedTimeLimiterMetrics ofTimeLimiterRegistry(TimeLimiterRegistry timeLimiterRegistry) {
        return new TaggedTimeLimiterMetrics(MetricNames.ofDefaults(), timeLimiterRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of time limiters.
     *
     * @param names               custom metric names
     * @param timeLimiterRegistry the source of time limiters
     * @return The {@link TaggedTimeLimiterMetrics} instance.
     */
    public static TaggedTimeLimiterMetrics ofTimeLimiterRegistry(MetricNames names, TimeLimiterRegistry timeLimiterRegistry) {
        return new TaggedTimeLimiterMetrics(names, timeLimiterRegistry);
    }

    private final MetricNames names;
    private final TimeLimiterRegistry timeLimiterRegistry;

    private TaggedTimeLimiterMetrics(MetricNames names, TimeLimiterRegistry timeLimiterRegistry) {
        super();
        this.names = Objects.requireNonNull(names);
        this.timeLimiterRegistry = Objects.requireNonNull(timeLimiterRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (TimeLimiter timeLimiter : timeLimiterRegistry.getAllTimeLimiters()) {
            addMetrics(registry, timeLimiter);
        }
        timeLimiterRegistry.getEventPublisher().onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        timeLimiterRegistry.getEventPublisher().onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        timeLimiterRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

    private void addMetrics(MeterRegistry registry, TimeLimiter timeLimiter) {
        Counter successes = Counter.builder(names.getCallsMetricName())
                .description("The number of successful calls")
                .tag(TagNames.NAME, timeLimiter.getName())
                .tag(TagNames.KIND, KIND_SUCCESSFUL)
                .register(registry);
        Counter failures = Counter.builder(names.getCallsMetricName())
                .description("The number of failed calls")
                .tag(TagNames.NAME, timeLimiter.getName())
                .tag(TagNames.KIND, KIND_FAILED)
                .register(registry);
        Counter timeouts = Counter.builder(names.getCallsMetricName())
                .description("The number of timed out calls")
                .tag(TagNames.NAME, timeLimiter.getName())
                .tag(TagNames.KIND, KIND_TIMEOUT)
                .register(registry);

        timeLimiter.getEventPublisher()
                .onSuccess(event -> successes.increment())
                .onError(event -> failures.increment())
                .onTimeout(event -> timeouts.increment());

        List<Meter.Id> ids = Arrays.asList(successes.getId(), failures.getId(), timeouts.getId());
        meterIdMap.put(timeLimiter.getName(), new HashSet<>(ids));
    }

    /**
     * Defines possible configuration for metric names.
     */
    public static class MetricNames {

        private static final String DEFAULT_PREFIX = "resilience4j.timelimiter";
        public static final String DEFAULT_TIME_LIMITER_CALLS = DEFAULT_PREFIX + ".calls";

        private String callsMetricName = DEFAULT_TIME_LIMITER_CALLS;

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
         *
         * @return The builder.
         */
        public static Builder custom() {
            return new Builder();
        }

        /**
         * Returns default metric names.
         *
         * @return The default {@link MetricNames} instance.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        /** Returns the metric name for circuit breaker calls, defaults to {@value DEFAULT_TIME_LIMITER_CALLS}.
         * @return The circuit breaker calls metric name.
         */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /**
         * Helps building custom instance of {@link MetricNames}.
         */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value TaggedTimeLimiterMetrics.MetricNames#DEFAULT_TIME_LIMITER_CALLS} with a given one.
             * @param callsMetricName The calls metric name.
             * @return The builder.*/
            public TaggedTimeLimiterMetrics.MetricNames.Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
                return this;
            }

            /**
             * Builds {@link MetricNames} instance.
             *
             * @return The built {@link MetricNames} instance.
             */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
