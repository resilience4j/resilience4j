package io.github.resilience4j.bulkhead.utils;

public class MetricNames {
    private MetricNames() {
    }
    public static final String DEFAULT_PREFIX = "resilience4j.bulkhead";
    public static final String AVAILABLE_CONCURRENT_CALLS = "available_concurrent_calls";
    public static final String MAX_ALLOWED_CONCURRENT_CALLS = "max_allowed_concurrent_calls";
}
