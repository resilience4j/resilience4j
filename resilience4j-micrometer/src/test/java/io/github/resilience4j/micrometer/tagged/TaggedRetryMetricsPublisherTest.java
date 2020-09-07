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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.RetryMetricNames.DEFAULT_RETRY_CALLS;
import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByKindAndNameTags;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedRetryMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private Retry retry;
    private RetryRegistry retryRegistry;
    private TaggedRetryMetricsPublisher taggedRetryMetricsPublisher;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        taggedRetryMetricsPublisher = new TaggedRetryMetricsPublisher(meterRegistry);
        retryRegistry = RetryRegistry.of(RetryConfig.ofDefaults(), taggedRetryMetricsPublisher);

        retry = retryRegistry.retry("backendA");
        // record some basic stats
        retry.executeRunnable(() -> {
        });
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedRetry() {
        Retry newRetry = retryRegistry.retry("backendB");

        assertThat(taggedRetryMetricsPublisher.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedRetryMetricsPublisher.meterIdMap.get("backendA")).hasSize(4);
        assertThat(taggedRetryMetricsPublisher.meterIdMap.get("backendB")).hasSize(4);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(8);

        Collection<FunctionCounter> counters = meterRegistry.get(DEFAULT_RETRY_CALLS)
            .functionCounters();

        Optional<FunctionCounter> successfulWithoutRetry = MetricsTestHelper
            .findMeterByKindAndNameTags(counters, "successful_without_retry", newRetry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().count())
            .isEqualTo(newRetry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(4);

        assertThat(taggedRetryMetricsPublisher.meterIdMap).containsKeys("backendA");
        retryRegistry.remove("backendA");

        assertThat(taggedRetryMetricsPublisher.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Collection<FunctionCounter> counters = meterRegistry.get(DEFAULT_RETRY_CALLS).functionCounters();
        Optional<FunctionCounter> successfulWithoutRetry = MetricsTestHelper.findMeterByKindAndNameTags(counters,
            "successful_without_retry", retry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().count())
            .isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());

        Retry newRetry = Retry.of(retry.getName(), RetryConfig.custom().maxAttempts(1).build());

        retryRegistry.replace(retry.getName(), newRetry);

        counters = meterRegistry.get(DEFAULT_RETRY_CALLS).functionCounters();
        successfulWithoutRetry = MetricsTestHelper
            .findMeterByKindAndNameTags(counters, "successful_without_retry",
            newRetry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().count())
            .isEqualTo(newRetry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void successfulWithoutRetryCallsGaugeReportsCorrespondingValue() {
        Collection<FunctionCounter> counters = meterRegistry.get(DEFAULT_RETRY_CALLS)
            .functionCounters();

        Optional<FunctionCounter> successfulWithoutRetry = findMeterByKindAndNameTags(
            counters, "successful_without_retry", retry.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().count())
            .isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void successfulWithRetryCallsGaugeReportsCorrespondingValue() {
        Collection<FunctionCounter> counters = meterRegistry.get(DEFAULT_RETRY_CALLS)
            .functionCounters();

        Optional<FunctionCounter> successfulWithRetry = MetricsTestHelper.findMeterByKindAndNameTags(counters,
            "successful_with_retry", retry.getName());
        assertThat(successfulWithRetry).isPresent();
        assertThat(successfulWithRetry.get().count())
            .isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
    }

    @Test
    public void failedWithoutRetryCallsGaugeReportsCorrespondingValue() {
        Collection<FunctionCounter> counters = meterRegistry.get(DEFAULT_RETRY_CALLS)
            .functionCounters();

        Optional<FunctionCounter> failedWithoutRetry = MetricsTestHelper.findMeterByKindAndNameTags(
            counters, "failed_without_retry", retry.getName());
        assertThat(failedWithoutRetry).isPresent();
        assertThat(failedWithoutRetry.get().count())
            .isEqualTo(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
    }

    @Test
    public void failedWithRetryCallsGaugeReportsCorrespondingValue() {
        Collection<FunctionCounter> counters = meterRegistry.get(DEFAULT_RETRY_CALLS).
            functionCounters();

        Optional<FunctionCounter> failedWithRetry = MetricsTestHelper
            .findMeterByKindAndNameTags(counters, "failed_with_retry",
            retry.getName());
        assertThat(failedWithRetry).isPresent();
        assertThat(failedWithRetry.get().count())
            .isEqualTo(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
    }

    @Test
    public void metricsAreRegisteredWithCustomNames() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TaggedRetryMetricsPublisher taggedRetryMetricsPublisher = new TaggedRetryMetricsPublisher(
            RetryMetricNames.custom()
                .callsMetricName("custom_calls")
                .build(), meterRegistry);
        RetryRegistry retryRegistry = RetryRegistry
            .of(RetryConfig.ofDefaults(), taggedRetryMetricsPublisher);
        retryRegistry.retry("backendA");

        Set<String> metricNames = meterRegistry.getMeters()
            .stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Collections.singletonList("custom_calls"));
    }

    @Test
    public void testReplaceNewMeter() {
        Retry oldOne = Retry.of("backendC", RetryConfig.ofDefaults());
        // add meters of old
        taggedRetryMetricsPublisher.addMetrics(meterRegistry, oldOne);
        // one success call
        oldOne.executeRunnable(() -> { });

        assertThat(taggedRetryMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedRetryMetricsPublisher.meterIdMap.get("backendC")).hasSize(4);
        Collection<FunctionCounter> counters = meterRegistry.get(DEFAULT_RETRY_CALLS)
            .functionCounters();
        Optional<FunctionCounter> successfulWithoutRetry =
            findMeterByKindAndNameTags(counters, "successful_without_retry", oldOne.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().count())
            .isEqualTo(oldOne.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());


        Retry newOne = Retry.of("backendC", RetryConfig.ofDefaults());
        // add meters of old
        taggedRetryMetricsPublisher.addMetrics(meterRegistry, newOne);
        // three success call
        newOne.executeRunnable(() -> { });
        newOne.executeRunnable(() -> { });
        newOne.executeRunnable(() -> { });

        assertThat(taggedRetryMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedRetryMetricsPublisher.meterIdMap.get("backendC")).hasSize(4);
        counters = meterRegistry.get(DEFAULT_RETRY_CALLS)
            .functionCounters();
        successfulWithoutRetry =
            findMeterByKindAndNameTags(counters, "successful_without_retry", newOne.getName());
        assertThat(successfulWithoutRetry).isPresent();
        assertThat(successfulWithoutRetry.get().count())
            .isEqualTo(newOne.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }
}