/*
 *
 *  Copyright 2017: Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

public class RateLimiterMetricsTest {

    private static final int DEFAULT_LIMIT_FOR_PERIOD = RateLimiterConfig.ofDefaults().getLimitForPeriod();
    private MetricRegistry metricRegistry;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        metricRegistry = new MetricRegistry();
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldRegisterMetrics() throws Throwable {
        //Given
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testLimit");
        metricRegistry.registerAll(RateLimiterMetrics.ofRateLimiterRegistry(rateLimiterRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        //When
        String value = rateLimiter.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(2);
        assertThat(metricRegistry.getGauges().get("resilience4j.ratelimiter.testLimit.number_of_waiting_threads")
            .getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("resilience4j.ratelimiter.testLimit.available_permissions").getValue())
            .isIn(DEFAULT_LIMIT_FOR_PERIOD, DEFAULT_LIMIT_FOR_PERIOD - 1);
    }

    @Test
    public void shouldUseCustomPrefix() throws Throwable {
        //Given
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("testLimit");
        metricRegistry.registerAll(RateLimiterMetrics.ofIterable("testPre", rateLimiterRegistry.getAllRateLimiters()));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        //When
        String value = rateLimiter.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(2);
        assertThat(metricRegistry.getGauges().get("testPre.testLimit.number_of_waiting_threads")
            .getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("testPre.testLimit.available_permissions").getValue())
            .isIn(DEFAULT_LIMIT_FOR_PERIOD, DEFAULT_LIMIT_FOR_PERIOD - 1);
    }
}
