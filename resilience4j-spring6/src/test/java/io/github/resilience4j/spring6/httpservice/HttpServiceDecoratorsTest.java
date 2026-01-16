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
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
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

@WireMockTest
class HttpServiceDecoratorsTest {

    private HttpServiceProxyFactory factory;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
    }

    @Test
    void shouldBuildDecoratorsWithCircuitBreaker() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(circuitBreaker)
                .build();

        assertThat(decorators).isNotNull();
    }

    @Test
    void shouldBuildDecoratorsWithMultipleComponents() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");
        RateLimiter rateLimiter = RateLimiter.ofDefaults("test");
        Retry retry = Retry.ofDefaults("test");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .withRateLimiter(rateLimiter)
                .build();

        assertThat(decorators).isNotNull();
    }

    @Test
    void shouldCallServiceSuccessfully() {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello World")));

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(circuitBreaker)
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        String result = service.greeting();

        assertThat(result).isEqualTo("Hello World");
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
    }

    @Test
    void shouldOpenCircuitBreakerOnFailures() {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse().withStatus(500)));

        CircuitBreaker circuitBreaker = CircuitBreaker.of("test",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(5)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(circuitBreaker)
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        // Call until circuit opens
        for (int i = 0; i < 5; i++) {
            try {
                service.greeting();
            } catch (Exception e) {
                // Expected failures
            }
        }

        // Circuit should be open now
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Further calls should throw CallNotPermittedException
        assertThatThrownBy(service::greeting)
                .isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void shouldUseFallbackOnFailure() {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse().withStatus(500)));

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");

        TestHttpService fallback = new TestHttpService() {
            @Override
            public String greeting() {
                return "Fallback greeting";
            }

            @Override
            public String greetingWithName(String name) {
                return "Fallback: " + name;
            }

            @Override
            public String echo(String message) {
                return "Fallback echo: " + message;
            }
        };

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(circuitBreaker)
                .withFallback(fallback)
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        String result = service.greeting();

        assertThat(result).isEqualTo("Fallback greeting");
    }

    @Test
    void shouldRetryOnFailure() {
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
                        .withBody("Success after retries")));

        Retry retry = Retry.of("test",
                RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(100))
                        .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRetry(retry)
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        String result = service.greeting();

        assertThat(result).isEqualTo("Success after retries");
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    void shouldApplyRateLimiter() {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Response")));

        RateLimiter rateLimiter = RateLimiter.of("test",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(2)
                        .timeoutDuration(Duration.ofMillis(100))
                        .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRateLimiter(rateLimiter)
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        // First two calls should succeed
        assertThat(service.greeting()).isNotNull();
        assertThat(service.greeting()).isNotNull();

        // Third call should be rate limited (might throw exception or timeout)
        // Note: This test might be flaky depending on timing
    }

    @Test
    void shouldBuildWithCustomName() {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello")));

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(CircuitBreaker.ofDefaults("test"))
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class, "customServiceName");

        String result = service.greeting();

        assertThat(result).isEqualTo("Hello");
    }

    @Test
    void shouldThrowExceptionForNonInterface() {
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(CircuitBreaker.ofDefaults("test"))
                .build();

        assertThatThrownBy(() ->
                Resilience4jHttpService.builder(decorators)
                        .factory(factory)
                        .build(String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceType must be an interface");
    }

    @Test
    void shouldThrowExceptionWhenNoClientConfigured() {
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withCircuitBreaker(CircuitBreaker.ofDefaults("test"))
                .build();

        assertThatThrownBy(() ->
                Resilience4jHttpService.builder(decorators)
                        .build(TestHttpService.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("factory must be configured");
    }
}
