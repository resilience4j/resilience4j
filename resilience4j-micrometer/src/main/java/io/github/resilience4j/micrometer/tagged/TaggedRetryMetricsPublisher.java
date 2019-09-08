package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.MeterRegistry;

import static java.util.Objects.requireNonNull;

public class TaggedRetryMetricsPublisher
        extends AbstractRetryMetrics implements MetricsPublisher<Retry> {

    private final MeterRegistry meterRegistry;

    public TaggedRetryMetricsPublisher(MeterRegistry meterRegistry) {
        super(MetricNames.ofDefaults());
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    public TaggedRetryMetricsPublisher(MetricNames names, MeterRegistry meterRegistry) {
        super(names);
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    @Override
    public void publishMetrics(Retry entry) {
        addMetrics(meterRegistry, entry);
    }

    @Override
    public void removeMetrics(Retry entry) {
        removeMetrics(meterRegistry, entry.getName());
    }
}
