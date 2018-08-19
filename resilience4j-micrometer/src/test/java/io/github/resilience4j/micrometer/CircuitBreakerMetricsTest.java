/*
 * Copyright 2018 Julien Hoarau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micrometer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
                .distinct() // collapse tag variants
                .collect(Collectors.toList());

        final List<String> expectedMetrics = newArrayList(
                "resilience4j.circuitbreaker.testName.successful",
                "resilience4j.circuitbreaker.testName.failed",
                "resilience4j.circuitbreaker.testName.not_permitted",
                "resilience4j.circuitbreaker.testName.state",
                "resilience4j.circuitbreaker.testName.buffered",
                "resilience4j.circuitbreaker.testName.buffered_max",
                "resilience4j.circuitbreaker.testName.elapsed");
        assertThat(metricNames).hasSameElementsAs(expectedMetrics);

    }

    @Test
    public void shouldPublishElapsedTimeMetricTaggedWithSuccess() throws Exception {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        CircuitBreakerMetrics circuitBreakerMetrics = CircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        circuitBreakerMetrics.bindTo(meterRegistry);

        circuitBreaker.executeCallable(() -> null);

        assertThat(meterRegistry.timer("resilience4j.circuitbreaker.testName.elapsed", "result", "success").count())
                .isEqualTo(1L);
    }

    @Test
    public void shouldPublishElapsedTimeMetricTaggedWithError() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        CircuitBreakerMetrics circuitBreakerMetrics = CircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        circuitBreakerMetrics.bindTo(meterRegistry);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
            circuitBreaker.executeCallable(() -> { throw new IllegalStateException(); }));

        assertThat(meterRegistry.timer("resilience4j.circuitbreaker.testName.elapsed", "result", "error").count())
                .isEqualTo(1L);
    }
}