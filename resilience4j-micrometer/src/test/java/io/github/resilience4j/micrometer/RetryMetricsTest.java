package io.github.resilience4j.micrometer;

import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class RetryMetricsTest {

    private MeterRegistry meterRegistry;

    @Before
    public void setUp() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    public void shouldRegisterMetrics() {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        retryRegistry.retry("testName");

        RetryMetrics retryMetrics = RetryMetrics.ofRetryRegistry(retryRegistry);
        retryMetrics.bindTo(meterRegistry);

        final List<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toList());

        final List<String> expectedMetrics = newArrayList(
                "resilience4j.retry.testName.successful_calls_with_retry",
                "resilience4j.retry.testName.failed_calls_with_retry",
                "resilience4j.retry.testName.successful_calls_without_retry",
                "resilience4j.retry.testName.failed_calls_without_retry");
        assertThat(metricNames).hasSameElementsAs(expectedMetrics);

    }


}