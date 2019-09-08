package io.github.resilience4j.prometheus.publisher;

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.prometheus.AbstractRetryMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.github.resilience4j.retry.Retry;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class RetryMetricsPublisher extends AbstractRetryMetrics implements MetricsPublisher<Retry> {

    private final GaugeMetricFamily retryCallsFamily;

    public RetryMetricsPublisher() {
        this(MetricNames.ofDefaults());
    }

    public RetryMetricsPublisher(MetricNames names) {
        super(names);
        retryCallsFamily = new GaugeMetricFamily(names.getCallsMetricName(), "The number of calls", LabelNames.NAME_AND_KIND);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return Collections.singletonList(retryCallsFamily);
    }

    @Override
    public void publishMetrics(Retry entry) {
        retryCallsFamily.addMetric(asList(entry.getName(), "successful_without_retry"), entry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
        retryCallsFamily.addMetric(asList(entry.getName(), "successful_with_retry"), entry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
        retryCallsFamily.addMetric(asList(entry.getName(), "failed_without_retry"), entry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
        retryCallsFamily.addMetric(asList(entry.getName(), "failed_with_retry"), entry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
    }

    @Override
    public void removeMetrics(Retry entry) {
        // Do nothing
    }

}
