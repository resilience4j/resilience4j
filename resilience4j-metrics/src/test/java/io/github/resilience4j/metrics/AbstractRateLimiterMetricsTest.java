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

package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public abstract class AbstractRateLimiterMetricsTest {

    private static final int DEFAULT_LIMIT_FOR_PERIOD = RateLimiterConfig.ofDefaults().getLimitForPeriod();
    private MetricRegistry metricRegistry;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        metricRegistry = new MetricRegistry();
        helloWorldService = mock(HelloWorldService.class);
    }

    protected abstract RateLimiter givenMetricRegistry(String prefix, MetricRegistry metricRegistry);

    protected abstract RateLimiter givenMetricRegistry(MetricRegistry metricRegistry);

    @Test
    public void shouldRegisterMetrics() throws Throwable {
        RateLimiter rateLimiter = givenMetricRegistry(metricRegistry);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String value = rateLimiter.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(2);
        assertThat(metricRegistry.getGauges().get("resilience4j.ratelimiter.testLimit.number_of_waiting_threads")
                .getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("resilience4j.ratelimiter.testLimit.available_permissions").getValue())
                .isIn(DEFAULT_LIMIT_FOR_PERIOD, DEFAULT_LIMIT_FOR_PERIOD - 1);
    }

    @Test
    public void shouldUseCustomPrefix() throws Throwable {
        RateLimiter rateLimiter = givenMetricRegistry("testPre", metricRegistry);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String value = rateLimiter.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(2);
        assertThat(metricRegistry.getGauges().get("testPre.testLimit.number_of_waiting_threads")
                .getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("testPre.testLimit.available_permissions").getValue())
                .isIn(DEFAULT_LIMIT_FOR_PERIOD, DEFAULT_LIMIT_FOR_PERIOD - 1);
    }
}
