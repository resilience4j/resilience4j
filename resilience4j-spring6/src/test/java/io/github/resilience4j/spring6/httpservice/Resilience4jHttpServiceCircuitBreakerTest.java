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
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.spring6.httpservice.test.TestHttpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the integration of the {@link Resilience4jHttpService} with {@link CircuitBreaker}
 */
@WireMockTest
class Resilience4jHttpServiceCircuitBreakerTest {

    private static final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(3)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();

    private CircuitBreaker circuitBreaker;
    private TestHttpService testService;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(circuitBreaker)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
    }

    @Test
    void testSuccessfulCall() {
        givenResponse(200);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
        assertThat(metrics.getNumberOfSuccessfulCalls())
                .describedAs("Successful Calls")
                .isEqualTo(1);
    }

    @Test
    void testSuccessfulCallWithDefaultMethod() {
        givenResponse(200);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        testService.defaultGreeting();

        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
        assertThat(metrics.getNumberOfSuccessfulCalls())
                .describedAs("Successful Calls")
                .isEqualTo(1);
    }

    @Test
    void testFailedCall() {
        givenResponse(500);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(Exception.class);

        assertThat(metrics.getNumberOfFailedCalls())
                .describedAs("Failed Calls")
                .isEqualTo(1);
    }

    @Test
    void testCircuitBreakerOpen() {
        givenResponse(500);
        int threshold = circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize() + 1;

        for (int i = 0; i < threshold - 1; i++) {
            try {
                testService.greeting();
            } catch (Exception ex) {
                // ignore
            }
        }

        // Circuit should be open now
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Further calls should throw CallNotPermittedException
        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(CallNotPermittedException.class);

        assertThat(circuitBreaker.tryAcquirePermission())
                .describedAs("CircuitBreaker Closed")
                .isFalse();
    }

    @Test
    void testCircuitBreakerClosed() {
        givenResponse(500);
        int threshold = circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize() - 1;

        for (int i = 0; i < threshold; i++) {
            try {
                testService.greeting();
            } catch (Exception ex) {
                // ignore
            }
        }

        assertThat(circuitBreaker.tryAcquirePermission())
                .describedAs("CircuitBreaker Closed")
                .isTrue();
    }

    @Test
    void testCircuitBreakerRecordsMetricsCorrectly() {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        // Use scenario to return different responses sequentially
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("metrics")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("success"))
                .willSetStateTo("second"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("metrics")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third"));

        stubFor(get(urlPathEqualTo("/api/greeting"))
                .inScenario("metrics")
                .whenScenarioStateIs("third")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("success")));

        testService.greeting();
        try {
            testService.greeting();
        } catch (Exception ignored) {
            // ignored
        }
        testService.greeting();

        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
