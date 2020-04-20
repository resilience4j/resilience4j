package io.github.resilience4j.micrometer.tagged;

import static java.util.Objects.requireNonNull;

public class TimeLimiterMetricNames {

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
     * @return The default {@link TimeLimiterMetricNames} instance.
     */
    public static TimeLimiterMetricNames ofDefaults() {
        return new TimeLimiterMetricNames();
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
     * Helps building custom instance of {@link TimeLimiterMetricNames}.
     */
    public static class Builder {

        private final TimeLimiterMetricNames metricNames = new TimeLimiterMetricNames();

        /**
         * Overrides the default metric name {@value TimeLimiterMetricNames#DEFAULT_TIME_LIMITER_CALLS}
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
         * Builds {@link TimeLimiterMetricNames} instance.
         *
         * @return The built {@link TimeLimiterMetricNames} instance.
         */
        public TimeLimiterMetricNames build() {
            return metricNames;
        }
    }
}
