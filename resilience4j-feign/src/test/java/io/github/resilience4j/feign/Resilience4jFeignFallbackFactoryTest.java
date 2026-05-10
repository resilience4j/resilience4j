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
import io.github.resilience4j.feign.test.TestServiceFallbackThrowingException;
import io.github.resilience4j.feign.test.TestServiceFallbackWithException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests on fallback factories.
 */
@WireMockTest
class Resilience4jFeignFallbackFactoryTest {

    private String baseUrl;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        baseUrl = wmRuntimeInfo.getHttpBaseUrl() + "/";
    }

    private TestService buildTestService(Function<Exception, ?> fallbackSupplier) {
        FeignDecorators decorators = FeignDecorators.builder()
            .withFallbackFactory(fallbackSupplier)
            .build();
        return Feign.builder()
            .addCapability(Resilience4jFeign.capability(decorators))
            .target(TestService.class, baseUrl);
    }

    private static void setupStub(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("Hello, world!")));
    }

    @Test
    void successfullyGetsResponse() {
        setupStub(200);
        TestService testService = buildTestService(e -> "my fallback");

        String result = testService.greeting();

        assertThat(result).isEqualTo("Hello, world!");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void lazilyFailsOnInvalidFallback() {
        TestService testService = buildTestService(e -> "my fallback");

        Throwable throwable = catchThrowable(testService::greeting);

        assertThat(throwable).isNotNull()
            .hasMessageContaining(
                "Cannot use the fallback [class java.lang.String] for [interface io.github.resilience4j.feign.test.TestService]");
    }

    @Test
    void goesToFallbackAndConsumesException() {
        setupStub(400);
        TestService testService = buildTestService(TestServiceFallbackWithException::new);

        String result = testService.greeting();

        assertThat(result)
            .startsWith("Message from exception: [400 Bad Request]");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void goesToFallbackAndRethrowsExceptionThrownInFallback() {
        setupStub(400);
        TestService testService = buildTestService(e -> new TestServiceFallbackThrowingException());

        Throwable result = catchThrowable(testService::greeting);

        assertThat(result)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Exception in greeting fallback");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void goesToFallbackAndConsumesExceptionWithExceptionFilter() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        FeignDecorators decorators = FeignDecorators.builder()
            .withFallbackFactory(TestServiceFallbackWithException::new, FeignException.class)
            .withFallbackFactory(e -> uselessFallback)
            .build();
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, baseUrl);

        String result = testService.greeting();

        assertThat(result)
            .startsWith("Message from exception: [400 Bad Request]");
        verify(uselessFallback, never()).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void goesToSecondFallbackAndConsumesExceptionWithExceptionFilter() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        FeignDecorators decorators = FeignDecorators.builder()
            .withFallbackFactory(e -> uselessFallback, MyException.class)
            .withFallbackFactory(TestServiceFallbackWithException::new)
            .build();
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, baseUrl);

        String result = testService.greeting();

        assertThat(result)
            .startsWith("Message from exception: [400 Bad Request]");
        verify(uselessFallback, never()).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void goesToFallbackAndConsumesExceptionWithPredicate() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        FeignDecorators decorators = FeignDecorators.builder()
            .withFallbackFactory(TestServiceFallbackWithException::new,
                FeignException.class::isInstance)
            .withFallbackFactory(e -> uselessFallback)
            .build();
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, baseUrl);

        String result = testService.greeting();

        assertThat(result)
            .startsWith("Message from exception: [400 Bad Request]");
        verify(uselessFallback, never()).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void goesToSecondFallbackAndConsumesExceptionWithPredicate() {
        setupStub(400);
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        FeignDecorators decorators = FeignDecorators.builder()
            .withFallbackFactory(e -> uselessFallback, MyException.class::isInstance)
            .withFallbackFactory(TestServiceFallbackWithException::new)
            .build();
        TestService testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, baseUrl);

        String result = testService.greeting();

        assertThat(result)
            .startsWith("Message from exception: [400 Bad Request]");
        verify(uselessFallback, never()).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    private static class MyException extends Exception {

    }
}
