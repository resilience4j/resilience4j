/*
 * Copyright 2019 Ingyu Hwang, Mahmoud Romeh
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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

abstract class AbstractBulkheadMetrics extends AbstractMetrics {

    protected final MetricNames names;

    protected AbstractBulkheadMetrics(MetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, Bulkhead bulkhead) {
        List<Tag> customTags = mapToTagsList(bulkhead.getTags().toJavaMap());
        addMetrics(meterRegistry, bulkhead, customTags);
    }

    private void addMetrics(MeterRegistry meterRegistry, Bulkhead bulkhead, List<Tag> customTags) {
        Set<Meter.Id> idSet = new HashSet<>();

        idSet.add(Gauge.builder(names.getAvailableConcurrentCallsMetricName(), bulkhead,
            bh -> bh.getMetrics().getAvailableConcurrentCalls())
            .description("The number of available permissions")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());
        idSet.add(Gauge.builder(names.getMaxAllowedConcurrentCallsMetricName(), bulkhead,
            bh -> bh.getMetrics().getMaxAllowedConcurrentCalls())
            .description("The maximum number of available permissions")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());

        meterIdMap.put(bulkhead.getName(), idSet);

    }

    /**
     * Defines possible configuration for metric names.
     */
    public static class MetricNames {

        private static final String DEFAULT_PREFIX = "resilience4j.bulkhead";

        public static final String DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME =
            DEFAULT_PREFIX + ".available.concurrent.calls";
        public static final String DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME =
            DEFAULT_PREFIX + ".max.allowed.concurrent.calls";
        private String availableConcurrentCallsMetricName = DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME;
        private String maxAllowedConcurrentCallsMetricName = DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME;

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
         * @return The default {@link TaggedBulkheadMetrics.MetricNames} instance.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        /**
         * Returns the metric name for bulkhead concurrent calls, defaults to {@value
         * DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME}.
         *
         * @return The available concurrent calls metric name.
         */
        public String getAvailableConcurrentCallsMetricName() {
            return availableConcurrentCallsMetricName;
        }

        /**
         * Returns the metric name for bulkhead max available concurrent calls, defaults to {@value
         * DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME}.
         *
         * @return The max allowed concurrent calls metric name.
         */
        public String getMaxAllowedConcurrentCallsMetricName() {
            return maxAllowedConcurrentCallsMetricName;
        }

        /**
         * Helps building custom instance of {@link TaggedBulkheadMetrics.MetricNames}.
         */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /**
             * Overrides the default metric name {@value TaggedBulkheadMetrics.MetricNames#DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME}
             * with a given one.
             *
             * @param availableConcurrentCallsMetricName The available concurrent calls metric
             *                                           name.
             * @return The builder.
             */
            public Builder availableConcurrentCallsMetricName(
                String availableConcurrentCallsMetricName) {
                metricNames.availableConcurrentCallsMetricName = requireNonNull(
                    availableConcurrentCallsMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value TaggedBulkheadMetrics.MetricNames#DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME}
             * with a given one.
             *
             * @param maxAllowedConcurrentCallsMetricName The max allowed concurrent calls metric
             *                                            name.
             * @return The builder.
             */
            public Builder maxAllowedConcurrentCallsMetricName(
                String maxAllowedConcurrentCallsMetricName) {
                metricNames.maxAllowedConcurrentCallsMetricName = requireNonNull(
                    maxAllowedConcurrentCallsMetricName);
                return this;
            }

            /**
             * Builds {@link TaggedBulkheadMetrics.MetricNames} instance.
             *
             * @return The built {@link TaggedBulkheadMetrics.MetricNames} instance.
             */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
