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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the integration of the {@link Resilience4jFeign} with {@link Retry}
 */
@WireMockTest
class Resilience4jFeignRetryTest {

    private String baseUrl;
    private TestService testService;
    private Retry retry;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        baseUrl = wmRuntimeInfo.getHttpBaseUrl() + "/";
        retry = spy(Retry.ofDefaults("test"));
        final FeignDecorators decorators = FeignDecorators.builder()
            .withRetry(retry)
            .build();
        testService = Feign.builder()
            .addCapability(Resilience4jFeign.capability(decorators))
            .target(TestService.class, baseUrl);
    }

    @Test
    void successfulCall() {
        givenResponse(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(retry).context();
    }

    @Test
    void successfulCallWithDefaultMethod() {
        givenResponse(200);

        testService.defaultGreeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(retry).context();
    }

    @Test
    void failedHttpCall() {
        givenResponse(400);
        assertThatThrownBy(() -> testService.greeting())
            .isInstanceOf(FeignException.class);
    }

    @Test
    void failedHttpCallWithRetry() {
        retry = Retry.of("test", RetryConfig.custom().retryExceptions(FeignException.class).maxAttempts(2).build());
        final FeignDecorators decorators = FeignDecorators.builder()
            .withRetry(retry)
            .build();
        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, baseUrl);
        givenResponse(400);
        assertThatThrownBy(() -> testService.greeting())
            .isInstanceOf(FeignException.class);
    }

    @Test
    void retryOnResult() {
        retry = Retry.of("test", RetryConfig.<String>custom().retryOnResult(s -> s.equalsIgnoreCase("hello world")).maxAttempts(2).build());
        final FeignDecorators decorators = FeignDecorators.builder()
            .withRetry(retry)
            .build();
        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, baseUrl);
        givenResponse(200);
        testService.greeting();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isOne();
    }


    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));
    }
}
