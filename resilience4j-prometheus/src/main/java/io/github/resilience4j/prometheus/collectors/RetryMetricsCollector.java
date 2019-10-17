/*
 * Copyright 2019 Robert Winkler
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
package io.github.resilience4j.prometheus.collectors;

import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Collects Retry exposed {@link Metrics}.
 */
public class RetryMetricsCollector extends Collector {

    private final MetricNames names;
    private final RetryRegistry retryRegistry;

    private RetryMetricsCollector(MetricNames names, RetryRegistry retryRegistry) {
        this.names = requireNonNull(names);
        this.retryRegistry = requireNonNull(retryRegistry);
    }

    /**
     * Creates a new collector with custom metric names and using given {@code supplier} as source
     * of retries.
     *
     * @param names         the custom metric names
     * @param retryRegistry the source of retries
     */
    public static RetryMetricsCollector ofRetryRegistry(MetricNames names,
        RetryRegistry retryRegistry) {
        return new RetryMetricsCollector(names, retryRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of retries.
     *
     * @param retryRegistry the source of retries
     */
    public static RetryMetricsCollector ofRetryRegistry(RetryRegistry retryRegistry) {
        return new RetryMetricsCollector(MetricNames.ofDefaults(), retryRegistry);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily retryCallsFamily = new GaugeMetricFamily(
            names.getCallsMetricName(),
            "The number of calls",
            LabelNames.NAME_AND_KIND
        );

        for (Retry retry : retryRegistry.getAllRetries()) {
            retryCallsFamily.addMetric(asList(retry.getName(), "successful_without_retry"),
                retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryCallsFamily.addMetric(asList(retry.getName(), "successful_with_retry"),
                retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            retryCallsFamily.addMetric(asList(retry.getName(), "failed_without_retry"),
                retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            retryCallsFamily.addMetric(asList(retry.getName(), "failed_with_retry"),
                retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());

        }

        return Collections.singletonList(retryCallsFamily);
    }

    /**
     * Defines possible configuration for metric names.
     */
    public static class MetricNames {

        public static final String DEFAULT_RETRY_CALLS = "resilience4j_retry_calls";
        private String callsMetricName = DEFAULT_RETRY_CALLS;

        private MetricNames() {
        }

        /**
         * Returns a builder for creating custom metric names. Note that names have default values,
         * so only desired metrics can be renamed.
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

        /**
         * Returns the metric name for retry calls, defaults to {@value DEFAULT_RETRY_CALLS}.
         *
         * @return The metric name for retry calls.
         */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /**
         * Helps building custom instance of {@link MetricNames}.
         */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_RETRY_CALLS} with a
             * given one.
             *
             * @param callsMetricName The metric name for retry calls.
             * @return The builder.
             */
            public Builder callsMetricName(String callsMetricName) {
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
