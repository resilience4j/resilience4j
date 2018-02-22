package io.github.resilience4j.micrometer;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class BulkheadMetricsTest {

    private MeterRegistry meterRegistry;

    @Before
    public void setUp() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    public void shouldRegisterMetrics() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        bulkheadRegistry.bulkhead("testName");

        BulkheadMetrics bulkheadMetrics = BulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry);
        bulkheadMetrics.bindTo(meterRegistry);

        final List<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toList());

        final List<String> expectedMetrics = newArrayList(
                "resilience4j.bulkhead.testName.available_concurrent_calls");
        assertThat(metricNames).hasSameElementsAs(expectedMetrics);

    }


}