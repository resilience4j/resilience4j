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

public abstract class AbstractBulkheadMetrics extends Collector {

    protected final MetricNames names;

    protected AbstractBulkheadMetrics(MetricNames names) {
        this.names = requireNonNull(names);
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
