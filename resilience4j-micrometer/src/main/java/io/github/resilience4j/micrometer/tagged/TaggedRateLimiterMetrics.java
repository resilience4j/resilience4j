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

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiter.Metrics;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register RateLimiter exposed {@link Metrics metrics}.
 */
public class TaggedRateLimiterMetrics extends AbstractMetrics implements MeterBinder {

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param rateLimiterRegistry the source of retries
     * @return The {@link TaggedRateLimiterMetrics} instance.
     */
    public static TaggedRateLimiterMetrics ofRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry) {
        return new TaggedRateLimiterMetrics(MetricNames.ofDefaults(), rateLimiterRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param names custom metric names
     * @param rateLimiterRegistry the source of rate limiters
     * @return The {@link TaggedRateLimiterMetrics} instance.
     */
    public static TaggedRateLimiterMetrics ofRateLimiterRegistry(MetricNames names, RateLimiterRegistry rateLimiterRegistry) {
        return new TaggedRateLimiterMetrics(names, rateLimiterRegistry);
    }

    private final MetricNames names;
    private final RateLimiterRegistry rateLimiterRegistry;

    private TaggedRateLimiterMetrics(MetricNames names, RateLimiterRegistry rateLimiterRegistry) {
        super();
        this.names = Objects.requireNonNull(names);
        this.rateLimiterRegistry = Objects.requireNonNull(rateLimiterRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (RateLimiter rateLimiter : rateLimiterRegistry.getAllRateLimiters()) {
            addMetrics(registry, rateLimiter);
        }
        rateLimiterRegistry.getEventPublisher().onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        rateLimiterRegistry.getEventPublisher().onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        rateLimiterRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

    private void addMetrics(MeterRegistry registry, RateLimiter rateLimiter) {
        Set<Meter.Id> idSet = new HashSet<>();

        idSet.add(Gauge.builder(names.getAvailablePermissionsMetricName(), rateLimiter, rl -> rl.getMetrics().getAvailablePermissions())
                .description("The number of available permissions")
                .tag(TagNames.NAME, rateLimiter.getName())
                .register(registry).getId());
        idSet.add(Gauge.builder(names.getWaitingThreadsMetricName(), rateLimiter, rl -> rl.getMetrics().getNumberOfWaitingThreads())
                .description("The number of waiting threads")
                .tag(TagNames.NAME, rateLimiter.getName())
                .register(registry).getId());

        meterIdMap.put(rateLimiter.getName(), idSet);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        private static final String DEFAULT_PREFIX = "resilience4j.ratelimiter";

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
