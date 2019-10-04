/*
 * Copyright 2019 authors
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

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeoutException;

import static io.github.resilience4j.prometheus.AbstractTimeLimiterMetrics.MetricNames.DEFAULT_CALLS_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class TimeLimiterMetricsCollectorTest {

    private static final String KIND_SUCCESSFUL = "successful";
    private static final String KIND_FAILED = "failed";
    private static final String KIND_TIMEOUT = "timeout";

    private CollectorRegistry registry;
    private TimeLimiter timeLimiter;
    private TimeLimiterRegistry timeLimiterRegistry;

    @Before
    public void setup() {
        registry = new CollectorRegistry();
        timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiter = timeLimiterRegistry.timeLimiter("backendA");

        TimeLimiterMetricsCollector.ofTimeLimiterRegistry(timeLimiterRegistry).register(registry);
    }

    @Test
    public void successfulCallsReportsCorrespondingValue() {
        timeLimiter.onSuccess();

        Double successfulCalls = getSampleValue(registry, DEFAULT_CALLS_METRIC_NAME, KIND_SUCCESSFUL);

        assertThat(successfulCalls).isEqualTo(1);
    }

    @Test
    public void failedCallsReportsCorrespondingValue() {
        timeLimiter.onError(new RuntimeException());

        Double failedCalls = getSampleValue(registry, DEFAULT_CALLS_METRIC_NAME, KIND_FAILED);

        assertThat(failedCalls).isEqualTo(1);
    }

    @Test
    public void timeoutCallsReportsCorrespondingValue() {
        timeLimiter.onError(new TimeoutException());

        Double timeoutCalls = getSampleValue(registry, DEFAULT_CALLS_METRIC_NAME, KIND_TIMEOUT);

        assertThat(timeoutCalls).isEqualTo(1);
    }

    @Test
    public void customMetricNamesOverrideDefaultOnes() {
        TimeLimiterMetricsCollector.MetricNames names = TimeLimiterMetricsCollector.MetricNames.custom()
                .callsMetricName("custom_calls")
                .build();
        CollectorRegistry customRegistry = new CollectorRegistry();
        TimeLimiterMetricsCollector.ofTimeLimiterRegistry(names, timeLimiterRegistry).register(customRegistry);
        timeLimiter.onSuccess();
        timeLimiter.onError(new RuntimeException());
        timeLimiter.onError(new TimeoutException());

        Double successfulCalls = getSampleValue(customRegistry, "custom_calls", KIND_SUCCESSFUL);
        Double failedCalls = getSampleValue(customRegistry, "custom_calls", KIND_FAILED);
        Double timeoutCalls = getSampleValue(customRegistry, "custom_calls", KIND_TIMEOUT);

        assertThat(successfulCalls).isNotNull();
        assertThat(failedCalls).isNotNull();
        assertThat(timeoutCalls).isNotNull();
    }

    private Double getSampleValue(CollectorRegistry collectorRegistry, String metricName, String metricKind) {
        return collectorRegistry.getSampleValue(
                metricName,
                new String[]{"name", "kind"},
                new String[]{timeLimiter.getName(), metricKind}
        );
    }
}
