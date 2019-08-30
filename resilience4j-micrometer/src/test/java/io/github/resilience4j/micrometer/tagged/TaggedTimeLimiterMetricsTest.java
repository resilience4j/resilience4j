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

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findGaugeByNamesTag;
import static io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetrics.MetricNames.DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
import static io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetrics.MetricNames.DEFAULT_WAITING_THREADS_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedTimeLimiterMetricsTest {

    private MeterRegistry meterRegistry;
    private TimeLimiter timeLimiter;
    private TimeLimiterRegistry timeLimiterRegistry;
    private TaggedTimeLimiterMetrics taggedTimeLimiterMetrics;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

        timeLimiter = timeLimiterRegistry.timeLimiter("backendA");
        taggedTimeLimiterMetrics = TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(timeLimiterRegistry);
        taggedTimeLimiterMetrics.bindTo(meterRegistry);
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedTimeLimiter() {
        TimeLimiter newTimeLimiter = timeLimiterRegistry.timeLimiter("backendB");

        assertThat(taggedTimeLimiterMetrics.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedTimeLimiterMetrics.meterIdMap.get("backendA")).hasSize(2);
        assertThat(taggedTimeLimiterMetrics.meterIdMap.get("backendB")).hasSize(2);

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(4);

        Collection<Gauge> gauges = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME).gauges();

        Optional<Gauge> successful = findGaugeByNamesTag(gauges, newTimeLimiter.getName());
        assertThat(successful).isPresent();
        assertThat(successful.get().value()).isEqualTo(newTimeLimiter.getMetrics().getAvailablePermissions());
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(2);

        assertThat(taggedTimeLimiterMetrics.meterIdMap).containsKeys("backendA");
        timeLimiterRegistry.remove("backendA");

        assertThat(taggedTimeLimiterMetrics.meterIdMap).isEmpty();

        meters = meterRegistry.getMeters();
        assertThat(meters).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Gauge availablePermissions = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME).gauge();

        assertThat(availablePermissions).isNotNull();
        assertThat(availablePermissions.value()).isEqualTo(timeLimiter.getMetrics().getAvailablePermissions());
        assertThat(availablePermissions.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());

        TimeLimiter newTimeLimiter = TimeLimiter.of(timeLimiter.getName(), TimeLimiterConfig.custom().limitForPeriod(1000).build());

        timeLimiterRegistry.replace(timeLimiter.getName(), newTimeLimiter);

        availablePermissions = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME).gauge();

        assertThat(availablePermissions).isNotNull();
        assertThat(availablePermissions.value()).isEqualTo(newTimeLimiter.getMetrics().getAvailablePermissions());
        assertThat(availablePermissions.getId().getTag(TagNames.NAME)).isEqualTo(newTimeLimiter.getName());
    }

    @Test
    public void availablePermissionsGaugeIsRegistered() {
        Gauge availablePermissions = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME).gauge();

        assertThat(availablePermissions).isNotNull();
        assertThat(availablePermissions.value()).isEqualTo(timeLimiter.getMetrics().getAvailablePermissions());
        assertThat(availablePermissions.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());
    }

    @Test
    public void waitingThreadsGaugeIsRegistered() {
        Gauge waitingThreads = meterRegistry.get(DEFAULT_WAITING_THREADS_METRIC_NAME).gauge();

        assertThat(waitingThreads).isNotNull();
        assertThat(waitingThreads.value()).isEqualTo(timeLimiter.getMetrics().getNumberOfWaitingThreads());
        assertThat(waitingThreads.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiterRegistry.timeLimiter("backendA");
        TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(
                TaggedTimeLimiterMetrics.MetricNames.custom()
                        .availablePermissionsMetricName("custom_available_permissions")
                        .waitingThreadsMetricName("custom_waiting_threads")
                        .build(),
                timeLimiterRegistry
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
