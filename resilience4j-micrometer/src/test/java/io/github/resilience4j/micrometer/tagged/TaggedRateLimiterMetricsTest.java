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

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics.MetricNames.DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
import static io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics.MetricNames.DEFAULT_WAITING_THREADS_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedRateLimiterMetricsTest {

    private MeterRegistry meterRegistry;
    private RateLimiter rateLimiter;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        rateLimiter = rateLimiterRegistry.rateLimiter("backendA");
        TaggedRateLimiterMetrics.ofRateLimiterRegistry(rateLimiterRegistry).bindTo(meterRegistry);
    }

    @Test
    public void availablePermissionsGaugeIsRegistered() {
        Gauge availablePermissions = meterRegistry.get(DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME).gauge();

        assertThat(availablePermissions).isNotNull();
        assertThat(availablePermissions.value()).isEqualTo(rateLimiter.getMetrics().getAvailablePermissions());
        assertThat(availablePermissions.getId().getTag(TagNames.NAME)).isEqualTo(rateLimiter.getName());
    }

    @Test
    public void waitingThreadsGaugeIsRegistered() {
        Gauge waitingThreads = meterRegistry.get(DEFAULT_WAITING_THREADS_METRIC_NAME).gauge();

        assertThat(waitingThreads).isNotNull();
        assertThat(waitingThreads.value()).isEqualTo(rateLimiter.getMetrics().getNumberOfWaitingThreads());
        assertThat(waitingThreads.getId().getTag(TagNames.NAME)).isEqualTo(rateLimiter.getName());
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        rateLimiterRegistry.rateLimiter("backendA");
        TaggedRateLimiterMetrics.ofRateLimiterRegistry(
                TaggedRateLimiterMetrics.MetricNames.custom()
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
