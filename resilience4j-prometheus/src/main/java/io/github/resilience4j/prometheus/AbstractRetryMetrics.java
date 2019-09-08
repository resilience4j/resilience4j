package io.github.resilience4j.prometheus;

import io.prometheus.client.Collector;

import static java.util.Objects.requireNonNull;

public abstract class AbstractRetryMetrics extends Collector {

    protected final MetricNames names;

    protected AbstractRetryMetrics(MetricNames names) {
        this.names = requireNonNull(names);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        public static final String DEFAULT_RETRY_CALLS = "resilience4j_retry_calls";

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
         * @return The builder.
         */
        public static Builder custom() {
            return new Builder();
        }

        /** Returns default metric names.
         * @return The default {@link MetricNames} instance.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        private String callsMetricName = DEFAULT_RETRY_CALLS;

        private MetricNames() {}

        /** Returns the metric name for retry calls, defaults to {@value DEFAULT_RETRY_CALLS}.
         * @return The metric name for retry calls.
         */
        public String getCallsMetricName() {
            return callsMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {
            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_RETRY_CALLS} with a given one.
             * @param callsMetricName The metric name for retry calls.
             * @return The builder.
             */
            public Builder callsMetricName(String callsMetricName) {
                metricNames.callsMetricName = requireNonNull(callsMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance.
             * @return The built {@link MetricNames} instance.
             */
            public MetricNames build() {
                return metricNames;
            }
        }
    }

}
