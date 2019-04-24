/*
 * Copyright 2019 Yevhenii Voievodin
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

import io.github.resilience4j.micrometer.RetryMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static io.github.resilience4j.retry.Retry.Metrics;
import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register retry exposed {@link Metrics metrics}.
 * The main difference from {@link RetryMetrics} is that this binder uses tags
 * to distinguish between metrics.
 */
public class TaggedRetryMetrics implements MeterBinder {

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param registry the source of retries
     * @return The {@link TaggedRetryMetrics} instance.
     */
    public static TaggedRetryMetrics ofRetryRegistry(RetryRegistry registry) {
        return new TaggedRetryMetrics(MetricNames.ofDefaults(), registry.getAllRetries());
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param names custom metric names
     * @param registry the source of retries
     * @return The {@link TaggedRetryMetrics} instance.
     */
    public static TaggedRetryMetrics ofRetryRegistry(MetricNames names, RetryRegistry registry) {
        return new TaggedRetryMetrics(names, registry.getAllRetries());
    }

    private final MetricNames names;
    private final Iterable<? extends Retry> retries;

    private TaggedRetryMetrics(MetricNames names, Iterable<? extends Retry> retries) {
        this.names = requireNonNull(names);
        this.retries = requireNonNull(retries);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Retry retry : retries) {
            Gauge.builder(names.getCallsMetricName(), retry, (rt) -> rt.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
                    .tag(TagNames.NAME, retry.getName())
                    .tag(TagNames.KIND, "successful_without_retry")
                    .register(registry);
            Gauge.builder(names.getCallsMetricName(), retry, (rt) -> rt.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt())
                    .tag(TagNames.NAME, retry.getName())
                    .tag(TagNames.KIND, "successful_with_retry")
                    .register(registry);
            Gauge.builder(names.getCallsMetricName(), retry, (rt) -> rt.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
                    .tag(TagNames.NAME, retry.getName())
                    .tag(TagNames.KIND, "failed_without_retry")
                    .register(registry);
            Gauge.builder(names.getCallsMetricName(), retry, (rt) -> rt.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
                    .tag(TagNames.NAME, retry.getName())
                    .tag(TagNames.KIND, "failed_with_retry")
                    .register(registry);
        }
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_RETRY_CALLS = "resilience4j_retry_calls";

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
