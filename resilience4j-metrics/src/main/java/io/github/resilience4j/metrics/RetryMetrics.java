package io.github.resilience4j.metrics;

import com.codahale.metrics.*;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.collection.Array;

import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.retry.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link Retry.Metrics} as Dropwizard Metrics Gauges.
 */
public class RetryMetrics implements MetricSet {

    private final MetricRegistry metricRegistry = new MetricRegistry();

    private RetryMetrics(Iterable<Retry> retries){
        this(DEFAULT_PREFIX, retries);
    }

    private RetryMetrics(String prefix, Iterable<Retry> retries){
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

    public static RetryMetrics ofRetryRegistry(String prefix, RetryRegistry retryRegistry) {
        return new RetryMetrics(prefix, retryRegistry.getAllRetries());
    }

    public static RetryMetrics ofRetryRegistry(RetryRegistry retryRegistry) {
        return new RetryMetrics(retryRegistry.getAllRetries());
    }

    public static RetryMetrics ofIterable(String prefix, Iterable<Retry> retries) {
        return new RetryMetrics(prefix, retries);
    }

    public static RetryMetrics ofIterable(Iterable<Retry> retries) {
        return new RetryMetrics(retries);
    }

    public static RetryMetrics ofRetry(Retry retry) {
        return new RetryMetrics(Array.of(retry));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
