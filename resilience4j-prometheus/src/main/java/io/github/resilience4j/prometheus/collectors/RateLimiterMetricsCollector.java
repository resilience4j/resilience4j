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

import io.github.resilience4j.prometheus.LabelNames;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static io.github.resilience4j.ratelimiter.RateLimiter.Metrics;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/** Collects RateLimiter exposed {@link Metrics}. */
public class RateLimiterMetricsCollector extends Collector {

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of rate limiters.
     *
     * @param names    the custom metric names
     * @param rateLimiterRegistry the source of rate limiters
     */
    public static RateLimiterMetricsCollector ofRateLimiterRegistry(MetricNames names, RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterMetricsCollector(names, rateLimiterRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of rate limiters.
     *
     * @param rateLimiterRegistry the source of rate limiters
     */
    public static RateLimiterMetricsCollector ofRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterMetricsCollector(MetricNames.ofDefaults(), rateLimiterRegistry);
    }

    private final MetricNames names;
    private final RateLimiterRegistry rateLimiterRegistry;

    private RateLimiterMetricsCollector(MetricNames names, RateLimiterRegistry rateLimiterRegistry) {
        this.names = requireNonNull(names);
        this.rateLimiterRegistry = requireNonNull(rateLimiterRegistry);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily availablePermissionsFamily = new GaugeMetricFamily(
            names.getAvailablePermissionsMetricName(),
            "The number of available permissions",
            LabelNames.NAME
        );
        GaugeMetricFamily waitingThreadsFamily = new GaugeMetricFamily(
            names.getWaitingThreadsMetricName(),
            "The number of waiting threads",
            LabelNames.NAME
        );

        for (RateLimiter rateLimiter : rateLimiterRegistry.getAllRateLimiters()) {
            List<String> nameLabel = singletonList(rateLimiter.getName());
            availablePermissionsFamily.addMetric(nameLabel, rateLimiter.getMetrics().getAvailablePermissions());
            waitingThreadsFamily.addMetric(nameLabel, rateLimiter.getMetrics().getNumberOfWaitingThreads());
        }

        return asList(availablePermissionsFamily, waitingThreadsFamily);
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
