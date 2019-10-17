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
package io.github.resilience4j.prometheus.collectors;

import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.prometheus.LabelNames;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Collects bulkhead exposed {@link Metrics}.
 */
public class ThreadPoolBulkheadMetricsCollector extends Collector {

    private final MetricNames names;
    private final ThreadPoolBulkheadRegistry bulkheadRegistry;

    private ThreadPoolBulkheadMetricsCollector(MetricNames names,
        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        this.names = requireNonNull(names);
        this.bulkheadRegistry = requireNonNull(bulkheadRegistry);
    }

    /**
     * Creates a new collector with custom metric names and using given {@code supplier} as source
     * of bulkheads.
     *
     * @param names            the custom metric names
     * @param bulkheadRegistry the source of bulkheads
     */
    public static ThreadPoolBulkheadMetricsCollector ofBulkheadRegistry(MetricNames names,
        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new ThreadPoolBulkheadMetricsCollector(names, bulkheadRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of bulkheads.
     *
     * @param bulkheadRegistry the source of bulkheads
     */
    public static ThreadPoolBulkheadMetricsCollector ofBulkheadRegistry(
        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new ThreadPoolBulkheadMetricsCollector(MetricNames.ofDefaults(), bulkheadRegistry);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily availableCallsFamily = new GaugeMetricFamily(
            names.getCurrentThreadPoolSizeName(),
            "The number of currently used bulkhead threads",
            LabelNames.NAME
        );
        GaugeMetricFamily maxAllowedCallsFamily = new GaugeMetricFamily(
            names.getAvailableQueueCapacityName(),
            "The number of available bulkhead queue slots",
            LabelNames.NAME
        );

        for (ThreadPoolBulkhead bulkhead : bulkheadRegistry.getAllBulkheads()) {
            List<String> labelValues = singletonList(bulkhead.getName());
            availableCallsFamily.addMetric(labelValues, bulkhead.getMetrics().getThreadPoolSize());
            maxAllowedCallsFamily
                .addMetric(labelValues, bulkhead.getMetrics().getRemainingQueueCapacity());
        }

        return asList(availableCallsFamily, maxAllowedCallsFamily);
    }

    /**
     * Defines possible configuration for metric names.
     */
    public static class MetricNames {

        public static final String DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME = "resilience4j_thread_pool_bulkhead_current_thread_pool_size";
        public static final String DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME = "resilience4j_thread_pool_bulkhead_available_queue_capacity";
        private String currentThreadPoolSizeName = DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME;
        private String availableQueueCapacityName = DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME;

        private MetricNames() {
        }

        /**
         * Returns a builder for creating custom metric names. Note that names have default values,
         * so only desired metrics can be renamed.
         */
        public static Builder custom() {
            return new Builder();
        }

        /**
         * Returns default metric names.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        /**
         * Returns the metric name for bulkhead concurrent calls, defaults to {@value
         * DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME}.
         */
        public String getCurrentThreadPoolSizeName() {
            return currentThreadPoolSizeName;
        }

        /**
         * Returns the metric name for bulkhead max available concurrent calls, defaults to {@value
         * DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME}.
         */
        public String getAvailableQueueCapacityName() {
            return availableQueueCapacityName;
        }

        /**
         * Helps building custom instance of {@link MetricNames}.
         */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME}
             * with a given one.
             */
            public Builder availableConcurrentCallsMetricName(String currentThreadPoolSizeName) {
                metricNames.currentThreadPoolSizeName = requireNonNull(currentThreadPoolSizeName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME}
             * with a given one.
             */
            public Builder maxAllowedConcurrentCallsMetricName(String availableQueueCapacityName) {
                metricNames.availableQueueCapacityName = requireNonNull(availableQueueCapacityName);
                return this;
            }

            /**
             * Builds {@link MetricNames} instance.
             */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
