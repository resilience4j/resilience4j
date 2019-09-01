/*
 * Copyright 2019 authors
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

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Collects TimeLimiter exposed events.
 */
public class TimeLimiterMetricsCollector extends Collector {

    static final String KIND_SUCCESSFUL = "successful";
    static final String KIND_FAILED = "failed";
    static final String KIND_TIMEOUT = "timeout";

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of time limiters.
     *
     * @param names    the custom metric names
     * @param timeLimiterRegistry the source of time limiters
     */
    public static TimeLimiterMetricsCollector ofTimeLimiterRegistry(TimeLimiterMetricsCollector.MetricNames names, TimeLimiterRegistry timeLimiterRegistry) {
        return new TimeLimiterMetricsCollector(names, timeLimiterRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of time limiters.
     *
     * @param timeLimiterRegistry the source of time limiters
     */
    public static TimeLimiterMetricsCollector ofTimeLimiterRegistry(TimeLimiterRegistry timeLimiterRegistry) {
        return new TimeLimiterMetricsCollector(TimeLimiterMetricsCollector.MetricNames.ofDefaults(), timeLimiterRegistry);
    }

    private final MetricNames names;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final CollectorRegistry collectorRegistry = new CollectorRegistry(true);
    private final Counter callsCounter;

    private TimeLimiterMetricsCollector(MetricNames names, TimeLimiterRegistry timeLimiterRegistry) {
        this.names = requireNonNull(names);
        this.timeLimiterRegistry = requireNonNull(timeLimiterRegistry);
        this.callsCounter = Counter.build(names.getCallsMetricName(),
                "Total number of calls by kind")
                .labelNames("name", "kind")
                .create().register(collectorRegistry);

        this.timeLimiterRegistry.getAllTimeLimiters()
                .forEach(this::addMetrics);
        this.timeLimiterRegistry.getEventPublisher()
                .onEntryAdded(event -> addMetrics(event.getAddedEntry()));
    }

    private void addMetrics(TimeLimiter timeLimiter) {
        String name = timeLimiter.getName();
        timeLimiter.getEventPublisher()
                .onSuccess(event -> callsCounter.labels(name, KIND_SUCCESSFUL).inc())
                .onError(event -> callsCounter.labels(name, KIND_FAILED).inc())
                .onTimeout(event -> callsCounter.labels(name, KIND_TIMEOUT).inc());
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return Collections.list(collectorRegistry.metricFamilySamples());
    }

    /**
     * Defines possible configuration for metric names.
     */
    public static class MetricNames {

        public static final String DEFAULT_CALLS_METRIC_NAME = "resilience4j_timelimiter_calls";

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
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

        private String callsMetricName = DEFAULT_CALLS_METRIC_NAME;

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
             * Overrides the default metric name {@value MetricNames#DEFAULT_CALLS_METRIC_NAME} with a given one.
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
