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
    public void shouldRegisterMetrics() {
        //Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        //When
        String value = circuitBreaker.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(6);
        assertThat(metricRegistry.getGauges().get("resilience4j.circuitbreaker.testName.state").getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("resilience4j.circuitbreaker.testName.buffered").getValue()).isEqualTo(1);
        assertThat(metricRegistry.getGauges().get("resilience4j.circuitbreaker.testName.successful").getValue()).isEqualTo(1);
        assertThat(metricRegistry.getGauges().get("resilience4j.circuitbreaker.testName.failed").getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("resilience4j.circuitbreaker.testName.not_permitted").getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.circuitbreaker.testName.failure_rate").getValue()).isEqualTo(-1f);
    }

    @Test
    public void shouldUseCustomPrefix() throws Throwable {
        //Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry("testPrefix", circuitBreakerRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        //When
        String value = circuitBreaker.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(6);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName.state").getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName.buffered").getValue()).isEqualTo(1);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName.successful").getValue()).isEqualTo(1);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName.failed").getValue()).isEqualTo(0);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName.not_permitted").getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName.failure_rate").getValue()).isEqualTo(-1f);
    }
}
