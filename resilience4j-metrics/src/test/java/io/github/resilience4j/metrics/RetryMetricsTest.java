package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

public class RetryMetricsTest extends AbstractRetryMetricsTest{

    @Override
    protected Retry given(String prefix, MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(prefix, retryRegistry));

        return retry;
    }

    @Override
    protected Retry given(MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(retryRegistry));

        return retry;
    }
}
