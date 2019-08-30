package io.github.resilience4j.metrics;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class StateTransitionMetricsTest {

    private MetricRegistry metricRegistry = new MetricRegistry();
    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp() throws Exception {
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .waitDurationInOpenState(Duration.ofSeconds(1))
                        .failureRateThreshold(50)
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .slidingWindowSize(10)
                        .build();
        circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("test", config);

        metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreaker(circuitBreaker));
    }

    @Test
    public void circuitBreakerMetricsUsesFirstStateObjectInstance() throws Exception {
        SortedMap<String, Gauge> gauges = metricRegistry.getGauges();

        assertThat(circuitBreaker.getState(), equalTo(CircuitBreaker.State.CLOSED));
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls(), equalTo(0));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls(), equalTo(0));
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue(), equalTo(0));

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        assertThat(circuitBreaker.getState(), equalTo(CircuitBreaker.State.CLOSED));
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls(), equalTo(1));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls(), equalTo(1));
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue(), equalTo(1));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue(), equalTo(1));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue(), equalTo(0));

        for (int i = 0; i < 9; i++) {
            circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        }

        assertThat(circuitBreaker.getState(), equalTo(CircuitBreaker.State.OPEN));
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls(), equalTo(10));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls(), equalTo(10));
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue(), equalTo(1));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue(), equalTo(10));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue(), equalTo(10));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue(), equalTo(0));

        await().atMost(1500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    circuitBreaker.tryAcquirePermission();
                    return circuitBreaker.getState().equals(CircuitBreaker.State.HALF_OPEN);
                });

        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState(), equalTo(CircuitBreaker.State.HALF_OPEN));
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls(), equalTo(1));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls(), equalTo(0));
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(), equalTo(1));

        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue(), equalTo(2));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue(), equalTo(1));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue(), equalTo(1));
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState(), equalTo(CircuitBreaker.State.HALF_OPEN));
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls(), equalTo(2));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls(), equalTo(0));
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(), equalTo(2));

        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue(), equalTo(2));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue(), equalTo(2));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue(), equalTo(2));
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState(), equalTo(CircuitBreaker.State.CLOSED));
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls(), equalTo(0));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls(), equalTo(0));
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(), equalTo(0));

        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue(), equalTo(0));
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue(), equalTo(0));
    }
}
