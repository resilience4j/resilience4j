package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import io.github.resilience4j.core.metrics.MetricsPublisher;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

abstract class AbstractMetricsPublisher<E> implements MetricSet, MetricsPublisher<E> {

    protected final MetricRegistry metricRegistry;
    protected final ConcurrentMap<String, Set<String>> metricsNameMap = new ConcurrentHashMap<>();

    protected AbstractMetricsPublisher(MetricRegistry metricRegistry) {
        this.metricRegistry = requireNonNull(metricRegistry);
    }

    protected void removeMetrics(String name) {
        Set<String> nameSet = metricsNameMap.get(name);
        if (nameSet != null) {
            nameSet.forEach(metricRegistry::remove);
        }
        metricsNameMap.remove(name);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }

}
