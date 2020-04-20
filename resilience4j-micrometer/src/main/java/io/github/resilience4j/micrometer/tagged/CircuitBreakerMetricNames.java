package io.github.resilience4j.micrometer.tagged;

import static java.util.Objects.requireNonNull;

public class CircuitBreakerMetricNames {

    private static final String DEFAULT_PREFIX = "resilience4j.circuitbreaker";

    public static final String DEFAULT_CIRCUIT_BREAKER_CALLS = DEFAULT_PREFIX + ".calls";
    public static final String DEFAULT_CIRCUIT_BREAKER_STATE = DEFAULT_PREFIX + ".state";
    public static final String DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS =
        DEFAULT_PREFIX + ".buffered.calls";
    public static final String DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS =
        DEFAULT_PREFIX + ".slow.calls";
    public static final String DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE =
        DEFAULT_PREFIX + ".failure.rate";
    public static final String DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE =
        DEFAULT_PREFIX + ".slow.call.rate";
    private String callsMetricName = DEFAULT_CIRCUIT_BREAKER_CALLS;
    private String stateMetricName = DEFAULT_CIRCUIT_BREAKER_STATE;
    private String bufferedCallsMetricName = DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS;
    private String slowCallsMetricName = DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS;
    private String failureRateMetricName = DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE;
    private String slowCallRateMetricName = DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE;

    protected CircuitBreakerMetricNames() {
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
     * @return The default {@link CircuitBreakerMetricNames} instance.
     */
    public static CircuitBreakerMetricNames ofDefaults() {
        return new CircuitBreakerMetricNames();
    }

    /**
     * Returns the metric name for circuit breaker calls, defaults to {@value
     * DEFAULT_CIRCUIT_BREAKER_CALLS}.
     *
     * @return The circuit breaker calls metric name.
     */
    public String getCallsMetricName() {
        return callsMetricName;
    }

    /**
     * Returns the metric name for currently buffered calls, defaults to {@value
     * DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS}.
     *
     * @return The buffered calls metric name.
     */
    public String getBufferedCallsMetricName() {
        return bufferedCallsMetricName;
    }

    /**
     * Returns the metric name for currently slow calls, defaults to {@value
     * DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS}.
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
     * Returns the metric name for failure rate, defaults to {@value
     * DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE}.
     *
     * @return The failure rate metric name.
     */
    public String getFailureRateMetricName() {
        return failureRateMetricName;
    }

    /**
     * Returns the metric name for slow call rate, defaults to {@value
     * DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE}.
     *
     * @return The failure rate metric name.
     */
    public String getSlowCallRateMetricName() {
        return slowCallRateMetricName;
    }

    /**
     * Helps building custom instance of {@link CircuitBreakerMetricNames}.
     */
    public static class Builder {

        private final CircuitBreakerMetricNames metricNames = new CircuitBreakerMetricNames();

        /**
         * Overrides the default metric name {@value CircuitBreakerMetricNames#DEFAULT_CIRCUIT_BREAKER_CALLS}
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
         * Overrides the default metric name {@value CircuitBreakerMetricNames#DEFAULT_CIRCUIT_BREAKER_STATE}
         * with a given one.
         *
         * @param stateMetricName The state metric name.
         * @return The builder.
         */
        public Builder stateMetricName(String stateMetricName) {
            metricNames.stateMetricName = requireNonNull(stateMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value CircuitBreakerMetricNames#DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS}
         * with a given one.
         *
         * @param bufferedCallsMetricName The bufferd calls metric name.
         * @return The builder.
         */
        public Builder bufferedCallsMetricName(String bufferedCallsMetricName) {
            metricNames.bufferedCallsMetricName = requireNonNull(bufferedCallsMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value CircuitBreakerMetricNames#DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS}
         * with a given one.
         *
         * @param slowCallsMetricName The slow calls metric name.
         * @return The builder.
         */
        public Builder slowCallsMetricName(String slowCallsMetricName) {
            metricNames.slowCallsMetricName = requireNonNull(slowCallsMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value CircuitBreakerMetricNames#DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE}
         * with a given one.
         *
         * @param failureRateMetricName The failure rate metric name.
         * @return The builder.
         */
        public Builder failureRateMetricName(String failureRateMetricName) {
            metricNames.failureRateMetricName = requireNonNull(failureRateMetricName);
            return this;
        }

        /**
         * Overrides the default metric name {@value CircuitBreakerMetricNames#DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE}
         * with a given one.
         *
         * @param slowCallRateMetricName The slow call rate metric name.
         * @return The builder.
         */
        public Builder slowCallRateMetricName(String slowCallRateMetricName) {
            metricNames.slowCallRateMetricName = requireNonNull(slowCallRateMetricName);
            return this;
        }

        /**
         * Builds {@link CircuitBreakerMetricNames} instance.
         *
         * @return The built {@link CircuitBreakerMetricNames} instance.
         */
        public CircuitBreakerMetricNames build() {
            return metricNames;
        }
    }
}
