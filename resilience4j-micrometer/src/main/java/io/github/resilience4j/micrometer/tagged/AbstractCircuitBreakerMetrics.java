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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

abstract class AbstractCircuitBreakerMetrics extends AbstractMetrics {

    private static final String KIND_STATE = "state";
    private static final String KIND_FAILED = "failed";
    private static final String KIND_SUCCESSFUL = "successful";
    private static final String KIND_IGNORED = "ignored";
    private static final String KIND_NOT_PERMITTED = "not_permitted";

    protected final MetricNames names;

    protected AbstractCircuitBreakerMetrics(MetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, CircuitBreaker circuitBreaker) {
        Set<Meter.Id> idSet = new HashSet<>();
        List<Tag> customTags = circuitBreaker.getTags()
                .toJavaMap()
                .entrySet()
                .stream().map(tagsEntry -> Tag.of(tagsEntry.getKey(), tagsEntry.getValue()))
                .collect(Collectors.toList());

        final CircuitBreaker.State[] states = CircuitBreaker.State.values();
        for (CircuitBreaker.State state : states) {
            idSet.add(Gauge.builder(names.getStateMetricName(), circuitBreaker, cb -> cb.getState() == state ? 1 : 0)
                    .description("The states of the circuit breaker")
                    .tag(TagNames.NAME, circuitBreaker.getName())
                    .tag(KIND_STATE, state.name().toLowerCase())
                    .tags(customTags)
                    .register(meterRegistry).getId());
        }
        idSet.add(Gauge.builder(names.getBufferedCallsMetricName(), circuitBreaker, cb -> cb.getMetrics().getNumberOfFailedCalls())
                .description("The number of buffered failed calls stored in the ring buffer")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_FAILED)
                .tags(customTags)
                .register(meterRegistry).getId());
        idSet.add(Gauge.builder(names.getBufferedCallsMetricName(), circuitBreaker, cb -> cb.getMetrics().getNumberOfSuccessfulCalls())
                .description("The number of buffered successful calls stored in the ring buffer")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_SUCCESSFUL)
                .tags(customTags)
                .register(meterRegistry).getId());
        idSet.add(Gauge.builder(names.getSlowCallsMetricName(), circuitBreaker, cb -> cb.getMetrics().getNumberOfSlowSuccessfulCalls())
                .description("The number of slow successful which were slower than a certain threshold")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_SUCCESSFUL)
                .tags(customTags)
                .register(meterRegistry).getId());
        idSet.add(Gauge.builder(names.getSlowCallsMetricName(), circuitBreaker, cb -> cb.getMetrics().getNumberOfSlowFailedCalls())
                .description("The number of slow failed calls which were slower than a certain threshold")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_FAILED)
                .tags(customTags)
                .register(meterRegistry).getId());
        idSet.add(Gauge.builder(names.getFailureRateMetricName(), circuitBreaker, cb -> cb.getMetrics().getFailureRate())
                .description("The failure rate of the circuit breaker")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tags(customTags)
                .register(meterRegistry).getId());

        idSet.add(Gauge.builder(names.getSlowCallRateMetricName(), circuitBreaker, cb -> cb.getMetrics().getSlowCallRate())
                .description("The slow call of the circuit breaker")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tags(customTags)
                .register(meterRegistry).getId());

        Timer successfulCalls = Timer.builder(names.getCallsMetricName())
                .description("Total number of successful calls")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_SUCCESSFUL)
                .tags(customTags)
                .register(meterRegistry);

        Timer failedCalls = Timer.builder(names.getCallsMetricName())
                .description("Total number of failed calls")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_FAILED)
                .tags(customTags)
                .register(meterRegistry);

        Timer ignoredFailedCalls = Timer.builder(names.getCallsMetricName())
                .description("Total number of calls which failed but the exception was ignored")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_IGNORED)
                .tags(customTags)
                .register(meterRegistry);

        Counter notPermittedCalls = Counter.builder(names.getCallsMetricName())
                .description("Total number of not permitted calls")
                .tag(TagNames.NAME, circuitBreaker.getName())
                .tag(TagNames.KIND, KIND_NOT_PERMITTED)
                .tags(customTags)
                .register(meterRegistry);

        idSet.add(successfulCalls.getId());
        idSet.add(failedCalls.getId());
        idSet.add(ignoredFailedCalls.getId());
        idSet.add(notPermittedCalls.getId());

        circuitBreaker.getEventPublisher()
                .onIgnoredError(event -> ignoredFailedCalls.record(event.getElapsedDuration()))
                .onCallNotPermitted(event -> notPermittedCalls.increment())
                .onSuccess(event -> successfulCalls.record(event.getElapsedDuration()))
                .onError(event -> failedCalls.record(event.getElapsedDuration()));

        meterIdMap.put(circuitBreaker.getName(), idSet);
    }

    public static class MetricNames {

        private static final String DEFAULT_PREFIX = "resilience4j.circuitbreaker";

        public static final String DEFAULT_CIRCUIT_BREAKER_CALLS = DEFAULT_PREFIX + ".calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_STATE = DEFAULT_PREFIX + ".state";
        public static final String DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS = DEFAULT_PREFIX + ".buffered.calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS = DEFAULT_PREFIX + ".slow.calls";
        public static final String DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE = DEFAULT_PREFIX + ".failure.rate";
        public static final String DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE = DEFAULT_PREFIX + ".slow.call.rate";

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
         *
         * @return The builder.
         */
        public static Builder custom() {
            return new Builder();
        }

        /**
         * Returns default metric names.
         *
         * @return The default {@link MetricNames} instance.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        private String callsMetricName = DEFAULT_CIRCUIT_BREAKER_CALLS;
        private String stateMetricName = DEFAULT_CIRCUIT_BREAKER_STATE;
        private String bufferedCallsMetricName = DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS;
        private String slowCallsMetricName = DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS;
        private String failureRateMetricName = DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE;
        private String slowCallRateMetricName = DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE;

        private MetricNames() {
        }

        /**
         * Returns the metric name for circuit breaker calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_CALLS}.
         *
         * @return The circuit breaker calls metric name.
         */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /**
         * Returns the metric name for currently buffered calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS}.
         *
         * @return The buffered calls metric name.
         */
        public String getBufferedCallsMetricName() {
            return bufferedCallsMetricName;
        }

        /**
         * Returns the metric name for currently slow calls, defaults to {@value DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS}.
         *
         * @return The slow calls metric name.
         */
        public String getSlowCallsMetricName() {
            return slowCallsMetricName;
        }

        /**
         * Returns the metric name for state, defaults to {@value DEFAULT_CIRCUIT_BREAKER_STATE}.
         *
         * @return The state metric name.
         */
        public String getStateMetricName() {
            return stateMetricName;
        }

        /**
         * Returns the metric name for failure rate, defaults to {@value DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE}.
         *
         * @return The failure rate metric name.
         */
        public String getFailureRateMetricName() {
            return failureRateMetricName;
        }

        /**
         * Returns the metric name for slow call rate, defaults to {@value DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE}.
         *
         * @return The failure rate metric name.
         */
        public String getSlowCallRateMetricName() {
            return slowCallRateMetricName;
        }

        /**
         * Helps building custom instance of {@link MetricNames}.
         */
        public static class Builder {
            private final MetricNames metricNames = new MetricNames();

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_CALLS} with a given one.
             *
             * @param callsMetricName The calls metric name.
             * @return The builder.
             */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_STATE} with a given one.
             *
             * @param stateMetricName The state metric name.
             * @return The builder.
             */
            public Builder stateMetricName(String stateMetricName) {
                metricNames.stateMetricName = requireNonNull(stateMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS} with a given one.
             *
             * @param bufferedCallsMetricName The bufferd calls metric name.
             * @return The builder.
             */
            public Builder bufferedCallsMetricName(String bufferedCallsMetricName) {
                metricNames.bufferedCallsMetricName = requireNonNull(bufferedCallsMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS} with a given one.
             *
             * @param slowCallsMetricName The slow calls metric name.
             * @return The builder.
             */
            public Builder slowCallsMetricName(String slowCallsMetricName) {
                metricNames.slowCallsMetricName = requireNonNull(slowCallsMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE} with a given one.
             *
             * @param failureRateMetricName The failure rate metric name.
             * @return The builder.
             */
            public Builder failureRateMetricName(String failureRateMetricName) {
                metricNames.failureRateMetricName = requireNonNull(failureRateMetricName);
                return this;
            }

            /**
             * Overrides the default metric name {@value MetricNames#DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE} with a given one.
             *
             * @param slowCallRateMetricName The slow call rate metric name.
             * @return The builder.
             */
            public Builder slowCallRateMetricName(String slowCallRateMetricName) {
                metricNames.slowCallRateMetricName = requireNonNull(slowCallRateMetricName);
                return this;
            }

            /**
             * Builds {@link MetricNames} instance.
             *
             * @return The built {@link MetricNames} instance.
             */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
