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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;

import static java.util.Objects.requireNonNull;

public abstract class AbstractTimeLimiterMetrics extends Collector {

    protected static final String KIND_SUCCESSFUL = "successful";
    protected static final String KIND_FAILED = "failed";
    protected static final String KIND_TIMEOUT = "timeout";

    protected final MetricNames names;
    protected final CollectorRegistry collectorRegistry = new CollectorRegistry(true);
    protected final Counter callsCounter;

    protected AbstractTimeLimiterMetrics(MetricNames names) {
        this.names = requireNonNull(names);
        callsCounter = Counter.build(names.getCallsMetricName(),
            "Total number of calls by kind")
            .labelNames("name", "kind")
            .create().register(collectorRegistry);
    }

    /**
     * Defines possible configuration for metric names.
     */
    public static class MetricNames {

        public static final String DEFAULT_CALLS_METRIC_NAME = "resilience4j_timelimiter_calls";
        private String callsMetricName = DEFAULT_CALLS_METRIC_NAME;

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
         * Returns the metric name for calls, defaults to {@value DEFAULT_CALLS_METRIC_NAME}.
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
             * Overrides the default metric name {@value MetricNames#DEFAULT_CALLS_METRIC_NAME} with
             * a given one.
             */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
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
