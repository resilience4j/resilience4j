/*
 *
 * Copyright 2018
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/**
 * Tests the integration of the {@link Resilience4jFeign} with {@link CircuitBreaker}
 */
public class Resilience4jFeignCircuitBreakerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private static final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(3)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();

    private CircuitBreaker circuitBreaker;
    private TestService testService;

    @Before
    public void setUp() {
        FeignDecorators.builder().withCircuitBreaker(circuitBreaker).build();
        this.circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        final FeignDecorators decorators = FeignDecorators.builder().withCircuitBreaker(circuitBreaker).build();
        this.testService = Resilience4jFeign.builder(decorators).target(TestService.class, "http://localhost:8080/");
    }

    @Test
    public void testSuccessfulCalls() throws Exception {
        setupStub(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls())
                .describedAs("Successful Calls")
                .isEqualTo(1);
    }

    @Test
    public void testFailedCalls() throws Exception {
        boolean exceptionThrown = false;
        setupStub(400);

        try {
            testService.greeting();
        } catch (final FeignException ex) {
            exceptionThrown = true;
        }

        assertTrue("Expected service to throw FeignException!", exceptionThrown);
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls())
                .describedAs("Successful Calls")
                .isEqualTo(1);
    }

    private void setupStub(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
