package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.Retry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.retry.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class RetryMetricsPublisher extends AbstractMetricsPublisher<Retry> {

    private final String prefix;

    public RetryMetricsPublisher() {
        this(DEFAULT_PREFIX, new MetricRegistry());
    }

    public RetryMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX, metricRegistry);
    }

    public RetryMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(Retry entry) {
        String name = entry.getName();

        String successfulWithoutRetry = name(prefix, name, SUCCESSFUL_CALLS_WITHOUT_RETRY);
        String successfulWithRetry = name(prefix, name, SUCCESSFUL_CALLS_WITH_RETRY);
        String failedWithoutRetry = name(prefix, name, FAILED_CALLS_WITHOUT_RETRY);
        String failedWithRetry = name(prefix, name, FAILED_CALLS_WITH_RETRY);

        metricRegistry.register(successfulWithoutRetry,
                (Gauge<Long>) () -> entry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
        metricRegistry.register(successfulWithRetry,
                (Gauge<Long>) () -> entry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
        metricRegistry.register(failedWithoutRetry,
                (Gauge<Long>) () -> entry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
        metricRegistry.register(failedWithRetry,
                (Gauge<Long>) () -> entry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());

        List<String> metricNames = Arrays.asList(successfulWithoutRetry, successfulWithRetry, failedWithoutRetry, failedWithRetry);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(Retry entry) {
        removeMetrics(entry.getName());
    }
}
