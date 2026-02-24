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
import io.github.resilience4j.spring6.httpservice.test.TestHttpService;
import io.github.resilience4j.spring6.httpservice.test.TestHttpServiceFallbackThrowingException;
import io.github.resilience4j.spring6.httpservice.test.TestHttpServiceFallbackWithException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

/**
 * Unit tests on fallback factories.
 */
@WireMockTest
class Resilience4jHttpServiceFallbackFactoryTest {

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

    private TestHttpService buildTestService(Function<Exception, ?> fallbackSupplier) {
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallbackFactory(fallbackSupplier)
                .build();
        return Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
    }

    @Test
    void should_successfully_get_a_response() {
        givenResponse(200);
        TestHttpService testService = buildTestService(e -> mock(TestHttpService.class));

        String result = testService.greeting();

        assertThat(result).isEqualTo("Hello, world!");
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void should_lazily_fail_on_invalid_fallback() {
        givenResponse(500);
        TestHttpService testService = buildTestService(e -> "my fallback");

        Throwable throwable = catchThrowable(testService::greeting);

        assertThat(throwable).isNotNull()
                .hasMessageContaining(
                        "Cannot use the fallback [class java.lang.String] for [interface io.github.resilience4j.spring6.httpservice.test.TestHttpService]");
    }

    @Test
    void should_go_to_fallback_and_consume_exception() {
        givenResponse(500);
        TestHttpService testService = buildTestService(TestHttpServiceFallbackWithException::new);

        String result = testService.greeting();

        assertThat(result)
                .startsWith("Message from exception:");
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void should_go_to_fallback_and_rethrow_an_exception_thrown_in_fallback() {
        givenResponse(500);
        TestHttpService testService = buildTestService(e -> new TestHttpServiceFallbackThrowingException());

        Throwable result = catchThrowable(testService::greeting);

        assertThat(result).isNotNull()
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Exception in greeting fallback");
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void should_go_to_fallback_and_consume_exception_with_exception_filter() {
        givenResponse(500);
        TestHttpService uselessFallback = spy(TestHttpService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallbackFactory(TestHttpServiceFallbackWithException::new, HttpServerErrorException.class)
                .withFallbackFactory(e -> uselessFallback)
                .build();
        TestHttpService testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        String result = testService.greeting();

        assertThat(result)
                .startsWith("Message from exception:");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void should_go_to_second_fallback_and_consume_exception_with_exception_filter() {
        givenResponse(500);
        TestHttpService uselessFallback = spy(TestHttpService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallbackFactory(e -> uselessFallback, MyException.class)
                .withFallbackFactory(TestHttpServiceFallbackWithException::new)
                .build();
        TestHttpService testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        String result = testService.greeting();

        assertThat(result)
                .startsWith("Message from exception:");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void should_go_to_fallback_and_consume_exception_with_predicate() {
        givenResponse(500);
        TestHttpService uselessFallback = spy(TestHttpService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallbackFactory(TestHttpServiceFallbackWithException::new,
                        HttpServerErrorException.class::isInstance)
                .withFallbackFactory(e -> uselessFallback)
                .build();
        TestHttpService testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        String result = testService.greeting();

        assertThat(result)
                .startsWith("Message from exception:");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void should_go_to_second_fallback_and_consume_exception_with_predicate() {
        givenResponse(500);
        TestHttpService uselessFallback = spy(TestHttpService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallbackFactory(e -> uselessFallback, MyException.class::isInstance)
                .withFallbackFactory(TestHttpServiceFallbackWithException::new)
                .build();
        TestHttpService testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        String result = testService.greeting();

        assertThat(result)
                .startsWith("Message from exception:");
        verify(uselessFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void should_pass_exception_to_factory() {
        givenResponse(500);

        TestHttpService testService = buildTestService(e -> {
            assertThat(e).isInstanceOf(HttpServerErrorException.class);
            return new TestHttpServiceFallbackWithException(e);
        });

        String result = testService.greeting();

        assertThat(result).startsWith("Message from exception:");
    }

    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello, world!")));
    }

    private static class MyException extends Exception {
    }
}
