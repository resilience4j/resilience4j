package io.github.resilience4j.circuitbreaker.utils;

public class MetricNames {
    public static final String DEFAULT_PREFIX = "resilience4j.circuitbreaker";
    public static final String SUCCESSFUL = "successful";
    public static final String FAILED = "failed";
    public static final String SLOW = "slow";
    public static final String SLOW_SUCCESS = "slow_successful";
    public static final String SLOW_FAILED = "slow_failed";
    public static final String NOT_PERMITTED = "not_permitted";
    public static final String BUFFERED = "buffered";
    public static final String STATE = "state";
    public static final String FAILURE_RATE = "failure_rate";
    public static final String SLOW_CALL_RATE = "slow_call_rate";
}
