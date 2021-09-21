/*
 * Copyright 2019 Yevhenii Voievodin, ,Mahmoud Romeh
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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.BulkheadMetricNames.DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME;
import static io.github.resilience4j.micrometer.tagged.BulkheadMetricNames.DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME;
import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByNamesTag;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedBulkheadMetricsTest {

    private MeterRegistry meterRegistry;
    private Bulkhead bulkhead;
    private BulkheadRegistry bulkheadRegistry;
    private TaggedBulkheadMetrics taggedBulkheadMetrics;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        bulkheadRegistry = BulkheadRegistry.ofDefaults();
        bulkhead = bulkheadRegistry.bulkhead("backendA");

        // record some basic stats
        bulkhead.tryAcquirePermission();
        bulkhead.tryAcquirePermission();

        taggedBulkheadMetrics = TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry);
        taggedBulkheadMetrics.bindTo(meterRegistry);
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedRetry() {
        Bulkhead newBulkhead = bulkheadRegistry.bulkhead("backendB");

        assertThat(taggedBulkheadMetrics.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedBulkheadMetrics.meterIdMap.get("backendA")).hasSize(2);
        assertThat(taggedBulkheadMetrics.meterIdMap.get("backendB")).hasSize(2);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(4);

        Collection<Gauge> gauges = meterRegistry
            .get(DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME).gauges();

        Optional<Gauge> successful = findMeterByNamesTag(gauges, newBulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newBulkhead.getMetrics().getMaxAllowedConcurrentCalls());
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(2);

        assertThat(taggedBulkheadMetrics.meterIdMap).containsKeys("backendA");
        bulkheadRegistry.remove("backendA");

        assertThat(taggedBulkheadMetrics.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Collection<Gauge> gauges = meterRegistry
            .get(DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME).gauges();

        Optional<Gauge> successful = findMeterByNamesTag(gauges, bulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(bulkhead.getMetrics().getMaxAllowedConcurrentCalls());

        Bulkhead newBulkhead = Bulkhead.of(bulkhead.getName(), BulkheadConfig.custom()
            .maxConcurrentCalls(100).build());

        bulkheadRegistry.replace(bulkhead.getName(), newBulkhead);

        gauges = meterRegistry.get(DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME)
            .gauges();

        successful = findMeterByNamesTag(gauges, newBulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newBulkhead.getMetrics().getMaxAllowedConcurrentCalls());

    }

    @Test
    public void availableConcurrentCallsGaugeIsRegistered() {
        Gauge available = meterRegistry.get(DEFAULT_BULKHEAD_AVAILABLE_CONCURRENT_CALLS_METRIC_NAME)
            .gauge();

        assertThat(available).isNotNull();
        assertThat(available.value())
            .isEqualTo(bulkhead.getMetrics().getAvailableConcurrentCalls());
    }

    @Test
    public void maxAllowedConcurrentCallsGaugeIsRegistered() {
        Gauge maxAllowed = meterRegistry
            .get(DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value())
            .isEqualTo(bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void customTagsShouldBeAdded() {
        Bulkhead bulkheadC = bulkheadRegistry.bulkhead("backendC", Map.of("key1", "value1"));
        // record some basic stats
        bulkheadC.tryAcquirePermission();
        bulkheadC.tryAcquirePermission();

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(4);
        final RequiredSearch match = meterRegistry.get(DEFAULT_BULKHEAD_MAX_ALLOWED_CONCURRENT_CALLS_METRIC_NAME).tags("key1", "value1");
        assertThat(match).isNotNull();
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        bulkhead = bulkheadRegistry.bulkhead("backendA");
        TaggedBulkheadMetrics.ofBulkheadRegistry(
            BulkheadMetricNames.custom()
                .availableConcurrentCallsMetricName("custom_available_calls")
                .maxAllowedConcurrentCallsMetricName("custom_max_allowed_calls")
                .build(),
            bulkheadRegistry
        ).bindTo(meterRegistry);

        Set<String> metricNames = meterRegistry.getMeters()
            .stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Arrays.asList(
            "custom_available_calls",
            "custom_max_allowed_calls"
        ));
    }
}
