/*
 * Copyright 2019 Yevhenii Voievodin
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
package io.github.resilience4j.prometheus.collectors;

import static io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector.MetricNames.DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS;
import static io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector.MetricNames.DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME;
import static io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector.MetricNames.DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS;
import static io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector.MetricNames.DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.prometheus.client.CollectorRegistry;

public class CircuitBreakerMetricsCollectorTest {

    CollectorRegistry registry;
    CircuitBreaker circuitBreaker;

    @Before
    public void setup() {
        registry = new CollectorRegistry();
        circuitBreaker = CircuitBreaker.ofDefaults("backendA");
        // record some basic stats
        circuitBreaker.onSuccess(0);
        circuitBreaker.onError(0, new RuntimeException("oops"));
        circuitBreaker.transitionToOpenState();

        CircuitBreakerMetricsCollector.ofCircuitBreaker(circuitBreaker).register(registry);
    }

    @Test
    public void stateReportsCorrespondingValue() {
        double state = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_STATE_METRIC_NAME,
            new String[]{"name", "state"},
            new String[]{circuitBreaker.getName(), circuitBreaker.getState().name().toLowerCase()}
        );

        assertThat(state).isEqualTo(circuitBreaker.getState().getOrder());
    }

    @Test
    public void bufferedCallsReportsCorrespondingValue() {
        double bufferedCalls = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS,
            new String[]{"name"},
            new String[]{circuitBreaker.getName()}
        );

        assertThat(bufferedCalls).isEqualTo(circuitBreaker.getMetrics().getNumberOfBufferedCalls());
    }

    @Test
    public void maxBufferedCallsReportsCorrespondingValue() {
        double maxBufferedCalls = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_MAX_BUFFERED_CALLS,
            new String[]{"name"},
            new String[]{circuitBreaker.getName()}
        );

        assertThat(maxBufferedCalls).isEqualTo(circuitBreaker.getMetrics().getMaxNumberOfBufferedCalls());
    }

    @Test
    public void successfulCallsReportsCorrespondingValue() {
        double successfulCalls = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME,
            new String[]{"name", "kind"},
            new String[]{circuitBreaker.getName(), "successful"}
        );

        assertThat(successfulCalls).isEqualTo(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    public void failedCallsReportsCorrespondingValue() {
        double failedCalls = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME,
            new String[]{"name", "kind"},
            new String[]{circuitBreaker.getName(), "failed"}
        );

        assertThat(failedCalls).isEqualTo(circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    public void notPermittedCallsReportsCorrespondingValue() {
        double notPermitted = registry.getSampleValue(
            DEFAULT_CIRCUIT_BREAKER_CALLS_METRIC_NAME,
            new String[]{"name", "kind"},
            new String[]{circuitBreaker.getName(), "not_permitted"}
        );

        assertThat(notPermitted).isEqualTo(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
    }

    @Test
    public void customMetricNamesOverrideDefaultOnes() {
        CollectorRegistry registry = new CollectorRegistry();

        CircuitBreakerMetricsCollector.ofSupplier(
            CircuitBreakerMetricsCollector.MetricNames.custom()
                .callsMetricName("custom_calls")
                .stateMetricName("custom_state")
                .maxBufferedCallsMetricName("custom_max_buffered_calls")
                .bufferedCallsMetricName("custom_buffered_calls")
                .build(),
            () -> singletonList(CircuitBreaker.ofDefaults("backendA"))
        ).register(registry);

        assertThat(registry.getSampleValue(
            "custom_calls",
            new String[]{"name", "kind"},
            new String[]{"backendA", "successful"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_calls",
            new String[]{"name", "kind"},
            new String[]{"backendA", "failed"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_calls",
            new String[]{"name", "kind"},
            new String[]{"backendA", "not_permitted"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_state",
            new String[]{"name", "state"},
            new String[]{"backendA", "closed"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_max_buffered_calls",
            new String[]{"name"},
            new String[]{"backendA"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_buffered_calls",
            new String[]{"name"},
            new String[]{"backendA"}
        )).isNotNull();
    }
}
