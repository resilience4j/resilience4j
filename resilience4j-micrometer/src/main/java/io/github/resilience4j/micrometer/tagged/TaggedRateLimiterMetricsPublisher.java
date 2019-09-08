package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;

import static java.util.Objects.requireNonNull;

public class TaggedRateLimiterMetricsPublisher
        extends AbstractRateLimiterMetrics implements MetricsPublisher<RateLimiter> {

    private final MeterRegistry meterRegistry;

    public TaggedRateLimiterMetricsPublisher(MeterRegistry meterRegistry) {
        super(MetricNames.ofDefaults());
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    public TaggedRateLimiterMetricsPublisher(MetricNames names, MeterRegistry meterRegistry) {
        super(names);
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    @Override
    public void publishMetrics(RateLimiter entry) {
        addMetrics(meterRegistry, entry);
    }

    @Override
    public void removeMetrics(RateLimiter entry) {
        removeMetrics(meterRegistry, entry.getName());
    }
}
