package io.github.resilience4j.timelimiter.utils;

public final class MetricNames {

    private MetricNames() {
    }

    public static final String DEFAULT_PREFIX = "resilience4j.timelimiter";
    public static final String SUCCESSFUL = "successful";
    public static final String FAILED = "failed";
    public static final String TIMEOUT = "timeout";
    public static final String PREFIX_NULL = "Prefix must not be null";
    public static final String ITERABLE_NULL = "TimeLimiters iterable must not be null";

}
