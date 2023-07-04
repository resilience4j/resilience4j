/*
 * Copyright 2023 Mariusz Kopylec
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

import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;

import static java.util.Objects.requireNonNullElse;

public class MonitorMetricNames {

    private static final String DEFAULT_PREFIX = "resilience4j.executionmeter";
    public static final String DEFAULT_EXECUTION_METER_CALLS = DEFAULT_PREFIX + ".calls";

    @NonNull
    private final String callsMetricName;

    protected MonitorMetricNames(@Nullable String callsMetricName) {
        this.callsMetricName = requireNonNullElse(callsMetricName, DEFAULT_EXECUTION_METER_CALLS);
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
     * @return The default {@link MonitorMetricNames} instance.
     */
    public static MonitorMetricNames ofDefaults() {
        return new Builder().build();
    }

    /**
     * Returns the metric name for operation execution calls, defaults to {@value DEFAULT_EXECUTION_METER_CALLS}.
     *
     * @return The operation execution calls metric name.
     */
    public String getCallsMetricName() {
        return callsMetricName;
    }

    /**
     * Helps to build custom instance of {@link MonitorMetricNames}.
     */
    public static class Builder {

        @Nullable
        private String callsMetricName;

        /**
         * Overrides the default metric name {@value MonitorMetricNames#DEFAULT_EXECUTION_METER_CALLS}
         * with a given one.
         *
         * @param callsMetricName The calls metric name.
         * @return The builder.
         */
        public Builder callsMetricName(@Nullable String callsMetricName) {
            this.callsMetricName = callsMetricName;
            return this;
        }

        /**
         * Builds {@link MonitorMetricNames} instance.
         *
         * @return The built {@link MonitorMetricNames} instance.
         */
        public MonitorMetricNames build() {
            return new MonitorMetricNames(callsMetricName);
        }
    }
}
