/*
 * Copyright 2019 Robert Winkler, Mahmoud Romeh
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

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByNamesTag;
import static io.github.resilience4j.micrometer.tagged.ThreadPoolBulkheadMetricNames.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedThreadPoolBulkheadMetricsTest {

    private MeterRegistry meterRegistry;
    private ThreadPoolBulkhead bulkhead;
    private ThreadPoolBulkheadRegistry bulkheadRegistry;
    private TaggedThreadPoolBulkheadMetrics taggedBulkheadMetrics;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        bulkheadRegistry = ThreadPoolBulkheadRegistry.ofDefaults();
        bulkhead = bulkheadRegistry.bulkhead("backendA");

        // record some basic stats
        bulkhead.executeSupplier(() -> "Bla");
        bulkhead.executeSupplier(() -> "Bla");

        taggedBulkheadMetrics = TaggedThreadPoolBulkheadMetrics
            .ofThreadPoolBulkheadRegistry(bulkheadRegistry);
        taggedBulkheadMetrics.bindTo(meterRegistry);
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedRetry() {
        ThreadPoolBulkhead newBulkhead = bulkheadRegistry.bulkhead("backendB");

        assertThat(taggedBulkheadMetrics.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedBulkheadMetrics.meterIdMap.get("backendA")).hasSize(5);
        assertThat(taggedBulkheadMetrics.meterIdMap.get("backendB")).hasSize(5);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(10);

        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME)
            .gauges();

        Optional<Gauge> successful = findMeterByNamesTag(gauges, newBulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newBulkhead.getMetrics().getMaximumThreadPoolSize());
    }

    @Test
    public void shouldAddCustomTags() {
        bulkheadRegistry.bulkhead("backendF", Map.of("key1", "value1"));
        assertThat(taggedBulkheadMetrics.meterIdMap).containsKeys("backendA", "backendF");
        assertThat(taggedBulkheadMetrics.meterIdMap.get("backendA")).hasSize(5);
        assertThat(taggedBulkheadMetrics.meterIdMap.get("backendF")).hasSize(5);
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(10);
        assertThat(meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME).tag("key1", "value1")).isNotNull();

    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(5);

        assertThat(taggedBulkheadMetrics.meterIdMap).containsKeys("backendA");
        bulkheadRegistry.remove("backendA");

        assertThat(taggedBulkheadMetrics.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME)
            .gauges();

        Optional<Gauge> successful = findMeterByNamesTag(gauges, bulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(bulkhead.getMetrics().getMaximumThreadPoolSize());

        ThreadPoolBulkhead newBulkhead = ThreadPoolBulkhead
            .of(bulkhead.getName(), ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE + 1)
                .build());

        bulkheadRegistry.replace(bulkhead.getName(), newBulkhead);

        gauges = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME).gauges();

        successful = findMeterByNamesTag(gauges, newBulkhead.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newBulkhead.getMetrics().getMaximumThreadPoolSize());
    }


    @Test
    public void maxThreadPoolSizeGaugeIsRegistered() {
        Gauge available = meterRegistry.get(DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME).gauge();

        assertThat(available).isNotNull();
        assertThat(available.value()).isEqualTo(bulkhead.getMetrics().getMaximumThreadPoolSize());
    }

    @Test
    public void coreThreadPoolSizeGaugeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getCoreThreadPoolSize());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void queueCapacityGaugeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getQueueCapacity());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void queueDepthGaugeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getQueueDepth());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void threadPoolSizeIsRegistered() {
        Gauge maxAllowed = meterRegistry.get(DEFAULT_THREAD_POOL_SIZE_METRIC_NAME).gauge();

        assertThat(maxAllowed).isNotNull();
        assertThat(maxAllowed.value()).isEqualTo(bulkhead.getMetrics().getThreadPoolSize());
        assertThat(maxAllowed.getId().getTag(TagNames.NAME)).isEqualTo(bulkhead.getName());
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ThreadPoolBulkheadRegistry bulkheadRegistry = ThreadPoolBulkheadRegistry.ofDefaults();
        bulkhead = bulkheadRegistry.bulkhead("backendA");
        TaggedThreadPoolBulkheadMetrics.ofThreadPoolBulkheadRegistry(
            ThreadPoolBulkheadMetricNames.custom()
                .maxThreadPoolSizeMetricName("custom.max.thread.pool.size")
                .coreThreadPoolSizeMetricName("custom.core.thread.pool.size")
                .build(),
            bulkheadRegistry
        ).bindTo(meterRegistry);

        Set<String> metricNames = meterRegistry.getMeters()
            .stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Arrays.asList(
            "custom.max.thread.pool.size",
            "custom.core.thread.pool.size",
            "resilience4j.bulkhead.queue.depth",
            "resilience4j.bulkhead.queue.capacity",
            "resilience4j.bulkhead.thread.pool.size"
        ));
    }
}
