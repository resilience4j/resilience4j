package io.github.resilience4j.metrics;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.publisher.CircuitBreakerMetricsPublisher;
import org.junit.Test;

import java.time.Duration;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class StateTransitionMetricsTest {

    @Test
    public void testWithCircuitBreakerMetrics() throws Exception {
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .waitDurationInOpenState(Duration.ofMillis(150))
                        .failureRateThreshold(50)
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .slidingWindowSize(10)
                        .build();
        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("test", config);
        MetricRegistry metricRegistry = new MetricRegistry();

        metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreaker(circuitBreaker));
        circuitBreakerMetricsUsesFirstStateObjectInstance(circuitBreaker, metricRegistry);
    }

    @Test
    public void testWithCircuitBreakerMetricsPublisher() throws Exception {
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .waitDurationInOpenState(Duration.ofSeconds(1))
                        .failureRateThreshold(50)
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .slidingWindowSize(10)
                        .build();
        MetricRegistry metricRegistry = new MetricRegistry();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(config, new CircuitBreakerMetricsPublisher(metricRegistry));
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test", config);

        circuitBreakerMetricsUsesFirstStateObjectInstance(circuitBreaker, metricRegistry);
    }


    private static void circuitBreakerMetricsUsesFirstStateObjectInstance(
            CircuitBreaker circuitBreaker, MetricRegistry metricRegistry) throws Exception {
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
