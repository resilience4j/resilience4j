package io.github.resilience4j.micrometer.tagged;

import static java.util.Objects.requireNonNull;

public class RateLimiterMetricNames {

    private static final String DEFAULT_PREFIX = "resilience4j.ratelimiter";

    public static final String DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME =
        DEFAULT_PREFIX + ".available.permissions";
    public static final String DEFAULT_WAITING_THREADS_METRIC_NAME =
        DEFAULT_PREFIX + ".waiting_threads";
    private String availablePermissionsMetricName = DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
    private String waitingThreadsMetricName = DEFAULT_WAITING_THREADS_METRIC_NAME;

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
     * @return The default {@link RateLimiterMetricNames} instance.
     */
    public static RateLimiterMetricNames ofDefaults() {
        return new RateLimiterMetricNames();
    }

    /**
     * Returns the metric name for available permissions, defaults to {@value
     * DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME}.
     *
     * @return The available permissions metric name.
     */
    public String getAvailablePermissionsMetricName() {
        return availablePermissionsMetricName;
    }

    /**
     * Returns the metric name for waiting threads, defaults to {@value
     * DEFAULT_WAITING_THREADS_METRIC_NAME}.
     *
     * @return The waiting threads metric name.
     */
    public String getWaitingThreadsMetricName() {
        return waitingThreadsMetricName;
    }

    /**
     * Helps building custom instance of {@link RateLimiterMetricNames}.
     */
    public static class Builder {

        private final RateLimiterMetricNames metricNames = new RateLimiterMetricNames();

        /**
         * Overrides the default metric name {@value RateLimiterMetricNames#DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME}
         * with a given one.
         *
         * @param availablePermissionsMetricName The available permissions metric name.
         * @return The builder.
         */
        public Builder availablePermissionsMetricName(String availablePermissionsMetricName) {
            metricNames.availablePermissionsMetricName = requireNonNull(
                availablePermissionsMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value RateLimiterMetricNames#DEFAULT_WAITING_THREADS_METRIC_NAME}
         * with a given one.
         *
         * @param waitingThreadsMetricName The waiting threads metric name.
         * @return The builder.
         */
        public Builder waitingThreadsMetricName(String waitingThreadsMetricName) {
            metricNames.waitingThreadsMetricName = requireNonNull(waitingThreadsMetricName);
            return this;
        }

        /**
         * Builds {@link RateLimiterMetricNames} instance.
         *
         * @return The built {@link RateLimiterMetricNames} instance.
         */
        public RateLimiterMetricNames build() {
            return metricNames;
        }
    }
}
