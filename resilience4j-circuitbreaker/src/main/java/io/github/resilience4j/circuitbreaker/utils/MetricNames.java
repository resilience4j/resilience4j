package io.github.resilience4j.circuitbreaker.utils;

public class MetricNames {

    private MetricNames() {
    }

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

    /**
     * Suffix for the cumulative count of public permission requests.
     */
    public static final String ADMISSION_ATTEMPTS = "admission.attempts";

    /**
     * Suffix for the cumulative count of requests granted permission.
     */
    public static final String ADMISSION_PERMITTED_ATTEMPTS = "admission.permitted.attempts";

    /**
     * Suffix for the current phased HALF_OPEN recovery phase gauge.
     */
    public static final String RECOVERY_PHASE = "recovery.phase";

    /**
     * Suffix for the current recovery admission target percentage gauge.
     */
    public static final String RECOVERY_TRAFFIC_PERCENTAGE = "recovery.traffic.percentage";
}
