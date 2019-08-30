/*
 * Copyright 2019 Yevhenii Voievodin, Robert Winkler
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
import io.github.resilience4j.timelimiter.TimeLimiter.Metrics;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register TimeLimiter exposed events.
 */
public class TaggedTimeLimiterMetrics extends AbstractMetrics implements MeterBinder {

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param timeLimiterRegistry the source of retries
     * @return The {@link TaggedTimeLimiterMetrics} instance.
     */
    public static TaggedTimeLimiterMetrics ofTimeLimiterRegistry(TimeLimiterRegistry timeLimiterRegistry) {
        return new TaggedTimeLimiterMetrics(MetricNames.ofDefaults(), timeLimiterRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param names custom metric names
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
        Set<Meter.Id> idSet = new HashSet<>();

        idSet.add(Counter.builder(names.getAvailablePermissionsMetricName(), timeLimiter, rl -> rl.getMetrics().getAvailablePermissions())
                .description("The number of available permissions")
                .tag(TagNames.NAME, timeLimiter.getName())
                .register(registry).getId());
        idSet.add(Gauge.builder(names.getWaitingThreadsMetricName(), timeLimiter, rl -> rl.getMetrics().getNumberOfWaitingThreads())
                .description("The number of waiting threads")
                .tag(TagNames.NAME, timeLimiter.getName())
                .register(registry).getId());

        meterIdMap.put(timeLimiter.getName(), idSet);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        private static final String DEFAULT_PREFIX = "resilience4j.timelimiter";

        public static final String DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME = DEFAULT_PREFIX + ".available.permissions";
        public static final String DEFAULT_WAITING_THREADS_METRIC_NAME = DEFAULT_PREFIX + ".waiting_threads";

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
         * @return The builder.
         */
        public static Builder custom() {
            return new Builder();
        }

        /** Returns default metric names.
         * @return The default {@link MetricNames} instance.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        private String availablePermissionsMetricName = DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
        private String waitingThreadsMetricName = DEFAULT_WAITING_THREADS_METRIC_NAME;

        /** Returns the metric name for available permissions, defaults to {@value DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME}.
         * @return The available permissions metric name.
         */
        public String getAvailablePermissionsMetricName() {
            return availablePermissionsMetricName;
        }

        /** Returns the metric name for waiting threads, defaults to {@value DEFAULT_WAITING_THREADS_METRIC_NAME}.
         * @return The waiting threads metric name.
         */
        public String getWaitingThreadsMetricName() {
            return waitingThreadsMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME} with a given one.
             * @param availablePermissionsMetricName The available permissions metric name.
             * @return The builder.
             */
            public Builder availablePermissionsMetricName(String availablePermissionsMetricName) {
                metricNames.availablePermissionsMetricName = requireNonNull(availablePermissionsMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_WAITING_THREADS_METRIC_NAME} with a given one.
             * @param waitingThreadsMetricName The waiting threads metric name.
             * @return The builder.
             */
            public Builder waitingThreadsMetricName(String waitingThreadsMetricName) {
                metricNames.waitingThreadsMetricName = requireNonNull(waitingThreadsMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance.
             * @return The built {@link MetricNames} instance.
             */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
