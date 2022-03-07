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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.resilience4j.prometheus.LabelNames.NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

public abstract class AbstractCircuitBreakerMetrics extends Collector {

    protected static final String KIND_FAILED = "failed";
    protected static final String KIND_SUCCESSFUL = "successful";
    protected static final String KIND_IGNORED = "ignored";
    protected static final String KIND_NOT_PERMITTED = "not_permitted";

    protected static final List<String> NAME_AND_STATE = asList("name", "state");
    protected static final List<String> NAME_AND_KIND = asList("name", "kind");

    protected final MetricNames names;
    protected final CollectorRegistry collectorRegistry = new CollectorRegistry(true);
    protected final Histogram callsHistogram;
    protected final List<String> labelValues;

    protected AbstractCircuitBreakerMetrics(MetricNames names, MetricOptions options) {
        this.names = requireNonNull(names);
        requireNonNull(options);
        String[] labelsKeys = Stream.of(NAME_AND_KIND, options.labels.keySet()).flatMap(Collection::stream).toArray((String[]::new));
        labelValues = new ArrayList<>(options.labels.values());
        callsHistogram = Histogram
            .build(names.getCallsMetricName(), "Total number of calls by kind")
            .labelNames(labelsKeys)
            .buckets(options.getBuckets())
            .create().register(collectorRegistry);
    }

    protected void addMetrics(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
            .onCallNotPermitted(
                event -> callsHistogram.labels(Stream.concat(Stream.of(circuitBreaker.getName(), KIND_NOT_PERMITTED), labelValues.stream()).toArray((String[]::new)))
                    .observe(0))
            .onIgnoredError(event -> callsHistogram.
                labels(Stream.concat(Stream.of(circuitBreaker.getName(), KIND_IGNORED), labelValues.stream()).toArray((String[]::new)))
                .observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
            .onSuccess(event -> callsHistogram.
                labels(Stream.concat(Stream.of(circuitBreaker.getName(), KIND_SUCCESSFUL), labelValues.stream()).toArray((String[]::new)))
                .observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND))
            .onError(event -> callsHistogram.
                labels(Stream.concat(Stream.of(circuitBreaker.getName(), KIND_FAILED), labelValues.stream()).toArray((String[]::new)))
                .observe(event.getElapsedDuration().toNanos() / Collector.NANOSECONDS_PER_SECOND));
    }

    protected List<MetricFamilySamples> collectGaugeSamples(List<CircuitBreaker> circuitBreakers) {
        Set<String> customTags = circuitBreakers.stream().findFirst().map(circuitBreaker -> circuitBreaker.getTags().keySet()).orElse(emptySet());

        GaugeMetricFamily stateFamily = new GaugeMetricFamily(
            names.getStateMetricName(),
            "The state of the circuit breaker:",
            Stream.of(NAME_AND_STATE, customTags).flatMap(Collection::stream).collect(Collectors.toList())
        );
        GaugeMetricFamily bufferedCallsFamily = new GaugeMetricFamily(
            names.getBufferedCallsMetricName(),
            "The number of buffered calls",
            Stream.of(NAME_AND_KIND, customTags).flatMap(Collection::stream).collect(Collectors.toList())
        );
        GaugeMetricFamily slowCallsFamily = new GaugeMetricFamily(
            names.getSlowCallsMetricName(),
            "The number of slow calls",
            Stream.of(NAME_AND_KIND, customTags).flatMap(Collection::stream).collect(Collectors.toList())
        );

        GaugeMetricFamily failureRateFamily = new GaugeMetricFamily(
            names.getFailureRateMetricName(),
            "The failure rate",
            Stream.of(NAME, customTags).flatMap(Collection::stream).collect(Collectors.toList())
        );

        GaugeMetricFamily slowCallRateFamily = new GaugeMetricFamily(
            names.getSlowCallRateMetricName(),
            "The slow call rate",
            Stream.of(NAME, customTags).flatMap(Collection::stream).collect(Collectors.toList())
        );

        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            final CircuitBreaker.State[] states = CircuitBreaker.State.values();
            for (CircuitBreaker.State state : states) {
                stateFamily.addMetric(
                    Stream.concat(
                        Stream.of(circuitBreaker.getName(), state.name().toLowerCase()), circuitBreaker.getTags().values().stream()).collect(Collectors.toList()),
                    circuitBreaker.getState() == state ? 1 : 0);
            }

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            bufferedCallsFamily.addMetric(
                Stream.concat(
                    Stream.of(circuitBreaker.getName(), KIND_SUCCESSFUL), circuitBreaker.getTags().values().stream()).collect(Collectors.toList()),
                metrics.getNumberOfSuccessfulCalls());
            bufferedCallsFamily.addMetric(
                Stream.concat(
                    Stream.of(circuitBreaker.getName(), KIND_FAILED), circuitBreaker.getTags().values().stream()).collect(Collectors.toList()),
                metrics.getNumberOfFailedCalls());
            slowCallsFamily.addMetric(
                Stream.concat(
                    Stream.of(circuitBreaker.getName(), KIND_SUCCESSFUL), circuitBreaker.getTags().values().stream()).collect(Collectors.toList()),
                metrics.getNumberOfSlowSuccessfulCalls());
            slowCallsFamily.addMetric(
                Stream.concat(
                    Stream.of(circuitBreaker.getName(), KIND_FAILED), circuitBreaker.getTags().values().stream()).collect(Collectors.toList()),
                metrics.getNumberOfSlowFailedCalls());
            failureRateFamily.addMetric(
                Stream.concat(
                    Stream.of(circuitBreaker.getName()), circuitBreaker.getTags().values().stream()).collect(Collectors.toList()),
                metrics.getFailureRate());
            slowCallRateFamily.addMetric(
                Stream.concat(
                    Stream.of(circuitBreaker.getName()), circuitBreaker.getTags().values().stream()).collect(Collectors.toList()),
                metrics.getSlowCallRate());
        }
        return asList(stateFamily, bufferedCallsFamily, slowCallsFamily, failureRateFamily,
            slowCallRateFamily);
    }

    /**
     * Defines possible configuration for metric names.
     */
    public static class MetricNames {

        public static final String DEFAULT_CIRCUIT_BREAKER_CALLS = "resilience4j_circuitbreaker_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_STATE = "resilience4j_circuitbreaker_state";
        public static final String DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS = "resilience4j_circuitbreaker_buffered_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS = "resilience4j_circuitbreaker_slow_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE = "resilience4j_circuitbreaker_failure_rate";
        public static final String DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE = "resilience4j_circuitbreaker_slow_call_rate";
        private String callsMetricName = DEFAULT_CIRCUIT_BREAKER_CALLS;
        private String stateMetricName = DEFAULT_CIRCUIT_BREAKER_STATE;
        private String bufferedCallsMetricName = DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS;
        private String slowCallsMetricName = DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS;
        private String failureRateMetricName = DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE;
        private String slowCallRateMetricName = DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE;

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
         * Returns the metric name for circuit breaker calls, defaults to {@value
         * DEFAULT_CIRCUIT_BREAKER_CALLS}.
         */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /**
         * Returns the metric name for currently buffered calls, defaults to {@value
         * DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS}.
         */
        public String getBufferedCallsMetricName() {
            return bufferedCallsMetricName;
        }

        /**
         * Returns the metric name for currently slow calls, defaults to {@value
         * DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS}.
         */
        public String getSlowCallsMetricName() {
            return slowCallsMetricName;
        }

        /**
         * Returns the metric name for failure rate, defaults to {@value
         * DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE}.
         */
        public String getFailureRateMetricName() {
            return failureRateMetricName;
        }

        /**
         * Returns the metric name for slow call rate, defaults to {@value
         * DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE}.
         */
        public String getSlowCallRateMetricName() {
            return slowCallRateMetricName;
        }

        /**
         * Returns the metric name for state, defaults to {@value DEFAULT_CIRCUIT_BREAKER_STATE}.
         */
        public String getStateMetricName() {
            return stateMetricName;
        }

        /**
         * Helps building custom instance of {@link MetricNames}.
         */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_CALLS}
             * with a given one.
             */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_STATE}
             * with a given one.
             */
            public Builder stateMetricName(String stateMetricName) {
                metricNames.stateMetricName = requireNonNull(stateMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS}
             * with a given one.
             */
            public Builder bufferedCallsMetricName(String bufferedCallsMetricName) {
                metricNames.bufferedCallsMetricName = requireNonNull(bufferedCallsMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS}
             * with a given one.
             */
            public Builder slowCallsMetricName(String slowCallsMetricName) {
                metricNames.slowCallsMetricName = requireNonNull(slowCallsMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE}
             * with a given one.
             */
            public Builder failureRateMetricName(String failureRateMetricName) {
                metricNames.failureRateMetricName = requireNonNull(failureRateMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE}
             * with a given one.
             */
            public Builder slowCallRateMetricName(String slowCallRateMetricName) {
                metricNames.slowCallRateMetricName = requireNonNull(slowCallRateMetricName);
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

    /**
     * Defines possible configuration for metric options.
     */
    public static class MetricOptions {

        private static final double[] DEFAULT_BUCKETS = new double[]{.005, .01, .025, .05, .075, .1,
            .25, .5, .75, 1, 2.5, 5, 7.5, 10};
        private double[] buckets = DEFAULT_BUCKETS;
        private Map<String, String> labels = emptyMap();

        private MetricOptions() {
        }

        /**
         * Returns a builder for creating custom metric options. Note that all options have default
         * values.
         */
        public static Builder custom() {
            return new Builder();
        }

        /**
         * Returns default metric options.
         */
        public static MetricOptions ofDefaults() {
            return new MetricOptions();
        }

        /**
         * Returns the Histogram buckets, defaults to {@link MetricOptions#DEFAULT_BUCKETS}.
         */
        public double[] getBuckets() {
            return buckets;
        }

        /**
         * Returns the Histogram labels, defaults to emptyMap.
         */
        public Map<String, String> getLabels() {
            return labels;
        }

        /**
         * Helps building custom instance of {@link MetricOptions}.
         */
        public static class Builder {

            private final MetricOptions metricOptions = new MetricOptions();

            /**
             * Overrides the default Histogram buckets {@link MetricOptions#DEFAULT_BUCKETS} with a
             * given one.
             */
            public Builder buckets(double[] buckets) {
                metricOptions.buckets = requireNonNull(buckets);
                return this;
            }

            /**
             * Overrides the default Histogram labels with a given one.
             */
            public Builder labels(Map<String, String> labels) {
                metricOptions.labels = requireNonNull(labels);
                return this;
            }

            /**
             * Builds {@link MetricOptions} instance.
             */
            public MetricOptions build() {
                return metricOptions;
            }
        }
    }
}
