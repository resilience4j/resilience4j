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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.github.resilience4j.feign.test.TestService;

/**
 * Tests the integration of the {@link Resilience4jFeign} with a fallback.
 */
public class Resilience4jFeignFallbackTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private TestService testService;
    private TestService testServiceFallback;

    @Before
    public void setUp() {
        final String url = "http://localhost:8080/";

        testServiceFallback = mock(TestService.class);
        when(testServiceFallback.greeting()).thenReturn("fallback");

        final FeignDecorators decorators = FeignDecorators.builder().withFallback(testServiceFallback).build();

        testService = Resilience4jFeign.builder(decorators).target(TestService.class, url);
    }

    @Test
    public void testSuccessful() throws Exception {
        setupStub(200);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback, times(0)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }


    @Test
    public void testFallback() throws Exception {
        setupStub(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(testServiceFallback, times(1)).greeting();
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }


    @Test
    public void testRevertFallback() throws Exception {
        setupStub(400);

        testService.greeting();
        setupStub(200);
        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("Hello, world!");
        verify(testServiceFallback, times(1)).greeting();
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
