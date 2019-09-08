package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.micrometer.core.instrument.MeterRegistry;

import static java.util.Objects.requireNonNull;

public class TaggedBulkheadMetricsPublisher
        extends AbstractBulkheadMetrics implements MetricsPublisher<Bulkhead> {

    private final MeterRegistry meterRegistry;

    public TaggedBulkheadMetricsPublisher(MeterRegistry meterRegistry) {
        super(MetricNames.ofDefaults());
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    public TaggedBulkheadMetricsPublisher(MetricNames names, MeterRegistry meterRegistry) {
        super(names);
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    @Override
    public void publishMetrics(Bulkhead entry) {
        addMetrics(meterRegistry, entry);
    }

    @Override
    public void removeMetrics(Bulkhead entry) {
        removeMetrics(meterRegistry, entry.getName());
    }
}
