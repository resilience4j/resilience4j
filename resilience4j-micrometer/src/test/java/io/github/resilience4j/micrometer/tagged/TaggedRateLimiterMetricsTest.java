/*
 * Copyright 2019 Yevhenii Voievodin, Mahmoud Romeh
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

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByNamesTag;
import static io.github.resilience4j.micrometer.tagged.RateLimiterMetricNames.DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
import static io.github.resilience4j.micrometer.tagged.RateLimiterMetricNames.DEFAULT_WAITING_THREADS_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedRateLimiterMetricsTest {

    private MeterRegistry meterRegistry;
    private RateLimiter rateLimiter;
    private RateLimiterRegistry rateLimiterRegistry;
    private TaggedRateLimiterMetrics taggedRateLimiterMetrics;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        rateLimiter = rateLimiterRegistry.rateLimiter("backendA");
        taggedRateLimiterMetrics = TaggedRateLimiterMetrics
            .ofRateLimiterRegistry(rateLimiterRegistry);
        taggedRateLimiterMetrics.bindTo(meterRegistry);
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedRateLimiter() {
        RateLimiter newRateLimiter = rateLimiterRegistry.rateLimiter("backendB");

        assertThat(taggedRateLimiterMetrics.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedRateLimiterMetrics.meterIdMap.get("backendA")).hasSize(2);
        assertThat(taggedRateLimiterMetrics.meterIdMap.get("backendB")).hasSize(2);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(4);

        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME)
            .gauges();

        Optional<Gauge> successful = findMeterByNamesTag(gauges, newRateLimiter.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value())
            .isEqualTo(newRateLimiter.getMetrics().getAvailablePermissions());
    }

    @Test
    public void shouldAddCustomTags() {
        RateLimiter newRateLimiter = rateLimiterRegistry.rateLimiter("backendF", Map.of("key1", "value1"));
        assertThat(taggedRateLimiterMetrics.meterIdMap).containsKeys("backendA", "backendF");
        assertThat(taggedRateLimiterMetrics.meterIdMap.get("backendA")).hasSize(2);
        assertThat(taggedRateLimiterMetrics.meterIdMap.get("backendF")).hasSize(2);
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(4);
        assertThat(meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME).tag("key1", "value1")).isNotNull();
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(2);

        assertThat(taggedRateLimiterMetrics.meterIdMap).containsKeys("backendA");
        rateLimiterRegistry.remove("backendA");

        assertThat(taggedRateLimiterMetrics.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Gauge availablePermissions = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME)
            .gauge();

        assertThat(availablePermissions).isNotNull();
        assertThat(availablePermissions.value())
            .isEqualTo(rateLimiter.getMetrics().getAvailablePermissions());
        assertThat(availablePermissions.getId().getTag(TagNames.NAME))
            .isEqualTo(rateLimiter.getName());

        RateLimiter newRateLimiter = RateLimiter
            .of(rateLimiter.getName(), RateLimiterConfig.custom().limitForPeriod(1000).build());

        rateLimiterRegistry.replace(rateLimiter.getName(), newRateLimiter);

        availablePermissions = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME).gauge();

        assertThat(availablePermissions).isNotNull();
        assertThat(availablePermissions.value())
            .isEqualTo(newRateLimiter.getMetrics().getAvailablePermissions());
        assertThat(availablePermissions.getId().getTag(TagNames.NAME))
            .isEqualTo(newRateLimiter.getName());
    }

    @Test
    public void availablePermissionsGaugeIsRegistered() {
        Gauge availablePermissions = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME)
            .gauge();

        assertThat(availablePermissions).isNotNull();
        assertThat(availablePermissions.value())
            .isEqualTo(rateLimiter.getMetrics().getAvailablePermissions());
        assertThat(availablePermissions.getId().getTag(TagNames.NAME))
            .isEqualTo(rateLimiter.getName());
    }

    @Test
    public void waitingThreadsGaugeIsRegistered() {
        Gauge waitingThreads = meterRegistry.get(DEFAULT_WAITING_THREADS_METRIC_NAME).gauge();

        assertThat(waitingThreads).isNotNull();
        assertThat(waitingThreads.value())
            .isEqualTo(rateLimiter.getMetrics().getNumberOfWaitingThreads());
        assertThat(waitingThreads.getId().getTag(TagNames.NAME)).isEqualTo(rateLimiter.getName());
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        rateLimiterRegistry.rateLimiter("backendA");
        TaggedRateLimiterMetrics.ofRateLimiterRegistry(
            RateLimiterMetricNames.custom()
                .availablePermissionsMetricName("custom_available_permissions")
                .waitingThreadsMetricName("custom_waiting_threads")
                .build(),
            rateLimiterRegistry
        ).bindTo(meterRegistry);

        Set<String> metricNames = meterRegistry.getMeters()
            .stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Arrays.asList(
            "custom_available_permissions",
            "custom_waiting_threads"
        ));
    }
}
