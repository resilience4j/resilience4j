package io.github.resilience4j.micrometer;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class CircuitBreakerMetricsTest {

    private MeterRegistry meterRegistry;

    @Before
    public void setUp() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    public void shouldRegisterMetrics() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        circuitBreakerRegistry.circuitBreaker("testName");

        CircuitBreakerMetrics circuitBreakerMetrics = CircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        circuitBreakerMetrics.bindTo(meterRegistry);

        final List<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toList());

        final List<String> expectedMetrics = newArrayList(
                "resilience4j.circuitbreaker.testName.successful",
                "resilience4j.circuitbreaker.testName.failed",
                "resilience4j.circuitbreaker.testName.not_permitted",
                "resilience4j.circuitbreaker.testName.state",
                "resilience4j.circuitbreaker.testName.buffered",
                "resilience4j.circuitbreaker.testName.buffered_max");
        assertThat(metricNames).hasSameElementsAs(expectedMetrics);

    }


}