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
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.feign.test.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the integration of the {@link Resilience4jFeign} with a fallback.
 */
@WireMockTest
class Resilience4jFeignFallbackTest {

    private String baseUrl;
    private TestService testService;
    private TestService testServiceFallback;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        baseUrl = wmRuntimeInfo.getHttpBaseUrl() + "/";

        testServiceFallback = mock(TestService.class);
        when(testServiceFallback.greeting()).thenReturn("fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(testServiceFallback)
            .build();

        testService = Feign.builder()
            .addCapability(Resilience4jFeign.capability(decorators))
            .target(TestService.class, baseUrl);
    }

    @Test
    void successful() throws Exception {
        setupStub(200);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback, never()).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void invalidFallback() {
        final FeignDecorators decorators = FeignDecorators.builder().withFallback("not a fallback")
            .build();
        assertThatThrownBy(() -> Resilience4jFeign.builder(decorators).target(TestService.class, baseUrl))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fallback() throws Exception {
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(testServiceFallback).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }


    @Test
    void fallbackExceptionFilter() throws Exception {
        final TestService testServiceExceptionFallback = mock(TestService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(testServiceExceptionFallback, FeignException.class)
            .withFallback(testServiceFallback)
            .build();

        testService = Resilience4jFeign.builder(decorators).target(TestService.class, baseUrl);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("exception fallback");
        verify(testServiceFallback, never()).greeting();
        verify(testServiceExceptionFallback).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void fallbackExceptionFilterNotCalled() throws Exception {
        final TestService testServiceExceptionFallback = mock(TestService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(testServiceExceptionFallback, CallNotPermittedException.class)
            .withFallback(testServiceFallback)
            .build();

        testService = Resilience4jFeign.builder(decorators).target(TestService.class, baseUrl);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(testServiceFallback).greeting();
        verify(testServiceExceptionFallback, never()).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void fallbackFilter() throws Exception {
        final TestService testServiceFilterFallback = mock(TestService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(testServiceFilterFallback, ex -> true)
            .withFallback(testServiceFallback)
            .build();

        testService = Resilience4jFeign.builder(decorators).target(TestService.class, baseUrl);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("filter fallback");
        verify(testServiceFallback, never()).greeting();
        verify(testServiceFilterFallback).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void fallbackFilterNotCalled() throws Exception {
        final TestService testServiceFilterFallback = mock(TestService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(testServiceFilterFallback, ex -> false)
            .withFallback(testServiceFallback)
            .build();

        testService = Resilience4jFeign.builder(decorators).target(TestService.class, baseUrl);
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(testServiceFallback).greeting();
        verify(testServiceFilterFallback, never()).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void revertFallback() throws Exception {
        setupStub(400);

        testService.greeting();
        setupStub(200);
        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback).greeting();
        verify(2, getRequestedFor(urlPathEqualTo("/greeting")));

    }

    private void setupStub(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("Hello, world!")));
    }
}
