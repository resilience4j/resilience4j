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

package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Objects.requireNonNull;

abstract class AbstractTimeLimiterMetrics extends AbstractMetrics {

    private static final String KIND_FAILED = "failed";
    private static final String KIND_SUCCESSFUL = "successful";
    private static final String KIND_TIMEOUT = "timeout";

    protected final MetricNames names;

    protected AbstractTimeLimiterMetrics(MetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, TimeLimiter timeLimiter) {
        // Remove previous meters before register
        removeMetrics(meterRegistry, timeLimiter.getName());

        Counter successes = Counter.builder(names.getCallsMetricName())
            .description("The number of successful calls")
            .tag(TagNames.NAME, timeLimiter.getName())
            .tag(TagNames.KIND, KIND_SUCCESSFUL)
            .register(meterRegistry);
        Counter failures = Counter.builder(names.getCallsMetricName())
            .description("The number of failed calls")
            .tag(TagNames.NAME, timeLimiter.getName())
            .tag(TagNames.KIND, KIND_FAILED)
            .register(meterRegistry);
        Counter timeouts = Counter.builder(names.getCallsMetricName())
            .description("The number of timed out calls")
            .tag(TagNames.NAME, timeLimiter.getName())
            .tag(TagNames.KIND, KIND_TIMEOUT)
            .register(meterRegistry);

        timeLimiter.getEventPublisher()
            .onSuccess(event -> successes.increment())
            .onError(event -> failures.increment())
            .onTimeout(event -> timeouts.increment());

        List<Meter.Id> ids = Arrays.asList(successes.getId(), failures.getId(), timeouts.getId());
        meterIdMap.put(timeLimiter.getName(), new HashSet<>(ids));
    }

    public static class MetricNames {

        private static final String DEFAULT_PREFIX = "resilience4j.timelimiter";
        public static final String DEFAULT_TIME_LIMITER_CALLS = DEFAULT_PREFIX + ".calls";

        private String callsMetricName = DEFAULT_TIME_LIMITER_CALLS;

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
         * @return The default {@link MetricNames} instance.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        /**
         * Returns the metric name for circuit breaker calls, defaults to {@value
         * DEFAULT_TIME_LIMITER_CALLS}.
         *
         * @return The circuit breaker calls metric name.
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
             * Overrides the default metric name {@value MetricNames#DEFAULT_TIME_LIMITER_CALLS}
             * with a given one.
             *
             * @param callsMetricName The calls metric name.
             * @return The builder.
             */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
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
