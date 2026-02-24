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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.spring6.httpservice.test.TestHttpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the integration of the {@link Resilience4jHttpService} with {@link Retry}
 */
@WireMockTest
class Resilience4jHttpServiceRetryTest {

    private HttpServiceProxyFactory factory;
    private TestHttpService testService;
    private Retry retry;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        retry = Retry.ofDefaults("test");
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRetry(retry)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
    }

    @Test
    void testSuccessfulCall() {
        givenResponse(200);

        String result = testService.greeting();

        assertThat(result).isEqualTo("hello world");
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testSuccessfulCallWithDefaultMethod() {
        givenResponse(200);

        String result = testService.defaultGreeting();

        assertThat(result).isEqualTo("hello world");
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFailedHttpCall() {
        givenResponse(500);

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void testFailedHttpCallWithRetry() {
        givenResponse(500);

        retry = Retry.of("test", RetryConfig.custom()
                .retryExceptions(HttpServerErrorException.class)
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(50))
                .build());
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRetry(retry)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(HttpServerErrorException.class);

        verify(3, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testSuccessAfterRetry() {
        retry = Retry.of("test", RetryConfig.custom()
                .retryExceptions(HttpServerErrorException.class)
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(50))
                .build());
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRetry(retry)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        // First two calls fail, third succeeds using scenario
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("retry")
                .whenScenarioStateIs("third")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("success after retry")));

        String result = testService.greeting();

        assertThat(result).isEqualTo("success after retry");
        verify(3, getRequestedFor(urlPathEqualTo("/api/greeting")));
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    void testRetryOnResult() {
        retry = Retry.of("test", RetryConfig.<String>custom()
                .retryOnResult(s -> s.equalsIgnoreCase("retry me"))
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(50))
                .build());
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRetry(retry)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        // First two return "retry me", third returns success
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("retryOnResult")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("retry me"))
                .willSetStateTo("second"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("retryOnResult")
                .whenScenarioStateIs("second")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("retry me"))
                .willSetStateTo("third"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("retryOnResult")
                .whenScenarioStateIs("third")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("success")));

        String result = testService.greeting();

        assertThat(result).isEqualTo("success");
        verify(3, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testRetryMetrics() {
        retry = Retry.of("test", RetryConfig.custom()
                .retryExceptions(HttpServerErrorException.class)
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(50))
                .build());
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRetry(retry)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        // First call fails, second succeeds
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("metrics")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("metrics")
                .whenScenarioStateIs("second")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("success")));

        testService.greeting();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isZero();
    }

    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
