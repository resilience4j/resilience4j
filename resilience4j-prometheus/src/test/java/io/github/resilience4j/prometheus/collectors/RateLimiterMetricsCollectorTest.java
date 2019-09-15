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

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import static io.github.resilience4j.prometheus.collectors.RateLimiterMetricsCollector.MetricNames.DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME;
import static io.github.resilience4j.prometheus.collectors.RateLimiterMetricsCollector.MetricNames.DEFAULT_WAITING_THREADS_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class RateLimiterMetricsCollectorTest {

    CollectorRegistry registry;
    RateLimiter rateLimiter;
    RateLimiterRegistry rateLimiterRegistry;

    @Before
    public void setup() {
        registry = new CollectorRegistry();
        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        rateLimiter = rateLimiterRegistry.rateLimiter("backendA");

        RateLimiterMetricsCollector.ofRateLimiterRegistry(rateLimiterRegistry).register(registry);
    }

    @Test
    public void availablePermissionsReportsCorrespondingValue() {
        double availablePermissions = registry.getSampleValue(
            DEFAULT_AVAILABLE_PERMISSIONS_METRIC_NAME,
            new String[]{"name"},
            new String[]{rateLimiter.getName()}
        );

        assertThat(availablePermissions).isEqualTo(rateLimiter.getMetrics().getAvailablePermissions());
    }

    @Test
    public void waitingThreadsReportsCorrespondingValue() {
        double waitingThreads = registry.getSampleValue(
            DEFAULT_WAITING_THREADS_METRIC_NAME,
            new String[]{"name"},
            new String[]{rateLimiter.getName()}
        );

        assertThat(waitingThreads).isEqualTo(rateLimiter.getMetrics().getNumberOfWaitingThreads());
    }

    @Test
    public void customMetricNamesOverrideDefaultOnes() {
        CollectorRegistry registry = new CollectorRegistry();

        RateLimiterMetricsCollector.ofRateLimiterRegistry(
            RateLimiterMetricsCollector.MetricNames.custom()
                .availablePermissionsMetricName("custom_available_permissions")
                .waitingThreadsMetricName("custom_waiting_threads")
                .build(),
                rateLimiterRegistry).register(registry);

        assertThat(registry.getSampleValue(
            "custom_available_permissions",
            new String[]{"name"},
            new String[]{"backendA"}
        )).isNotNull();
        assertThat(registry.getSampleValue(
            "custom_waiting_threads",
            new String[]{"name"},
            new String[]{"backendA"}
        )).isNotNull();
    }
}
