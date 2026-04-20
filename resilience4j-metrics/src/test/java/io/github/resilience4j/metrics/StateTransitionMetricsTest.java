/*
 *
 *  Copyright 2026 authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.metrics;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.publisher.CircuitBreakerMetricsPublisher;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class StateTransitionMetricsTest {

    @SuppressWarnings("rawtypes")
    private static void circuitBreakerMetricsUsesFirstStateObjectInstance(
        CircuitBreaker circuitBreaker, MetricRegistry metricRegistry) throws Exception {
        SortedMap<String, Gauge> gauges = metricRegistry.getGauges();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue()).isEqualTo(0);

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue()).isEqualTo(1);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue()).isEqualTo(1);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue()).isEqualTo(0);

        for (int i = 0; i < 9; i++) {
            circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(10);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(10);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue()).isEqualTo(1);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue()).isEqualTo(10);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue()).isEqualTo(10);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue()).isEqualTo(0);

        await().atMost(1500, TimeUnit.MILLISECONDS)
            .until(() -> {
                circuitBreaker.tryAcquirePermission();
                return circuitBreaker.getState().equals(CircuitBreaker.State.HALF_OPEN);
            });

        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isOne();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isOne();

        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue()).isEqualTo(2);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue()).isEqualTo(1);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue()).isEqualTo(1);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);

        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue()).isEqualTo(2);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue()).isEqualTo(2);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue()).isEqualTo(2);
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();

        assertThat(gauges.get("resilience4j.circuitbreaker.test.state").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.buffered").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.failed").getValue()).isEqualTo(0);
        assertThat(gauges.get("resilience4j.circuitbreaker.test.successful").getValue()).isEqualTo(0);
    }

    @Test
    void withCircuitBreakerMetrics() throws Exception {
        CircuitBreakerConfig config =
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofMillis(150))
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .build();
        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults()
            .circuitBreaker("test", config);
        MetricRegistry metricRegistry = new MetricRegistry();

        metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreaker(circuitBreaker));
        circuitBreakerMetricsUsesFirstStateObjectInstance(circuitBreaker, metricRegistry);
    }

    @Test
    void withCircuitBreakerMetricsPublisher() throws Exception {
        CircuitBreakerConfig config =
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .build();
        MetricRegistry metricRegistry = new MetricRegistry();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(config, new CircuitBreakerMetricsPublisher(metricRegistry));
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test", config);

        circuitBreakerMetricsUsesFirstStateObjectInstance(circuitBreaker, metricRegistry);
    }
}
