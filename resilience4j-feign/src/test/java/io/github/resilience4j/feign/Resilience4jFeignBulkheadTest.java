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
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.feign.test.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the integration of the {@link Resilience4jFeign} with a bulkhead.
 */
@WireMockTest
class Resilience4jFeignBulkheadTest {

    private TestService testService;
    private Bulkhead bulkhead;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        bulkhead = spy(Bulkhead.of("bulkheadTest", BulkheadConfig.ofDefaults()));
        final FeignDecorators decorators = FeignDecorators.builder()
                .withBulkhead(bulkhead)
                .build();
        testService = Feign.builder()
                .addCapability(Resilience4jFeign.capability(decorators))
                .target(TestService.class, wmRuntimeInfo.getHttpBaseUrl() + "/");
    }

    @Test
    void successfulCall() {
        givenResponse(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(bulkhead).acquirePermission();
    }

    @Test
    void successfulCallWithDefaultMethod() {
        givenResponse(200);

        testService.defaultGreeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
        verify(bulkhead).acquirePermission();
    }

    @Test
    void bulkheadFull() {
        givenResponse(200);
        when(bulkhead.tryAcquirePermission()).thenReturn(false);

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(BulkheadFullException.class);

        verify(0, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    void failedCall() {
        givenResponse(400);
        when(bulkhead.tryAcquirePermission()).thenReturn(true);

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(FeignException.class);
    }


    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
