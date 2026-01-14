/*
 * Copyright 2026 Bobae Kim
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
package io.github.resilience4j.spring6.httpservice;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.spring6.httpservice.test.TestHttpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * Tests the integration of the {@link Resilience4jHttpService} with fallback.
 */
@WireMockTest
class Resilience4jHttpServiceFallbackTest {

    private HttpServiceProxyFactory factory;
    private TestHttpService testService;
    private TestHttpService testServiceFallback;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        testServiceFallback = mock(TestHttpService.class);
        when(testServiceFallback.greeting()).thenReturn("fallback");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback(testServiceFallback)
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
    }

    @Test
    void testSuccessful() {
        givenResponse(200);

        String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testInvalidFallback() {
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback("not a fallback")
                .build();

        assertThatThrownBy(() ->
                Resilience4jHttpService.builder(decorators)
                        .factory(factory)
                        .build(TestHttpService.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot use the fallback");
    }

    @Test
    void testFallback() {
        givenResponse(500);

        String result = testService.greeting();

        assertThat(result).describedAs("Result")
                .isNotEqualTo("Hello, world!")
                .isEqualTo("fallback");
        verify(testServiceFallback, times(1)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFallbackExceptionFilter() {
        TestHttpService testServiceExceptionFallback = mock(TestHttpService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback(testServiceExceptionFallback, HttpServerErrorException.class)
                .withFallback(testServiceFallback)
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
        givenResponse(500);

        String result = testService.greeting();

        assertThat(result).describedAs("Result")
                .isNotEqualTo("Hello, world!")
                .isEqualTo("exception fallback");
        verify(testServiceFallback, times(0)).greeting();
        verify(testServiceExceptionFallback, times(1)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFallbackExceptionFilterNotCalled() {
        TestHttpService testServiceExceptionFallback = mock(TestHttpService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback(testServiceExceptionFallback, CallNotPermittedException.class)
                .withFallback(testServiceFallback)
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
        givenResponse(500);

        String result = testService.greeting();

        assertThat(result).describedAs("Result")
                .isNotEqualTo("Hello, world!")
                .isEqualTo("fallback");
        verify(testServiceFallback, times(1)).greeting();
        verify(testServiceExceptionFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFallbackFilter() {
        TestHttpService testServiceFilterFallback = mock(TestHttpService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback(testServiceFilterFallback, ex -> true)
                .withFallback(testServiceFallback)
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
        givenResponse(500);

        String result = testService.greeting();

        assertThat(result).describedAs("Result")
                .isNotEqualTo("Hello, world!")
                .isEqualTo("filter fallback");
        verify(testServiceFallback, times(0)).greeting();
        verify(testServiceFilterFallback, times(1)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFallbackFilterNotCalled() {
        TestHttpService testServiceFilterFallback = mock(TestHttpService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback(testServiceFilterFallback, ex -> false)
                .withFallback(testServiceFallback)
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
        givenResponse(500);

        String result = testService.greeting();

        assertThat(result).describedAs("Result")
                .isNotEqualTo("Hello, world!")
                .isEqualTo("fallback");
        verify(testServiceFallback, times(1)).greeting();
        verify(testServiceFilterFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testRevertFallback() {
        // First call fails, second succeeds
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("revert")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello, world!"))
                .willSetStateTo("second"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("revert")
                .whenScenarioStateIs("second")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello, world!")));

        testService.greeting();
        String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback, times(1)).greeting();
        verify(2, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFallbackWithCircuitBreaker() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(circuitBreaker)
                .withFallback(testServiceFallback)
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        givenResponse(500);

        String result = testService.greeting();

        assertThat(result).isEqualTo("fallback");
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    void testFallbackWithParameters() {
        when(testServiceFallback.greetingWithName(anyString())).thenReturn("fallback greeting");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback(testServiceFallback)
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        stubFor(get(urlPathMatching("/api/greeting/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello, world!")));

        String result = testService.greetingWithName("John");

        assertThat(result).isEqualTo("fallback greeting");
        verify(testServiceFallback).greetingWithName("John");
    }

    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello, world!")));
    }
}
