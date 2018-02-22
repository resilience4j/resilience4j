package io.github.resilience4j.micrometer;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static io.github.resilience4j.micrometer.MetricUtils.getName;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.AVAILABLE_PERMISSIONS;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.WAITING_THREADS;
import static java.util.Objects.requireNonNull;

public class RateLimiterMetrics implements MeterBinder {

    private final Iterable<RateLimiter> rateLimiters;
    private final String prefix;

    private RateLimiterMetrics(Iterable<RateLimiter> rateLimiters) {
        this(rateLimiters, DEFAULT_PREFIX);
    }

    private RateLimiterMetrics(Iterable<RateLimiter> rateLimiters, String prefix) {
        this.rateLimiters = requireNonNull(rateLimiters);
        this.prefix = requireNonNull(prefix);
    }

    /**
     * Creates a new instance RateLimiterMetrics {@link RateLimiterMetrics} with
     * a {@link RateLimiterRegistry} as a source.
     *
     * @param rateLimiterRegistry the registry of rate limiters
     */
    public static RateLimiterMetrics ofRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterMetrics(rateLimiterRegistry.getAllRateLimiters());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (RateLimiter rateLimiter : rateLimiters) {
            final String name = rateLimiter.getName();
            Gauge.builder(getName(prefix, name, AVAILABLE_PERMISSIONS), rateLimiter, (cb) -> cb.getMetrics().getAvailablePermissions())
                    .register(registry);
            Gauge.builder(getName(prefix, name, WAITING_THREADS), rateLimiter, (cb) -> cb.getMetrics().getNumberOfWaitingThreads())
                    .register(registry);
        }
    }
}
