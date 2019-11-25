package io.github.resilience4j.retry.utils;

public class MetricNames {

    public static final String DEFAULT_PREFIX = "resilience4j.retry";
    public static final String DEFAULT_PREFIX_ASYNC = "resilience4j.asyncRetry";
    public static final String SUCCESSFUL_CALLS_WITHOUT_RETRY = "successful_calls_without_retry";
    public static final String SUCCESSFUL_CALLS_WITH_RETRY = "successful_calls_with_retry";
    public static final String FAILED_CALLS_WITHOUT_RETRY = "failed_calls_without_retry";
    public static final String FAILED_CALLS_WITH_RETRY = "failed_calls_with_retry";
}
