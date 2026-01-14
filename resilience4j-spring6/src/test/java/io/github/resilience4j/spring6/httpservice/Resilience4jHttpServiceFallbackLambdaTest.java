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
import io.github.resilience4j.spring6.httpservice.test.LambdaFallbackCreator;
import io.github.resilience4j.spring6.httpservice.test.TestGreetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class Resilience4jHttpServiceFallbackLambdaTest {

    private HttpServiceProxyFactory factory;
    private TestGreetingService testService;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withFallback(LambdaFallbackCreator.createLambdaFallback())
                .build();

        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestGreetingService.class);
    }

    @Test
    void testFallback() {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello, world!")));

        String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("fallback");
        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

}
