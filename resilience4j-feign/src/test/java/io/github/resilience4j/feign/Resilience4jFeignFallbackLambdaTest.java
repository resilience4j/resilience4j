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
import io.github.resilience4j.feign.test.Issue560;
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

/**
 * Tests the integration of the {@link Resilience4jFeign} with the lambda as a fallback.
 */
@WireMockTest
class Resilience4jFeignFallbackLambdaTest {

    private TestService testService;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(Issue560.createLambdaFallback())
            .build();

        this.testService = Feign.builder()
            .addCapability(Resilience4jFeign.capability(decorators))
            .target(TestService.class, wmRuntimeInfo.getHttpBaseUrl() + "/");
    }

    @Test
    void fallback() {
        setupStub();

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    private void setupStub() {
        stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "text/plain")
                .withBody("Hello, world!")));
    }
}
