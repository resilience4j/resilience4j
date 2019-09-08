package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;

import static java.util.Objects.requireNonNull;

public class TaggedTimeLimiterMetricsPublisher
        extends AbstractTimeLimiterMetrics implements MetricsPublisher<TimeLimiter> {

    private final MeterRegistry meterRegistry;

    public TaggedTimeLimiterMetricsPublisher(MeterRegistry meterRegistry) {
        super(MetricNames.ofDefaults());
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    public TaggedTimeLimiterMetricsPublisher(MetricNames names, MeterRegistry meterRegistry) {
        super(names);
        this.meterRegistry = requireNonNull(meterRegistry);
    }
    @Override
    public void publishMetrics(TimeLimiter entry) {
        addMetrics(meterRegistry, entry);
    }

    @Override
    public void removeMetrics(TimeLimiter entry) {
        removeMetrics(meterRegistry, entry.getName());
    }
}
