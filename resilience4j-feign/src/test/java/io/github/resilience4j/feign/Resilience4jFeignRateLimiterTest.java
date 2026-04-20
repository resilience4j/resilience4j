/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import feign.Feign;
import feign.FeignException;
import io.github.resilience4j.feign.test.TestService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the integration of the {@link Resilience4jFeign} with {@link RateLimiter}
 */
@WireMockTest
class Resilience4jFeignRateLimiterTest {

    private String baseUrl;
    private TestService testService;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        baseUrl = wmRuntimeInfo.getHttpBaseUrl() + "/";
        rateLimiter = mock(RateLimiter.class);
        final FeignDecorators decorators = FeignDecorators.builder()
            .withRateLimiter(rateLimiter)
            .build();
        testService = Feign.builder()
            .addCapability(Resilience4jFeign.capability(decorators))
            .target(TestService.class, baseUrl);
    }

    @Test
    void successfulCall() {
        givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(rateLimiter).acquirePermission(anyInt());
    }

    @Test
    void successfulCallWithDefaultMethod() {
        givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        testService.defaultGreeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(rateLimiter).acquirePermission(anyInt());
    }

    @Test
    void rateLimiterLimiting() {
        givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(false);
        when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.ofDefaults());

        assertThatThrownBy(() -> testService.greeting())
            .isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void failedHttpCall() {
        givenResponse(400);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        assertThatThrownBy(() -> testService.greeting())
            .isInstanceOf(FeignException.class);
    }

    @Test
    void rateLimiterCreateByStaticMethod() {
        testService = TestService.create(baseUrl, rateLimiter);
        givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(false);
        when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.ofDefaults());

        assertThatThrownBy(() -> testService.greeting())
            .isInstanceOf(RequestNotPermitted.class);
    }


    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));
    }
}
