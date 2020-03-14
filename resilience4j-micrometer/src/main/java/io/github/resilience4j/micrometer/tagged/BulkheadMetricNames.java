package io.github.resilience4j.micrometer.tagged;

import static java.util.Objects.requireNonNull;

/**
 * Defines possible configuration for metric names.
 */
public class BulkheadMetricNames {
    private static final String DEFAULT_PREFIX = "resilience4j.bulkhead";

    public static final String DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME =
        DEFAULT_PREFIX + ".available.concurrent.calls";
    public static final String DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME =
        DEFAULT_PREFIX + ".max.allowed.concurrent.calls";
    private String availableConcurrentCallsMetricName = DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME;
    private String maxAllowedConcurrentCallsMetricName = DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME;

    protected BulkheadMetricNames() {
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
     * @return The default {@link BulkheadMetricNames} instance.
     */
    public static BulkheadMetricNames ofDefaults() {
        return new BulkheadMetricNames();
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
     * Helps building custom instance of {@link BulkheadMetricNames}.
     */
    public static class Builder {

        private final BulkheadMetricNames metricNames = new BulkheadMetricNames();

        /**
         * Overrides the default metric name {@value BulkheadMetricNames#DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME}
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
         * Overrides the default metric name {@value BulkheadMetricNames#DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME}
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
         * Builds {@link BulkheadMetricNames} instance.
         *
         * @return The built {@link BulkheadMetricNames} instance.
         */
        public BulkheadMetricNames build() {
            return metricNames;
        }
    }
}
