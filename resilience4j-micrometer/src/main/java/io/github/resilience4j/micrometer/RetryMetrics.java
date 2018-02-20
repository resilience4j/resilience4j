package io.github.resilience4j.micrometer;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static io.github.resilience4j.micrometer.MetricUtils.getName;
import static io.github.resilience4j.retry.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class RetryMetrics implements MeterBinder {

    private final Iterable<Retry> retries;
    private final String prefix;

    private RetryMetrics(Iterable<Retry> retries) {
        this(retries, DEFAULT_PREFIX);
    }

    private RetryMetrics(Iterable<Retry> retries, String prefix) {
        this.retries = requireNonNull(retries);
        this.prefix = requireNonNull(prefix);
    }

    /**
     * Creates a new instance RetryMetrics {@link RetryMetrics} with
     * a {@link RateLimiterRegistry} as a source.
     *
     * @param retryRegistry the registry of retries
     */
    public static RetryMetrics ofRetryRegistry(RetryRegistry retryRegistry) {
        return new RetryMetrics(retryRegistry.getAllRetries());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Retry retry : retries) {
            final String name = retry.getName();
            Gauge.builder(getName(prefix, name, SUCCESSFUL_CALLS_WITHOUT_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
                    .register(registry);
            Gauge.builder(getName(prefix, name, SUCCESSFUL_CALLS_WITH_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt())
                    .register(registry);
            Gauge.builder(getName(prefix, name, FAILED_CALLS_WITHOUT_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
                    .register(registry);
            Gauge.builder(getName(prefix, name, FAILED_CALLS_WITH_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
                    .register(registry);
        }
    }
}
