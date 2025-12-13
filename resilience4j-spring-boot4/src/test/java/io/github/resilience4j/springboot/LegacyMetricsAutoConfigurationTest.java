/*
 * Copyright 2025 Ingyu Hwang, Artur Havliukovskyi
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

package io.github.resilience4j.springboot;

import io.github.resilience4j.core.metrics.MetricsPublisher;
import io.github.resilience4j.micrometer.tagged.*;
import io.github.resilience4j.springboot.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestApplication.class,
    properties = {
        "resilience4j.bulkhead.metrics.legacy.enabled=true",
        "resilience4j.thread-pool-bulkhead.metrics.legacy.enabled=true",
        "resilience4j.circuitbreaker.metrics.legacy.enabled=true",
        "resilience4j.ratelimiter.metrics.legacy.enabled=true",
        "resilience4j.retry.metrics.legacy.enabled=true",
        "resilience4j.timelimiter.metrics.legacy.enabled=true"
    })
public class LegacyMetricsAutoConfigurationTest {

    @Autowired(required = false)
    List<MetricsPublisher<?>> metricsPublishers = new ArrayList<>();

    @Autowired(required = false)
    TaggedCircuitBreakerMetrics taggedCircuitBreakerMetrics;

    @Autowired(required = false)
    TaggedBulkheadMetrics taggedBulkheadMetrics;

    @Autowired(required = false)
    TaggedThreadPoolBulkheadMetrics taggedThreadPoolBulkheadMetrics;

    @Autowired(required = false)
    TaggedRateLimiterMetrics taggedRateLimiterMetrics;

    @Autowired(required = false)
    TaggedRetryMetrics taggedRetryMetrics;

    @Autowired(required = false)
    TaggedTimeLimiterMetrics taggedTimeLimiterMetrics;

    @Test
    public void newMetricsPublisherIsNotBound() {
        assertThat(metricsPublishers).isEmpty();
    }

    @Test
    public void legacyCircuitBreakerBinderIsBound() {
        assertThat(taggedCircuitBreakerMetrics).isNotNull();
    }

    @Test
    public void legacyBulkheadBinderIsBound() {
        assertThat(taggedBulkheadMetrics).isNotNull();
    }

    @Test
    public void legacyThreadPoolBulkheadBinderIsBound() {
        assertThat(taggedThreadPoolBulkheadMetrics).isNotNull();
    }

    @Test
    public void legacyRateLimiterBinderIsBound() {
        assertThat(taggedRateLimiterMetrics).isNotNull();
    }

    @Test
    public void legacyRetryBinderIsBound() {
        assertThat(taggedRetryMetrics).isNotNull();
    }

    @Test
    public void legacyTimeLimiterBinderIsBound() {
        assertThat(taggedTimeLimiterMetrics).isNotNull();
    }

}
