package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.micrometer.core.instrument.MeterRegistry;

import static java.util.Objects.requireNonNull;

public class TaggedThreadPoolBulkheadMetricsPublisher
        extends AbstractThreadPoolBulkheadMetrics implements MetricsPublisher<ThreadPoolBulkhead> {

    private final MeterRegistry meterRegistry;

    public TaggedThreadPoolBulkheadMetricsPublisher(MeterRegistry meterRegistry) {
        super(MetricNames.ofDefaults());
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    public TaggedThreadPoolBulkheadMetricsPublisher(MetricNames names, MeterRegistry meterRegistry) {
        super(names);
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    @Override
    public void publishMetrics(ThreadPoolBulkhead entry) {
        addMetrics(meterRegistry, entry);
    }

    @Override
    public void removeMetrics(ThreadPoolBulkhead entry) {
        removeMetrics(meterRegistry, entry.getName());
    }
}
