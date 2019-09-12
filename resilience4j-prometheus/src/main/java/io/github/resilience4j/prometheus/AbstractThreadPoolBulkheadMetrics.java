/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.prometheus;

import io.prometheus.client.Collector;

import static java.util.Objects.requireNonNull;

public abstract class AbstractThreadPoolBulkheadMetrics extends Collector {

    protected final MetricNames names;

    protected AbstractThreadPoolBulkheadMetrics(MetricNames names) {
        this.names = requireNonNull(names);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME = "resilience4j_thread_pool_bulkhead_current_thread_pool_size";
        public static final String DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME = "resilience4j_thread_pool_bulkhead_available_queue_capacity";

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

        private String currentThreadPoolSizeName = DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME;
        private String availableQueueCapacityName = DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME;

        private MetricNames() {}

        /**
         * Returns the metric name for bulkhead concurrent calls,
         * defaults to {@value DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME}.
         */
        public String getCurrentThreadPoolSizeName() {
            return currentThreadPoolSizeName;
        }

        /**
         * Returns the metric name for bulkhead max available concurrent calls,
         * defaults to {@value DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME}.
         */
        public String getAvailableQueueCapacityName() {
            return availableQueueCapacityName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_CURRENT_THREAD_POOL_SIZE_NAME} with a given one. */
            public Builder availableConcurrentCallsMetricName(String currentThreadPoolSizeName) {
                metricNames.currentThreadPoolSizeName = requireNonNull(currentThreadPoolSizeName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_AVAILABLE_QUEUE_CAPACITY_NAME} with a given one. */
            public Builder maxAllowedConcurrentCallsMetricName(String availableQueueCapacityName) {
                metricNames.availableQueueCapacityName = requireNonNull(availableQueueCapacityName);
                return this;
            }

            /** Builds {@link MetricNames} instance. */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
