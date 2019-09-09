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

public abstract class AbstractRateLimiterMetrics extends Collector {

    protected final MetricNames names;

    protected AbstractRateLimiterMetrics(MetricNames names) {
        this.names = requireNonNull(names);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME = "resilience4j_ratelimiter_available_permissions";
        public static final String DEFAULT_WAITING_THREADS_METRIC_NAME = "resilience4j_ratelimiter_waiting_threads";

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

        private String availablePermissionsMetricName = DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
        private String waitingThreadsMetricName = DEFAULT_WAITING_THREADS_METRIC_NAME;

        /** Returns the metric name for available permissions, defaults to {@value DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME}. */
        public String getAvailablePermissionsMetricName() {
            return availablePermissionsMetricName;
        }

        /** Returns the metric name for waiting threads, defaults to {@value DEFAULT_WAITING_THREADS_METRIC_NAME}. */
        public String getWaitingThreadsMetricName() {
            return waitingThreadsMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME} with a given one. */
            public Builder availablePermissionsMetricName(String availablePermissionsMetricName) {
                metricNames.availablePermissionsMetricName = requireNonNull(availablePermissionsMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_WAITING_THREADS_METRIC_NAME} with a given one. */
            public Builder waitingThreadsMetricName(String waitingThreadsMetricName) {
                metricNames.waitingThreadsMetricName = requireNonNull(waitingThreadsMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance. */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
