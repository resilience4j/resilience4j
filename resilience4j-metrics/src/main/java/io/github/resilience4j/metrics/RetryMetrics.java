package io.github.resilience4j.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.List;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.retry.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link Retry.Metrics} as Dropwizard Metrics Gauges.
 */
public class RetryMetrics implements MetricSet {

    private final MetricRegistry metricRegistry;

    private RetryMetrics(Iterable<Retry> retries) {
        this(DEFAULT_PREFIX, retries, new MetricRegistry());
    }

    private RetryMetrics(String prefix, Iterable<Retry> retries, MetricRegistry metricRegistry) {
        requireNonNull(prefix);
        requireNonNull(retries);
        requireNonNull(metricRegistry);
        this.metricRegistry = metricRegistry;
        retries.forEach(retry -> {
            String name = retry.getName();
            metricRegistry.register(name(prefix, name, SUCCESSFUL_CALLS_WITHOUT_RETRY),
                (Gauge<Long>) () -> retry.getMetrics()
                    .getNumberOfSuccessfulCallsWithoutRetryAttempt());
            metricRegistry.register(name(prefix, name, SUCCESSFUL_CALLS_WITH_RETRY),
                (Gauge<Long>) () -> retry.getMetrics()
                    .getNumberOfSuccessfulCallsWithRetryAttempt());
            metricRegistry.register(name(prefix, name, FAILED_CALLS_WITHOUT_RETRY),
                (Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            metricRegistry.register(name(prefix, name, FAILED_CALLS_WITH_RETRY),
                (Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
        });
    }

    public static RetryMetrics ofRetryRegistry(String prefix, RetryRegistry retryRegistry,
        MetricRegistry metricRegistry) {
        return new RetryMetrics(prefix, retryRegistry.getAllRetries(), metricRegistry);
    }

    public static RetryMetrics ofRetryRegistry(String prefix, RetryRegistry retryRegistry) {
        return new RetryMetrics(prefix, retryRegistry.getAllRetries(), new MetricRegistry());
    }

    public static RetryMetrics ofRetryRegistry(RetryRegistry retryRegistry,
        MetricRegistry metricRegistry) {
        return new RetryMetrics(DEFAULT_PREFIX, retryRegistry.getAllRetries(), metricRegistry);
    }

    public static RetryMetrics ofRetryRegistry(RetryRegistry retryRegistry) {
        return new RetryMetrics(retryRegistry.getAllRetries());
    }

    public static RetryMetrics ofIterable(String prefix, Iterable<Retry> retries) {
        return new RetryMetrics(prefix, retries, new MetricRegistry());
    }

    public static RetryMetrics ofIterable(Iterable<Retry> retries) {
        return new RetryMetrics(retries);
    }

    public static RetryMetrics ofRetry(Retry retry) {
        return new RetryMetrics(List.of(retry));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
