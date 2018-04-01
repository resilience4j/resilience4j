package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.Bulkhead;

/**
 * TODO documentation
 */
public interface AdaptiveBulkheadMetrics extends Bulkhead.Metrics {
    int getConcurrencyLimit();

    long getMaxPermissionWaitTimeMillis();

    double getMaxLatencySeconds();

    long getAverageLatencyNanos();
}
