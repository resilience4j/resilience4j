package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.bulkhead.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class BulkheadMetricsPublisher extends AbstractMetricsPublisher<Bulkhead> {

    private final String prefix;

    public BulkheadMetricsPublisher() {
        this(DEFAULT_PREFIX, new MetricRegistry());
    }

    public BulkheadMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX, metricRegistry);
    }

    public BulkheadMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(Bulkhead entry) {
        String name = entry.getName();

        //number of available concurrent calls as an integer
        String availableConcurrentCalls = name(prefix, name, AVAILABLE_CONCURRENT_CALLS);
        String maxAllowedConcurrentCalls = name(prefix, name, MAX_ALLOWED_CONCURRENT_CALLS);

        metricRegistry.register(availableConcurrentCalls, (Gauge<Integer>) () -> entry.getMetrics().getAvailableConcurrentCalls());
        metricRegistry.register(maxAllowedConcurrentCalls, (Gauge<Integer>) () -> entry.getMetrics().getMaxAllowedConcurrentCalls());

        List<String> metricNames = Arrays.asList(availableConcurrentCalls, maxAllowedConcurrentCalls);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(Bulkhead entry) {
        removeMetrics(entry.getName());
    }
}
