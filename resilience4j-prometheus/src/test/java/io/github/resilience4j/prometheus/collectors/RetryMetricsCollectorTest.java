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


import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.prometheus.client.CollectorRegistry;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static io.github.resilience4j.prometheus.collectors.RetryMetricsCollector.MetricNames.DEFAULT_RETRY_CALLS;
import static org.assertj.core.api.Assertions.assertThat;

public class RetryMetricsCollectorTest {

    CollectorRegistry registry;
    Retry retry;
    RetryRegistry retryRegistry;

    @Before
    public void setup() {
        registry = new CollectorRegistry();
        retryRegistry = RetryRegistry.ofDefaults();
        retry = retryRegistry.retry("backendA");

        RetryMetricsCollector.ofRetryRegistry(retryRegistry).register(registry);

        retry.executeSupplier(() -> "return");

        Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
            throw new RuntimeException();
        });

        Try.ofSupplier(supplier);
    }

    @Test
    public void successfulCallsWithoutRetryReportsCorrespondingValue() {
        double successfulCallsWithoutRetry = registry.getSampleValue(
            DEFAULT_RETRY_CALLS,
            new String[]{"name", "kind"},
            new String[]{retry.getName(), "successful_without_retry"}
        );

        assertThat(successfulCallsWithoutRetry)
            .isEqualTo(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    @Test
    public void callsReportsCorrespondingValue() {
        double failedCallsWithRetry = registry.getSampleValue(
            DEFAULT_RETRY_CALLS,
            new String[]{"name", "kind"},
            new String[]{retry.getName(), "failed_with_retry"}
        );

        assertThat(failedCallsWithRetry)
            .isEqualTo(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
    }

    @Test
    public void customMetricNamesOverrideDefaultOnes() {
        CollectorRegistry registry = new CollectorRegistry();

        RetryMetricsCollector.ofRetryRegistry(
            RetryMetricsCollector.MetricNames.custom()
                .callsMetricName("custom_resilience4j_retry_calls")
                .build(),
            retryRegistry).register(registry);

        assertThat(registry.getSampleValue(
            "custom_resilience4j_retry_calls",
            new String[]{"name", "kind"},
            new String[]{"backendA", "successful_without_retry"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_resilience4j_retry_calls",
            new String[]{"name", "kind"},
            new String[]{"backendA", "failed_with_retry"}
        )).isNotNull();
    }
}
