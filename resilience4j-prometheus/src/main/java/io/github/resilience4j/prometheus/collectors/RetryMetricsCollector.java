/*
 * Copyright 2019 Robert Winkler
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

import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.prometheus.AbstractRetryMetrics;
import io.github.resilience4j.prometheus.LabelNames;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;

/** Collects Retry exposed {@link Metrics}. */
public class RetryMetricsCollector extends AbstractRetryMetrics {

    /**
     * Creates a new collector with custom metric names and
     * using given {@code supplier} as source of retries.
     *
     * @param names    the custom metric names
     * @param retryRegistry the source of retries
     */
    public static RetryMetricsCollector ofRetryRegistry(RetryMetricsCollector.MetricNames names, RetryRegistry retryRegistry) {
        return new RetryMetricsCollector(names, retryRegistry);
    }

    /**
     * Creates a new collector using given {@code registry} as source of retries.
     *
     * @param retryRegistry the source of retries
     */
    public static RetryMetricsCollector ofRetryRegistry(RetryRegistry retryRegistry) {
        return new RetryMetricsCollector(RetryMetricsCollector.MetricNames.ofDefaults(), retryRegistry);
    }

    private final RetryRegistry retryRegistry;

    private RetryMetricsCollector(MetricNames names, RetryRegistry retryRegistry) {
        super(names);
        this.retryRegistry = Objects.requireNonNull(retryRegistry);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily retryCallsFamily = new GaugeMetricFamily(
                names.getCallsMetricName(),
            "The number of calls",
            LabelNames.NAME_AND_KIND
        );

        for (Retry retry: retryRegistry.getAllRetries()) {
            retryCallsFamily.addMetric(asList(retry.getName(), "successful_without_retry"), retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryCallsFamily.addMetric(asList(retry.getName(), "successful_with_retry"), retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            retryCallsFamily.addMetric(asList(retry.getName(), "failed_without_retry"), retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            retryCallsFamily.addMetric(asList(retry.getName(), "failed_with_retry"), retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());

        }

        return Collections.singletonList(retryCallsFamily);
    }

}
