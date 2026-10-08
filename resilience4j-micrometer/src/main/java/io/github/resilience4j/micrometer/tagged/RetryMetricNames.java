package io.github.resilience4j.micrometer.tagged;

import static java.util.Objects.requireNonNull;

public class RetryMetricNames {

    public static final String DEFAULT_RETRY_CALLS = "resilience4j.retry.calls";
    public static final String DEFAULT_RETRY_BACKOFF = "resilience4j.retry.backoff";

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
     * @return The default {@link RetryMetricNames} instance.
     */
    public static RetryMetricNames ofDefaults() {
        return new RetryMetricNames();
    }

    private String callsMetricName = DEFAULT_RETRY_CALLS;
    private String backoffMetricName = DEFAULT_RETRY_BACKOFF;

    protected RetryMetricNames() {
    }

    /**
     * Returns the metric name for retry calls, defaults to {@value DEFAULT_RETRY_CALLS}.
     *
     * @return The metric name for retry calls.
     */
    public String getCallsMetricName() {
        return callsMetricName;
    }

    /**
     * Returns the metric name for the wait scheduled between retry attempts, defaults to
     * {@value DEFAULT_RETRY_BACKOFF}.
     *
     * @return The metric name for the wait scheduled between retry attempts.
     */
    public String getBackoffMetricName() {
        return backoffMetricName;
    }

    /**
     * Helps building custom instance of {@link RetryMetricNames}.
     */
    public static class Builder {

        private final RetryMetricNames retryMetricNames = new RetryMetricNames();

        /**
         * Overrides the default metric name {@value RetryMetricNames#DEFAULT_RETRY_CALLS} with a
         * given one.
         *
         * @param callsMetricName The metric name for retry calls.
         * @return The builder.
         */
        public Builder callsMetricName(String callsMetricName) {
            retryMetricNames.callsMetricName = requireNonNull(callsMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value RetryMetricNames#DEFAULT_RETRY_BACKOFF} with
         * a given one.
         *
         * @param backoffMetricName The metric name for the wait scheduled between retry attempts.
         * @return The builder.
         */
        public Builder backoffMetricName(String backoffMetricName) {
            retryMetricNames.backoffMetricName = requireNonNull(backoffMetricName);
            return this;
        }

        /**
         * Builds {@link RetryMetricNames} instance.
         *
         * @return The built {@link RetryMetricNames} instance.
         */
        public RetryMetricNames build() {
            return retryMetricNames;
        }
    }
}
