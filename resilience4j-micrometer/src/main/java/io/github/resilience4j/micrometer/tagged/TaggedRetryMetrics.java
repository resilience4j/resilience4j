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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.HashSet;
import java.util.Set;

import static io.github.resilience4j.retry.Retry.Metrics;
import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register Retry exposed {@link Metrics metrics}.
 */
public class TaggedRetryMetrics extends AbstractMetrics implements MeterBinder {

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param retryRegistry the source of retries
     * @return The {@link TaggedRetryMetrics} instance.
     */
    public static TaggedRetryMetrics ofRetryRegistry(RetryRegistry retryRegistry) {
        return new TaggedRetryMetrics(MetricNames.ofDefaults(), retryRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param names custom metric names
     * @param retryRegistry the source of retries
     * @return The {@link TaggedRetryMetrics} instance.
     */
    public static TaggedRetryMetrics ofRetryRegistry(MetricNames names, RetryRegistry retryRegistry) {
        return new TaggedRetryMetrics(names, retryRegistry);
    }

    private final MetricNames names;
    private final RetryRegistry retryRegistry;

    private TaggedRetryMetrics(MetricNames names, RetryRegistry retryRegistry) {
        super();
        this.names = requireNonNull(names);
        this.retryRegistry = requireNonNull(retryRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Retry retry : retryRegistry.getAllRetries()) {
            addMetrics(registry, retry);
        }
        retryRegistry.getEventPublisher().onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        retryRegistry.getEventPublisher().onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        retryRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

    private void addMetrics(MeterRegistry registry, Retry retry) {
        Set<Meter.Id> idSet = new HashSet<>();

        idSet.add(Gauge.builder(names.getCallsMetricName(), retry, rt -> rt.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
                .description("The number of successful calls without a retry attempt")
                .tag(TagNames.NAME, retry.getName())
                .tag(TagNames.KIND, "successful_without_retry")
                .register(registry).getId());
        idSet.add(Gauge.builder(names.getCallsMetricName(), retry, rt -> rt.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt())
                .description("The number of successful calls after a retry attempt")
                .tag(TagNames.NAME, retry.getName())
                .tag(TagNames.KIND, "successful_with_retry")
                .register(registry).getId());
        idSet.add(Gauge.builder(names.getCallsMetricName(), retry, rt -> rt.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
                .description("The number of failed calls without a retry attempt")
                .tag(TagNames.NAME, retry.getName())
                .tag(TagNames.KIND, "failed_without_retry")
                .register(registry).getId());
        idSet.add(Gauge.builder(names.getCallsMetricName(), retry, rt -> rt.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
                .description("The number of failed calls after a retry attempt")
                .tag(TagNames.NAME, retry.getName())
                .tag(TagNames.KIND, "failed_with_retry")
                .register(registry).getId());

        meterIdMap.put(retry.getName(), idSet);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_RETRY_CALLS = "resilience4j.retry.calls";

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

        private String callsMetricName = DEFAULT_RETRY_CALLS;

        private MetricNames() {}

        /** Returns the metric name for retry calls, defaults to {@value DEFAULT_RETRY_CALLS}.
         * @return The metric name for retry calls.
         */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {
            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_RETRY_CALLS} with a given one.
             * @param callsMetricName The metric name for retry calls.
             * @return The builder.
             */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
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
