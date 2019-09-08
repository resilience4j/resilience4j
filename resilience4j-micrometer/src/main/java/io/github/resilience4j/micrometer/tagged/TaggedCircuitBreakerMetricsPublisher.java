package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.micrometer.core.instrument.MeterRegistry;

import static java.util.Objects.requireNonNull;

public class TaggedCircuitBreakerMetricsPublisher
        extends AbstractCircuitBreakerMetrics implements MetricsPublisher<CircuitBreaker> {

    private final MeterRegistry meterRegistry;

    public TaggedCircuitBreakerMetricsPublisher(MeterRegistry meterRegistry) {
        super(MetricNames.ofDefaults());
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    public TaggedCircuitBreakerMetricsPublisher(MetricNames names, MeterRegistry meterRegistry) {
        super(names);
        this.meterRegistry = requireNonNull(meterRegistry);
    }

    @Override
    public void publishMetrics(CircuitBreaker entry) {
        addMetrics(meterRegistry, entry);
    }

    @Override
    public void removeMetrics(CircuitBreaker entry) {
        removeMetrics(meterRegistry, entry.getName());
    }
}
