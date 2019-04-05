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
package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnIgnoredErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.micrometer.CircuitBreakerMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register circuit breaker exposed {@link Metrics metrics}.
 * The main difference from {@link CircuitBreakerMetrics} is that this binder uses tags
 * to distinguish between circuit breaker instances.
 */
public class TaggedCircuitBreakerMetrics implements MeterBinder {

    /**
     * Creates a new binder that uses given {@code registry} as source of circuit breakers.
     *
     * @param metricNames custom metric names
     * @param registry the source of circuit breakers
     */
    public static TaggedCircuitBreakerMetrics ofCircuitBreakerRegistry(MetricNames metricNames, CircuitBreakerRegistry registry) {
        return new TaggedCircuitBreakerMetrics(metricNames, registry.getAllCircuitBreakers());
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of circuit breakers.
     *
     * @param registry the source of circuit breakers
     */
    public static TaggedCircuitBreakerMetrics ofCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
        return ofCircuitBreakerRegistry(MetricNames.ofDefaults(), registry);
    }

    private final MetricNames names;
    private final Iterable<? extends CircuitBreaker> circuitBreakers;

    private TaggedCircuitBreakerMetrics(MetricNames names, Iterable<? extends CircuitBreaker> circuitBreakers) {
        this.names = requireNonNull(names);
        this.circuitBreakers = requireNonNull(circuitBreakers);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            Gauge.builder(names.getStateMetricName(), circuitBreaker, (cb) -> cb.getState().getOrder())
                    .tag(TagNames.NAME, circuitBreaker.getName())
                    .register(registry);

            Gauge.builder(names.getCallsMetricName(), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfFailedCalls())
                    .tag(TagNames.NAME, circuitBreaker.getName())
                    .tag(TagNames.KIND, "failed")
                    .register(registry);
            Gauge.builder(names.getCallsMetricName(), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfNotPermittedCalls())
                    .tag(TagNames.NAME, circuitBreaker.getName())
                    .tag(TagNames.KIND, "not_permitted")
                    .register(registry);
            Gauge.builder(names.getCallsMetricName(), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfSuccessfulCalls())
                    .tag(TagNames.NAME, circuitBreaker.getName())
                    .tag(TagNames.KIND, "successful")
                    .register(registry);

            Gauge.builder(names.getBufferedCallsMetricName(), circuitBreaker, (cb) -> cb.getMetrics().getNumberOfBufferedCalls())
                    .tag(TagNames.NAME, circuitBreaker.getName())
                    .register(registry);

            Gauge.builder(names.getMaxBufferedCallsMetricName(), circuitBreaker, (cb) -> cb.getMetrics().getMaxNumberOfBufferedCalls())
                    .tag(TagNames.NAME, circuitBreaker.getName())
                    .register(registry);
        }
    }

    /**
     * Registers timer per circuit breaker instance measuring elapsed duration per following event:
     * <ul>
     * <li>{@link CircuitBreakerOnSuccessEvent}</li>
     * <li>{@link CircuitBreakerOnErrorEvent}</li>
     * <li>{@link CircuitBreakerOnIgnoredErrorEvent}</li>
     * </ul>
     *
     * <p>Note that metrics recording is triggered on events publication
     * thus it should be used judiciously in performance sensitive environments.
     * Also the existing consumers will be replaced with the ones recording stats,
     * thus if it's necessary to have more consumers in place consider using dedicated methods
     * such as {@link #createOnSuccessElapsedDurationRecorder} and chaining those.
     */
    public void registerElapsedDurationRecorders(MeterRegistry registry) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            circuitBreaker.getEventPublisher()
                .onSuccess(createOnSuccessElapsedDurationRecorder(circuitBreaker, registry))
                .onError(createOnErrorElapsedDurationRecorder(circuitBreaker, registry))
                .onIgnoredError(createOnIgnoredErrorElapsedDurationRecorder(circuitBreaker, registry));
        }
    }

    /**
     * Creates a new consumer recording {@link CircuitBreakerOnSuccessEvent} elapsed durations.
     *
     * @param circuitBreaker tne circuit breaker to record stats for
     * @param registry the registry used to bind timer
     */
    public EventConsumer<CircuitBreakerOnSuccessEvent> createOnSuccessElapsedDurationRecorder(CircuitBreaker circuitBreaker, MeterRegistry registry) {
        return new OnSuccessElapsedDurationRecorder(
            names.getElapsedDurationMetricName(),
            circuitBreaker.getName(),
            registry
        );
    }

    /**
     * Creates a new consumer recording {@link CircuitBreakerOnErrorEvent} elapsed durations.
     *
     * @param circuitBreaker tne circuit breaker to record stats for
     * @param registry       the registry used to bind timer
     */
    public EventConsumer<CircuitBreakerOnErrorEvent> createOnErrorElapsedDurationRecorder(CircuitBreaker circuitBreaker, MeterRegistry registry) {
        return new OnErrorElapsedDurationRecorder(
            names.getElapsedDurationMetricName(),
            circuitBreaker.getName(),
            registry
        );
    }

    /**
     * Creates a new consumer recording {@link CircuitBreakerOnIgnoredErrorEvent} elapsed durations.
     *
     * @param circuitBreaker tne circuit breaker to record stats for
     * @param registry       the registry used to bind timer
     */
    public EventConsumer<CircuitBreakerOnIgnoredErrorEvent> createOnIgnoredErrorElapsedDurationRecorder(CircuitBreaker circuitBreaker, MeterRegistry registry) {
        return new OnErrorIgnoredElapsedDurationRecorder(
            names.getElapsedDurationMetricName(),
            circuitBreaker.getName(),
            registry
        );
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME = "resilience4j_circuitbreaker_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME = "resilience4j_circuitbreaker_state";
        public static final String DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS = "resilience4j_circuitbreaker_buffered_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS = "resilience4j_circuitbreaker_max_buffered_calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_ELAPSED_DURATION_METRIC_NAME = "resilience4j_circuitbreaker_elapsed_duration";

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
        private String elapsedDurationMetricName = DEFAULT_CIRCUIT_BREAKER_ELAPSED_DURATION_METRIC_NAME;

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

        /** Returns the metric name for elapsed duration, defaults to {@value DEFAULT_CIRCUIT_BREAKER_ELAPSED_DURATION_METRIC_NAME}. */
        public String getElapsedDurationMetricName() { return elapsedDurationMetricName; }

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

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_ELAPSED_DURATION_METRIC_NAME} with a given one. */
            public Builder elapsedDurationMetricName(String elapsedDurationMetricName) {
                metricNames.elapsedDurationMetricName = requireNonNull(elapsedDurationMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance. */
            public MetricNames build() {
                return metricNames;
            }
        }
    }

    private static class OnSuccessElapsedDurationRecorder extends ElapsedDurationRecorder<CircuitBreakerOnSuccessEvent> {
        OnSuccessElapsedDurationRecorder(String metricName, String cbName, MeterRegistry registry) {
            super("success", metricName, cbName, registry);
        }

        @Override
        Duration extractElapsedDuration(CircuitBreakerOnSuccessEvent event) {
            return event.getElapsedDuration();
        }
    }

    private static class OnErrorElapsedDurationRecorder extends ElapsedDurationRecorder<CircuitBreakerOnErrorEvent> {
        OnErrorElapsedDurationRecorder(String metricName, String cbName, MeterRegistry registry) {
            super("error", metricName, cbName, registry);
        }

        @Override
        Duration extractElapsedDuration(CircuitBreakerOnErrorEvent event) {
            return event.getElapsedDuration();
        }
    }

    private static class OnErrorIgnoredElapsedDurationRecorder extends ElapsedDurationRecorder<CircuitBreakerOnIgnoredErrorEvent> {
        OnErrorIgnoredElapsedDurationRecorder(String metricName, String cbName, MeterRegistry registry) {
            super("ignored_error", metricName, cbName, registry);
        }

        @Override
        Duration extractElapsedDuration(CircuitBreakerOnIgnoredErrorEvent event) {
            return event.getElapsedDuration();
        }
    }

    private static abstract class ElapsedDurationRecorder<T extends CircuitBreakerEvent> implements EventConsumer<T> {
        final Timer timer;

        ElapsedDurationRecorder(String kind, String metricName, String cbName, MeterRegistry registry) {
            this.timer = Timer.builder(metricName)
                .tag(TagNames.NAME, cbName)
                .tag(TagNames.KIND, kind)
                .register(registry);
        }

        abstract Duration extractElapsedDuration(T event);

        @Override
        public void consumeEvent(T event) {
            timer.record(extractElapsedDuration(event));
        }
    }
}
