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
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.spring6.httpservice.test.TestHttpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * Tests the integration of the {@link Resilience4jHttpService} with {@link RateLimiter}
 */
@WireMockTest
class Resilience4jHttpServiceRateLimiterTest {

    private HttpServiceProxyFactory factory;
    private TestHttpService testService;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        rateLimiter = mock(RateLimiter.class);
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRateLimiter(rateLimiter)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
    }

    @Test
    void testSuccessfulCall() {
        givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
        verify(rateLimiter).acquirePermission(anyInt());
    }

    @Test
    void testSuccessfulCallWithDefaultMethod() {
        givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        testService.defaultGreeting();

        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
        verify(rateLimiter).acquirePermission(anyInt());
    }

    @Test
    void testRateLimiterLimiting() {
        givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(false);
        when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.ofDefaults());

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(RequestNotPermitted.class);

        verify(0, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFailedHttpCall() {
        givenResponse(500);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void testRateLimiterWithRealConfig(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        HttpServiceProxyFactory newFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        RateLimiter realRateLimiter = RateLimiter.of("test",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(2)
                        .timeoutDuration(Duration.ofMillis(100))
                        .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRateLimiter(realRateLimiter)
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(newFactory)
                .build(TestHttpService.class);

        // Stub responses
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Response")));

        // First two calls should succeed
        assertThat(service.greeting()).isNotNull();
        assertThat(service.greeting()).isNotNull();

        // Third call should be rate limited
        assertThatThrownBy(service::greeting)
                .isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void testRateLimiterMetrics(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        HttpServiceProxyFactory newFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        RateLimiter realRateLimiter = RateLimiter.of("test",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(10))
                        .limitForPeriod(5)
                        .timeoutDuration(Duration.ofMillis(100))
                        .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withRateLimiter(realRateLimiter)
                .build();

        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(newFactory)
                .build(TestHttpService.class);

        // Stub responses
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Response")));

        service.greeting();
        service.greeting();
        service.greeting();

        RateLimiter.Metrics metrics = realRateLimiter.getMetrics();
        assertThat(metrics.getAvailablePermissions()).isEqualTo(2);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
