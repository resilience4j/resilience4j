/*
 *
 * Copyright 2020 Mahmoud Romeh
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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.Feign;
import feign.FeignException;
import io.github.resilience4j.feign.test.TestService;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests the integration of the {@link Resilience4jFeign} with {@link Retry}
 */
public class Resilience4jFeignRetryTest {

    private static final String MOCK_URL = "http://localhost:8080/";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private TestService testService;
    private Retry retry;

    @Before
    public void setUp() {
        retry = spy(Retry.ofDefaults("test"));
        final FeignDecorators decorators = FeignDecorators.builder()
            .withRetry(retry)
            .build();
        testService = Feign.builder()
            .addCapability(Resilience4jFeign.capability(decorators))
            .target(TestService.class, MOCK_URL);
    }

    @Test
    public void testSuccessfulCall() {
        givenResponse(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(retry).context();
    }

    @Test
    public void testSuccessfulCallWithDefaultMethod() {
        givenResponse(200);

        testService.defaultGreeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(retry).context();
    }

    @Test(expected = FeignException.class)
    public void testFailedHttpCall() {
        givenResponse(400);
        testService.greeting();
    }

    @Test(expected = FeignException.class)
    public void testFailedHttpCallWithRetry() {
        retry = Retry.of("test",RetryConfig.custom().retryExceptions(FeignException.class).maxAttempts(2).build());
        final FeignDecorators decorators = FeignDecorators.builder()
            .withRetry(retry)
            .build();
        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
        givenResponse(400);
        testService.greeting();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
    }

    @Test
    public void testRetryOnResult() {
        retry = Retry.of("test",RetryConfig.<String>custom().retryOnResult(s->s.equalsIgnoreCase("hello world")).maxAttempts(2).build());
        final FeignDecorators decorators = FeignDecorators.builder()
            .withRetry(retry)
            .build();
        testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
        givenResponse(200);
        testService.greeting();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }


    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(responseCode)
                .withHeader("Content-Type", "text/plain")
                .withBody("hello world")));
    }
}
