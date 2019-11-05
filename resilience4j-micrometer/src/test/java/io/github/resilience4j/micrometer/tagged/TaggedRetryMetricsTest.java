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
package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.AbstractRetryMetrics.MetricNames.DEFAULT_RETRY_CALLS;
import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findGaugeByKindAndNameTags;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedRetryMetricsTest {

    private MeterRegistry meterRegistry;
    private Retry retry;
    private RetryRegistry retryRegistry;
    private TaggedRetryMetrics taggedRetryMetrics;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        retryRegistry = RetryRegistry.ofDefaults();

        retry = retryRegistry.retry("backendA");
        // record some basic stats
        retry.executeRunnable(() -> {});

        taggedRetryMetrics = TaggedRetryMetrics.ofRetryRegistry(retryRegistry);
        taggedRetryMetrics.bindTo(meterRegistry);
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedRetry() {
        Retry newRetry = retryRegistry.retry("backendB");

        assertThat(taggedRetryMetrics.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedRetryMetrics.meterIdMap.get("backendA")).hasSize(4);
        assertThat(taggedRetryMetrics.meterIdMap.get("backendB")).hasSize(4);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(8);

        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> successfulWithoutRetry = findGaugeByKindAndNameTags(gauges, "successful_without_retry", newRetry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().value()).isEqualTo(newRetry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void shouldAddCustomTags() {
        retryRegistry.retry("backendF", io.vavr.collection.HashMap.of("key1", "value1"));
        assertThat(taggedRetryMetrics.meterIdMap).containsKeys("backendA", "backendF");
        assertThat(taggedRetryMetrics.meterIdMap.get("backendA")).hasSize(4);
        assertThat(taggedRetryMetrics.meterIdMap.get("backendF")).hasSize(4);
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(8);
        assertThat(meterRegistry.get(DEFAULT_RETRY_CALLS).tag("key1", "value1")).isNotNull();

    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(4);

        assertThat(taggedRetryMetrics.meterIdMap).containsKeys("backendA");
        retryRegistry.remove("backendA");

        assertThat(taggedRetryMetrics.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();
        Optional<Gauge> successfulWithoutRetry = findGaugeByKindAndNameTags(gauges, "successful_without_retry", retry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());


        Retry newRetry = Retry.of(retry.getName(), RetryConfig.custom().maxAttempts(1).build());

        retryRegistry.replace(retry.getName(), newRetry);

        gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();
        successfulWithoutRetry = findGaugeByKindAndNameTags(gauges, "successful_without_retry", newRetry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().value()).isEqualTo(newRetry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void successfulWithoutRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> successfulWithoutRetry = findGaugeByKindAndNameTags(gauges, "successful_without_retry", retry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void successfulWithRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> successfulWithRetry = findGaugeByKindAndNameTags(gauges, "successful_with_retry", retry.getName());
        assertThat(successfulWithRetry).isPresent();
        assertThat(successfulWithRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
    }

    @Test
    public void failedWithoutRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> failedWithoutRetry = findGaugeByKindAndNameTags(gauges, "failed_without_retry", retry.getName());
        assertThat(failedWithoutRetry).isPresent();
        assertThat(failedWithoutRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
    }

    @Test
    public void failedWithRetryCallsGaugeReportsCorrespondingValue() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_RETRY_CALLS).gauges();

        Optional<Gauge> failedWithRetry = findGaugeByKindAndNameTags(gauges, "failed_with_retry", retry.getName());
        assertThat(failedWithRetry).isPresent();
        assertThat(failedWithRetry.get().value()).isEqualTo(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
    }

    @Test
    public void metricsAreRegisteredWithCustomNames() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        retryRegistry.retry("backendA");
        TaggedRetryMetrics.ofRetryRegistry(
                TaggedRetryMetrics.MetricNames.custom()
                        .callsMetricName("custom_calls")
                        .build(),
                retryRegistry
        ).bindTo(meterRegistry);

        Set<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Collections.singletonList("custom_calls"));
    }
}
