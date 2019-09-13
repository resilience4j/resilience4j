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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import javax.xml.ws.WebServiceException;

import static io.github.resilience4j.retry.utils.MetricNames.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public abstract class AbstractRetryMetricsTest {

    private MetricRegistry metricRegistry;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        metricRegistry = new MetricRegistry();
        helloWorldService = mock(HelloWorldService.class);
    }

    protected abstract Retry given(String prefix, MetricRegistry metricRegistry);

    protected abstract Retry given(MetricRegistry metricRegistry);

    @Test
    public void shouldRegisterMetricsWithoutRetry() throws Throwable {
        //Given
        Retry retry = given(metricRegistry);

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // Setup circuitbreaker with retry
        String value = retry.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }

    @Test
    public void shouldRegisterMetricsWithRetry() throws Throwable {
        //Given
        Retry retry = given(metricRegistry);

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"))
                .willReturn("Hello world")
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"));

        // Setup circuitbreaker with retry
        String value1 = retry.executeSupplier(helloWorldService::returnHelloWorld);
        Try.ofSupplier(Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld));

        //Then
        assertThat(value1).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(5)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }

    @Test
    public void shouldUseCustomPrefix() throws Throwable {
        //Given
        Retry retry = given("testPrefix", metricRegistry);

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String value = retry.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }
}
