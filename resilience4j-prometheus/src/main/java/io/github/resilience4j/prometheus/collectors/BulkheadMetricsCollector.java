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
package io.github.resilience4j.prometheus.collectors;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/** Collects bulkhead exposed {@link Metrics}. */
public class BulkheadMetricsCollector extends Collector {

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of bulkheads.
     *
     * @param names    the custom metric names
     * @param supplier the supplier of bulkheads, note that supplier will be called one every {@link #collect()}
     */
    public static BulkheadMetricsCollector ofSupplier(MetricNames names, Supplier<? extends Iterable<? extends Bulkhead>> supplier) {
        return new BulkheadMetricsCollector(names, supplier);
    }

    /**
     * Creates a new collector using given {@code supplier} as source of bulkheads.
     *
     * @param supplier the supplier of bulkheads, note that supplier will be called one very {@link #collect()}
     */
    public static BulkheadMetricsCollector ofSupplier(Supplier<? extends Iterable<? extends Bulkhead>> supplier) {
        return new BulkheadMetricsCollector(MetricNames.ofDefaults(), supplier);
    }

    /**
     * Creates a new collector using given {@code registry} as source of bulkheads.
     *
     * @param registry the source of bulkheads
     */
    public static BulkheadMetricsCollector ofBulkheadRegistry(BulkheadRegistry registry) {
        return new BulkheadMetricsCollector(MetricNames.ofDefaults(), registry::getAllBulkheads);
    }

    /**
     * Creates a new collector using given {@code bulkheads} iterable as source of bulkheads.
     *
     * @param bulkheads the source of bulkheads
     */
    public static BulkheadMetricsCollector ofIterable(Iterable<? extends Bulkhead> bulkheads) {
        return new BulkheadMetricsCollector(MetricNames.ofDefaults(), () -> bulkheads);
    }

    /**
     * Creates a new collector for a given {@code bulkhead}.
     *
     * @param bulkhead the bulkhead to collect metrics for
     */
    public static BulkheadMetricsCollector ofBulkhead(Bulkhead bulkhead) {
        return ofIterable(singletonList(bulkhead));
    }

    private final MetricNames names;
    private final Supplier<? extends Iterable<? extends Bulkhead>> bulkheadsSupplier;

    private BulkheadMetricsCollector(MetricNames names, Supplier<? extends Iterable<? extends Bulkhead>> bulkheadsSupplier) {
        this.names = Objects.requireNonNull(names);
        this.bulkheadsSupplier = Objects.requireNonNull(bulkheadsSupplier);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily availableCallsFamily = new GaugeMetricFamily(
            names.getAvailableConcurrentCallsMetricName(),
            "The number of available concurrent calls",
            LabelNames.NAME
        );
        GaugeMetricFamily maxAllowedCallsFamily = new GaugeMetricFamily(
            names.getMaxAllowedConcurrentCallsMetricName(),
            "The maximum number of allowed concurrent calls",
            LabelNames.NAME
        );

        for (Bulkhead bulkhead: bulkheadsSupplier.get()) {
            List<String> labelValues = singletonList(bulkhead.getName());
            availableCallsFamily.addMetric(labelValues, bulkhead.getMetrics().getAvailableConcurrentCalls());
            maxAllowedCallsFamily.addMetric(labelValues, bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
        }

        return asList(availableCallsFamily, maxAllowedCallsFamily);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME = "resilience4j_bulkhead_available_concurrent_calls";
        public static final String DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME = "resilience4j_bulkhead_max_allowed_concurrent_calls";

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
         */
        public static Builder custom() {
            return new Builder();
        }

        /** Returns default metric names. */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        private String availableConcurrentCallsMetricName = DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME;
        private String maxAllowedConcurrentCallsMetricName = DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME;

        private MetricNames() {}

        /**
         * Returns the metric name for bulkhead concurrent calls,
         * defaults to {@value DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME}.
         */
        public String getAvailableConcurrentCallsMetricName() {
            return availableConcurrentCallsMetricName;
        }

        /**
         * Returns the metric name for bulkhead max available concurrent calls,
         * defaults to {@value DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME}.
         */
        public String getMaxAllowedConcurrentCallsMetricName() {
            return maxAllowedConcurrentCallsMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME} with a given one. */
            public Builder availableConcurrentCallsMetricName(String availableConcurrentCallsMetricNames) {
                metricNames.availableConcurrentCallsMetricName = requireNonNull(availableConcurrentCallsMetricNames);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME} with a given one. */
            public Builder maxAllowedConcurrentCallsMetricName(String maxAllowedConcurrentCallsMetricName) {
                metricNames.maxAllowedConcurrentCallsMetricName = requireNonNull(maxAllowedConcurrentCallsMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance. */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
