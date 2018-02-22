package io.github.resilience4j.micrometer;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static io.github.resilience4j.bulkhead.utils.MetricNames.AVAILABLE_CONCURRENT_CALLS;
import static io.github.resilience4j.bulkhead.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.micrometer.MetricUtils.getName;
import static java.util.Objects.requireNonNull;

public class BulkheadMetrics implements MeterBinder {

    private final Iterable<Bulkhead> bulkheads;
    private final String prefix;

    private BulkheadMetrics(Iterable<Bulkhead> bulkheads) {
        this(bulkheads, DEFAULT_PREFIX);
    }

    private BulkheadMetrics(Iterable<Bulkhead> bulkheads, String prefix) {
        this.bulkheads = requireNonNull(bulkheads);
        this.prefix = requireNonNull(prefix);
    }

    /**
     * Creates a new instance BulkheadMetrics {@link BulkheadMetrics} with
     * a {@link BulkheadRegistry} as a source.
     *
     * @param bulkheadRegistry the registry of bulkheads
     */
    public static BulkheadMetrics ofBulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
        return new BulkheadMetrics(bulkheadRegistry.getAllBulkheads());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Bulkhead bulkhead : bulkheads) {
            final String name = bulkhead.getName();
            Gauge.builder(getName(prefix, name, AVAILABLE_CONCURRENT_CALLS), bulkhead, (cb) -> cb.getMetrics().getAvailableConcurrentCalls())
                    .register(registry);
        }
    }
}
