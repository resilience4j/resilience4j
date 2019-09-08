package io.github.resilience4j.prometheus.publisher;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.prometheus.AbstractThreadPoolBulkheadMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.prometheus.client.GaugeMetricFamily;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ThreadPoolBulkheadMetricsPublisher
        extends AbstractThreadPoolBulkheadMetrics implements MetricsPublisher<ThreadPoolBulkhead> {

    private final GaugeMetricFamily availableCallsFamily;
    private final GaugeMetricFamily maxAllowedCallsFamily;

    public ThreadPoolBulkheadMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public ThreadPoolBulkheadMetricsPublisher(MetricNames names) {
        super(names);
        availableCallsFamily = new GaugeMetricFamily(
                names.getCurrentThreadPoolSizeName(),
                "The number of currently used bulkhead threads",
                LabelNames.NAME
        );

        maxAllowedCallsFamily = new GaugeMetricFamily(
                names.getAvailableQueueCapacityName(),
                "The number of available bulkhead queue slots",
                LabelNames.NAME
        );
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return asList(availableCallsFamily, maxAllowedCallsFamily);
    }

    @Override
    public void publishMetrics(ThreadPoolBulkhead entry) {
        List<String> labelValues = singletonList(entry.getName());
        availableCallsFamily.addMetric(labelValues, entry.getMetrics().getThreadPoolSize());
        maxAllowedCallsFamily.addMetric(labelValues, entry.getMetrics().getRemainingQueueCapacity());
    }

    @Override
    public void removeMetrics(ThreadPoolBulkhead entry) {
        // Do nothing
    }

}
