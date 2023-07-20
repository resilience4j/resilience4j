/*
 *
 * Copyright 2019
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
import io.github.resilience4j.feign.test.Issue560;
import io.github.resilience4j.feign.test.TestService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration of the {@link Resilience4jFeign} with the lambda as a fallbackFactory.
 */
public class Resilience4jFeignFallbackFactoryLambdaTest {

    private static final String MOCK_URL = "http://localhost:8080/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private TestService testService;

    @Before
    public void setUp() {
        final FeignDecorators decorators = FeignDecorators.builder()
            .withFallbackFactory(e -> Issue560.createLambdaFallback())
            .build();

        this.testService = Resilience4jFeign.builder(decorators)
            .target(TestService.class, MOCK_URL);
    }

    @Test
    public void testFallbackFactory() {
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
