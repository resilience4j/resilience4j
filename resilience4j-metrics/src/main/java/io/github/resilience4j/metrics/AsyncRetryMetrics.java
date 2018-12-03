package io.github.resilience4j.metrics;

import com.codahale.metrics.*;
import io.github.resilience4j.retry.AsyncRetry;
import io.vavr.collection.Array;

import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.retry.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link AsyncRetry.Metrics} as Dropwizard Metrics Gauges.
 */
public class AsyncRetryMetrics implements MetricSet {

    private final MetricRegistry metricRegistry = new MetricRegistry();

    private AsyncRetryMetrics(Iterable<AsyncRetry> retries){
        this(DEFAULT_PREFIX, retries);
    }

    private AsyncRetryMetrics(String prefix, Iterable<AsyncRetry> retries){
        requireNonNull(prefix);
        requireNonNull(retries);
        retries.forEach(retry -> {
            String name = retry.getName();

            metricRegistry.register(name(prefix, name, SUCCESSFUL_CALLS_WITHOUT_RETRY),
                    (Gauge<Long>) () -> retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            metricRegistry.register(name(prefix, name, SUCCESSFUL_CALLS_WITH_RETRY),
                    (Gauge<Long>) () -> retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            metricRegistry.register(name(prefix, name, FAILED_CALLS_WITHOUT_RETRY),
                    (Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            metricRegistry.register(name(prefix, name, FAILED_CALLS_WITH_RETRY),
                    (Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
        });
    }

    public static AsyncRetryMetrics ofIterable(String prefix, Iterable<AsyncRetry> retries) {
        return new AsyncRetryMetrics(prefix, retries);
    }

    public static AsyncRetryMetrics ofIterable(Iterable<AsyncRetry> retries) {
        return new AsyncRetryMetrics(retries);
    }

    public static AsyncRetryMetrics ofRateLimiter(AsyncRetry retry) {
        return new AsyncRetryMetrics(Array.of(retry));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
