/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.CircuitBreakerMetricNames.*;
import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByKindAndNameTags;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedCircuitBreakerMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private CircuitBreaker circuitBreaker;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private TaggedCircuitBreakerMetricsPublisher taggedCircuitBreakerMetricsPublisher;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        taggedCircuitBreakerMetricsPublisher =
            new TaggedCircuitBreakerMetricsPublisher(meterRegistry);
        circuitBreakerRegistry =
            CircuitBreakerRegistry
                .of(CircuitBreakerConfig.ofDefaults(), taggedCircuitBreakerMetricsPublisher);

        CircuitBreakerConfig configWithSlowCallThreshold = CircuitBreakerConfig.custom()
            .slowCallDurationThreshold(Duration.ofSeconds(1)).build();
        circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("backendA", configWithSlowCallThreshold);
        // record some basic stats
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("oops"));
        // record slow call
        circuitBreaker.onSuccess(2000, TimeUnit.NANOSECONDS);
        circuitBreaker.onError(2000, TimeUnit.NANOSECONDS, new RuntimeException("oops"));

    }

    @Test
    public void shouldAddMetricsForANewlyCreatedCircuitBreaker() {
        CircuitBreaker newCircuitBreaker = circuitBreakerRegistry.circuitBreaker("backendB");
        newCircuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);

        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap)
            .containsKeys("backendA", "backendB");
        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap.get("backendA")).hasSize(16);
        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap.get("backendB")).hasSize(16);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(32);

        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS)
            .gauges();

        Optional<Gauge> successful = findMeterByKindAndNameTags(gauges, "successful",
            newCircuitBreaker.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newCircuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(16);

        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap).containsKeys("backendA");
        circuitBreakerRegistry.remove("backendA");

        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void notPermittedCallsCounterReportsCorrespondingValue() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(16);

        Collection<Counter> counters = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_NOT_PERMITTED_CALLS).counters();

        Optional<Counter> notPermitted = findMeterByKindAndNameTags(counters, "not_permitted",
            circuitBreaker.getName());
        assertThat(notPermitted).isPresent();
        assertThat(notPermitted.get().count())
            .isEqualTo(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
    }

    @Test
    public void failedCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS)
            .gauges();

        Optional<Gauge> failed = MetricsTestHelper.findMeterByKindAndNameTags(gauges, "failed",
            circuitBreaker.getName());
        assertThat(failed).isPresent();
        assertThat(failed.get().value())
            .isEqualTo(circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    public void successfulCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS)
            .gauges();

        Optional<Gauge> successful = MetricsTestHelper
            .findMeterByKindAndNameTags(gauges, "successful",
            circuitBreaker.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    public void slowSuccessFulCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS).gauges();

        Optional<Gauge> slow = MetricsTestHelper.findMeterByKindAndNameTags(gauges, "successful",
            circuitBreaker.getName());
        assertThat(slow).isPresent();
        assertThat(slow.get().value())
            .isEqualTo(circuitBreaker.getMetrics().getNumberOfSlowSuccessfulCalls());
    }

    @Test
    public void slowFailedCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_SLOW_CALLS).gauges();

        Optional<Gauge> slow = MetricsTestHelper.findMeterByKindAndNameTags(gauges, "failed",
            circuitBreaker.getName());
        assertThat(slow).isPresent();
        assertThat(slow.get().value())
            .isEqualTo(circuitBreaker.getMetrics().getNumberOfSlowFailedCalls());
    }

    @Test
    public void failureRateGaugeReportsCorrespondingValue() {
        Gauge failureRate = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_FAILURE_RATE).gauge();

        assertThat(failureRate).isNotNull();
        assertThat(failureRate.value()).isEqualTo(circuitBreaker.getMetrics().getFailureRate());
        assertThat(failureRate.getId().getTag(TagNames.NAME)).isEqualTo(circuitBreaker.getName());
    }

    @Test
    public void slowCallRateGaugeReportsCorrespondingValue() {
        Gauge slowCallRate = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_SLOW_CALL_RATE).gauge();

        assertThat(slowCallRate).isNotNull();
        assertThat(slowCallRate.value()).isEqualTo(circuitBreaker.getMetrics().getSlowCallRate());
        assertThat(slowCallRate.getId().getTag(TagNames.NAME)).isEqualTo(circuitBreaker.getName());
    }

    @Test
    public void stateGaugeReportsCorrespondingValue() {
        Gauge state = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_STATE).gauge();

        assertThat(state.value()).isEqualTo(circuitBreaker.getState().getOrder());
        assertThat(state.getId().getTag(TagNames.NAME)).isEqualTo(circuitBreaker.getName());
    }

    @Test
    public void metricsAreRegisteredWithCustomName() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TaggedCircuitBreakerMetricsPublisher taggedCircuitBreakerMetricsPublisher = new TaggedCircuitBreakerMetricsPublisher(
            CircuitBreakerMetricNames.custom()
                .callsMetricName("custom_calls")
                .notPermittedCallsMetricName("custom_not_permitted_calls")
                .stateMetricName("custom_state")
                .bufferedCallsMetricName("custom_buffered_calls")
                .slowCallsMetricName("custom_slow_calls")
                .failureRateMetricName("custom_failure_rate")
                .slowCallRateMetricName("custom_slow_call_rate")
                .build(), meterRegistry
        );
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry
            .of(CircuitBreakerConfig.ofDefaults(), taggedCircuitBreakerMetricsPublisher);
        circuitBreakerRegistry.circuitBreaker("backendA");

        Set<String> metricNames = meterRegistry.getMeters()
            .stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Arrays.asList(
            "custom_calls",
            "custom_not_permitted_calls",
            "custom_state",
            "custom_buffered_calls",
            "custom_slow_calls",
            "custom_failure_rate",
            "custom_slow_call_rate"
        ));
    }

    @Test
    public void testReplaceNewMeter(){
        CircuitBreaker oldOne = CircuitBreaker.of("backendC", CircuitBreakerConfig.ofDefaults());
        // add meters of old
        taggedCircuitBreakerMetricsPublisher.addMetrics(meterRegistry, oldOne);
        // one success call
        oldOne.onSuccess(0, TimeUnit.NANOSECONDS);

        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap.get("backendC")).hasSize(16);
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS).gauges();
        Optional<Gauge> successful = findMeterByKindAndNameTags(gauges, "successful", oldOne.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(oldOne.getMetrics().getNumberOfSuccessfulCalls());

        CircuitBreaker newOne = CircuitBreaker.of("backendC", CircuitBreakerConfig.ofDefaults());

        // add meters of new
        taggedCircuitBreakerMetricsPublisher.addMetrics(meterRegistry, newOne);
        // three success call
        newOne.onSuccess(0, TimeUnit.NANOSECONDS);
        newOne.onSuccess(0, TimeUnit.NANOSECONDS);
        newOne.onSuccess(0, TimeUnit.NANOSECONDS);

        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedCircuitBreakerMetricsPublisher.meterIdMap.get("backendC")).hasSize(16);
        gauges = meterRegistry.get(DEFAULT_CIRCUIT_BREAKER_BUFFERED_CALLS).gauges();
        successful = findMeterByKindAndNameTags(gauges, "successful", newOne.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newOne.getMetrics().getNumberOfSuccessfulCalls());

    }
}