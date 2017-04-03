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

import com.codahale.metrics.MetricRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.test.HelloWorldService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CircuitBreakerMetricsTest {

    private MetricRegistry metricRegistry;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        metricRegistry = new MetricRegistry();
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldRegisterMetrics() throws Throwable {
        //Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        metricRegistry.registerAll(CircuitBreakerMetrics.of(circuitBreakerRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        //When
        String value = circuitBreaker.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(5);
        assertThat(metricRegistry.getGauges().get("io.github.resilience4j.circuitbreaker.CircuitBreaker.testName.buffered").getValue()).isEqualTo(1);
        assertThat(metricRegistry.getGauges().get("io.github.resilience4j.circuitbreaker.CircuitBreaker.testName.successful").getValue()).isEqualTo(1);
        assertThat(metricRegistry.getGauges().get("io.github.resilience4j.circuitbreaker.CircuitBreaker.testName.failed").getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("io.github.resilience4j.circuitbreaker.CircuitBreaker.testName.not_permitted").getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("io.github.resilience4j.circuitbreaker.CircuitBreaker.testName.buffered_max").getValue()).isEqualTo(100);

    }
}
