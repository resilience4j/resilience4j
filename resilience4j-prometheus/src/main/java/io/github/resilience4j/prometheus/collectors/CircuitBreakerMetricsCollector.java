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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/** Collects circuit breaker exposed {@link Metrics}. */
public class CircuitBreakerMetricsCollector extends Collector {

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of circuit breakers.
     *
     * @param names    the custom metric names
     * @param supplier the supplier of circuit breakers, note that supplier will be called one every {@link #collect()}
     */
    public static CircuitBreakerMetricsCollector ofSupplier(MetricNames names, Supplier<? extends Iterable<? extends CircuitBreaker>> supplier) {
        return new CircuitBreakerMetricsCollector(names, supplier);
    }

    /**
     * Creates a new collector using given {@code supplier} as source of circuit breakers.
     *
     * @param supplier the supplier of circuit breakers, note that supplier will be called one very {@link #collect()}
     */
    public static CircuitBreakerMetricsCollector ofSupplier(Supplier<? extends Iterable<? extends CircuitBreaker>> supplier) {
        return new CircuitBreakerMetricsCollector(MetricNames.ofDefaults(), supplier);
    }

    /**
     * Creates a new collector using given {@code registry} as source of circuit breakers.
     *
     * @param registry the source of circuit breakers
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
        return new CircuitBreakerMetricsCollector(MetricNames.ofDefaults(), registry::getAllCircuitBreakers);
    }

    /**
     * Creates a new collector for given {@code circuitBreakers}.
     *
     * @param circuitBreakers the circuit breakers to collect metrics for
     */
    public static CircuitBreakerMetricsCollector ofIterable(Iterable<? extends CircuitBreaker> circuitBreakers) {
        return new CircuitBreakerMetricsCollector(MetricNames.ofDefaults(), () -> circuitBreakers);
    }

    /**
     * Creates a new collector for a given {@code circuitBreaker}.
     *
     * @param circuitBreaker the circuit breaker to collect metrics for
     */
    public static CircuitBreakerMetricsCollector ofCircuitBreaker(CircuitBreaker circuitBreaker) {
        return ofIterable(singletonList(circuitBreaker));
    }

    private final MetricNames names;
    private final Supplier<? extends Iterable<? extends CircuitBreaker>> supplier;

    private CircuitBreakerMetricsCollector(MetricNames names, Supplier<? extends Iterable<? extends CircuitBreaker>> supplier) {
        this.names = Objects.requireNonNull(names);
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily stateFamily = new GaugeMetricFamily(
            names.getStateMetricName(),
            "The state of the circuit breaker: 0 - CLOSED, 1 - OPEN, 2 - HALF_OPEN",
            LabelNames.NAME
        );
        GaugeMetricFamily callsFamily = new GaugeMetricFamily(
            names.getCallsMetricName(),
            "The number of calls for a corresponding kind",
            LabelNames.NAME_AND_KIND
        );
        GaugeMetricFamily bufferedCallsFamily = new GaugeMetricFamily(
            names.getBufferedCallsMetricName(),
            "The number of buffered calls",
            LabelNames.NAME
        );
        GaugeMetricFamily maxBufferedCallsFamily = new GaugeMetricFamily(
            names.getMaxBufferedCallsMetricName(),
            "The maximum number of buffered calls",
            LabelNames.NAME
        );

        for (CircuitBreaker circuitBreaker : supplier.get()) {
            List<String> nameLabel = singletonList(circuitBreaker.getName());

            stateFamily.addMetric(nameLabel, circuitBreaker.getState().getOrder());

            Metrics metrics = circuitBreaker.getMetrics();
            callsFamily.addMetric(asList(circuitBreaker.getName(), "successful"), metrics.getNumberOfSuccessfulCalls());
            callsFamily.addMetric(asList(circuitBreaker.getName(), "failed"), metrics.getNumberOfFailedCalls());
            callsFamily.addMetric(asList(circuitBreaker.getName(), "not_permitted"), metrics.getNumberOfNotPermittedCalls());

            bufferedCallsFamily.addMetric(nameLabel, metrics.getNumberOfBufferedCalls());
            maxBufferedCallsFamily.addMetric(nameLabel, metrics.getMaxNumberOfBufferedCalls());
        }

        return asList(stateFamily, callsFamily, bufferedCallsFamily, maxBufferedCallsFamily);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME = "resilience4j_circuitbreaker_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME = "resilience4j_circuitbreaker_state";
        public static final String DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS = "resilience4j_circuitbreaker_buffered_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS = "resilience4j_circuitbreaker_max_buffered_calls";

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

        private String callsMetricName = DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME;
        private String stateMetricName = DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME;
        private String bufferedCallsMetricName = DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS;
        private String maxBufferedCallsMetricName = DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS;

        private MetricNames() {}

        /** Returns the metric name for circuit breaker calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME}. */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /** Returns the metric name for currently buffered calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME}. */
        public String getBufferedCallsMetricName() {
            return bufferedCallsMetricName;
        }

        /** Returns the metric name for max buffered calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME}. */
        public String getMaxBufferedCallsMetricName() {
            return maxBufferedCallsMetricName;
        }

        /** Returns the metric name for state, defaults to {@value DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME}. */
        public String getStateMetricName() {
            return stateMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {
            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME} with a given one. */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME} with a given one. */
            public Builder stateMetricName(String stateMetricName) {
                metricNames.stateMetricName = requireNonNull(stateMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS} with a given one. */
            public Builder bufferedCallsMetricName(String bufferedCallsMetricName) {
                metricNames.bufferedCallsMetricName = requireNonNull(bufferedCallsMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS} with a given one. */
            public Builder maxBufferedCallsMetricName(String maxBufferedCallsMetricName) {
                metricNames.maxBufferedCallsMetricName = requireNonNull(maxBufferedCallsMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance. */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
