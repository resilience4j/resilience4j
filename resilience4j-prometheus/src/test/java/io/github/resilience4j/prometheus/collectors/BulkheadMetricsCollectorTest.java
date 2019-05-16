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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import static io.github.resilience4j.prometheus.collectors.BulkheadMetricsCollector.MetricNames.DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME;
import static io.github.resilience4j.prometheus.collectors.BulkheadMetricsCollector.MetricNames.DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class BulkheadMetricsCollectorTest {

    CollectorRegistry registry;
    Bulkhead bulkhead;
    BulkheadRegistry bulkheadRegistry;

    @Before
    public void setup() {
        registry = new CollectorRegistry();
        bulkheadRegistry = BulkheadRegistry.ofDefaults();
        bulkhead = bulkheadRegistry.bulkhead("backendA");
        BulkheadMetricsCollector.ofBulkheadRegistry(bulkheadRegistry).register(registry);
        // record some basic stats
        bulkhead.tryAcquirePermission();
        bulkhead.tryAcquirePermission();
    }

    @Test
    public void availableConcurrentCallsReportsCorrespondingValue() {
        double availableCalls = registry.getSampleValue(
            DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME,
            new String[]{"name"},
            new String[]{bulkhead.getName()}
        );

        assertThat(availableCalls).isEqualTo(bulkhead.getMetrics().getAvailableConcurrentCalls());
    }

    @Test
    public void maxAllowedConcurrentCallsReportsCorrespondingValue() {
        double maxAllowed = registry.getSampleValue(
            DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME,
            new String[]{"name"},
            new String[]{bulkhead.getName()}
        );

        assertThat(maxAllowed).isEqualTo(bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
    }

    @Test
    public void customMetricNamesOverrideDefaultOnes() {
        CollectorRegistry registry = new CollectorRegistry();

        BulkheadMetricsCollector.ofBulkheadRegistry(
            BulkheadMetricsCollector.MetricNames.custom()
                .availableConcurrentCallsMetricName("custom_available_calls")
                .maxAllowedConcurrentCallsMetricName("custom_max_allowed_calls")
                .build(),
                bulkheadRegistry ).register(registry);

        assertThat(registry.getSampleValue(
            "custom_available_calls",
            new String[]{"name"},
            new String[]{"backendA"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_max_allowed_calls",
            new String[]{"name"},
            new String[]{"backendA"}
        )).isNotNull();
    }
}
