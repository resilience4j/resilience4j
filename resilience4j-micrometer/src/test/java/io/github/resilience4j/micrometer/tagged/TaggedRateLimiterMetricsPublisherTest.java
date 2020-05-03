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

import static io.github.resilience4j.micrometer.tagged.RateLimiterMetricNames.DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
import static io.github.resilience4j.micrometer.tagged.RateLimiterMetricNames.DEFAULT_WAITING_THREADS_METRIC_NAME;
import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByNamesTag;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedRateLimiterMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private RateLimiter rateLimiter;
    private RateLimiterRegistry rateLimiterRegistry;
    private TaggedRateLimiterMetricsPublisher taggedRateLimiterMetricsPublisher;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        taggedRateLimiterMetricsPublisher = new TaggedRateLimiterMetricsPublisher(meterRegistry);
        rateLimiterRegistry = RateLimiterRegistry
            .of(RateLimiterConfig.ofDefaults(), taggedRateLimiterMetricsPublisher);

        rateLimiter = rateLimiterRegistry.rateLimiter("backendA");
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedRateLimiter() {
        RateLimiter newRateLimiter = rateLimiterRegistry.rateLimiter("backendB");

        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap)
            .containsKeys("backendA", "backendB");
        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap.get("backendA")).hasSize(2);
        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap.get("backendB")).hasSize(2);

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
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(2);

        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap).containsKeys("backendA");
        rateLimiterRegistry.remove("backendA");

        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap).isEmpty();

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
        TaggedRateLimiterMetricsPublisher taggedRateLimiterMetricsPublisher = new TaggedRateLimiterMetricsPublisher(
            RateLimiterMetricNames.custom()
                .availablePermissionsMetricName("custom_available_permissions")
                .waitingThreadsMetricName("custom_waiting_threads")
                .build(), meterRegistry);

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry
            .of(RateLimiterConfig.ofDefaults(), taggedRateLimiterMetricsPublisher);
        rateLimiterRegistry.rateLimiter("backendA");

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

    @Test
    public void testReplaceNewMeter() {
        RateLimiter oldOne = RateLimiter.of("backendC", RateLimiterConfig.ofDefaults());
        // add meters of old
        taggedRateLimiterMetricsPublisher.addMetrics(meterRegistry, oldOne);
        // one permission class
        oldOne.acquirePermission();

        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap.get("backendC")).hasSize(2);
        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME)
            .gauges();
        Optional<Gauge> available = findMeterByNamesTag(gauges, oldOne.getName());
        assertThat(available).isPresent();
        assertThat(available.get().value())
            .isEqualTo(oldOne.getMetrics().getAvailablePermissions());

        RateLimiter newOne = RateLimiter.of("backendC", RateLimiterConfig.ofDefaults());

        // add meters of old
        taggedRateLimiterMetricsPublisher.addMetrics(meterRegistry, newOne);
        // three permission call
        newOne.acquirePermission(3);

        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedRateLimiterMetricsPublisher.meterIdMap.get("backendC")).hasSize(2);
        gauges = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME)
            .gauges();
        available = findMeterByNamesTag(gauges, newOne.getName());
        assertThat(available).isPresent();
        assertThat(available.get().value())
            .isEqualTo(newOne.getMetrics().getAvailablePermissions());
    }
}