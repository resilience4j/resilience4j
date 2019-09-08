package io.github.resilience4j.prometheus.publisher;

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.prometheus.AbstractTimeLimiterMetrics;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.Collections;
import java.util.List;

public class TimeLimiterMetricsPublisher extends AbstractTimeLimiterMetrics implements MetricsPublisher<TimeLimiter> {

    public TimeLimiterMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public TimeLimiterMetricsPublisher(MetricNames names) {
        super(names);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return Collections.list(collectorRegistry.metricFamilySamples());
    }

    @Override
    public void publishMetrics(TimeLimiter entry) {
        String name = entry.getName();
        entry.getEventPublisher()
                .onSuccess(event -> callsCounter.labels(name, KIND_SUCCESSFUL).inc())
                .onError(event -> callsCounter.labels(name, KIND_FAILED).inc())
                .onTimeout(event -> callsCounter.labels(name, KIND_TIMEOUT).inc());
    }

    @Override
    public void removeMetrics(TimeLimiter entry) {
        // Do nothing
    }
}
