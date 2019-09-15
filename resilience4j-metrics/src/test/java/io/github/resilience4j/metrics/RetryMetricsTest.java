package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;

public class RetryMetricsTest extends AbstractRetryMetricsTest{

    @Override
    protected Retry given(String prefix, MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom().waitDuration(Duration.ofMillis(150)).build());
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(prefix, retryRegistry));

        return retry;
    }

    @Override
    protected Retry given(MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom().waitDuration(Duration.ofMillis(150)).build());
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(retryRegistry));

        return retry;
    }
}
