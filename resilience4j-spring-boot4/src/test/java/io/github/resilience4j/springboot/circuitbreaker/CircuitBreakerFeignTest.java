/*
 * Copyright 2026 Robert Winkler, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.circuitbreaker;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.service.test.DummyFeignClient;
import io.github.resilience4j.springboot.service.test.TestApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
class CircuitBreakerFeignTest {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8090))
            .build();
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private DummyFeignClient dummyFeignClient;

    /**
     * This test verifies that the combination of @FeignClient and @CircuitBreaker annotation works
     * as same as @CircuitBreaker alone works with any normal service class
     */
    @Test
    void testFeignClient() {

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/sample/"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("This is successful call")));
        wireMockServer.stubFor(WireMock.get(WireMock.urlMatching("^.*\\/sample\\/error.*$"))
            .willReturn(WireMock.aResponse().withStatus(400).withBody("This is error")));

        try {
            dummyFeignClient.doSomething("error");
        } catch (Exception e) {
            // Ignore the error, we want to increase the error counts
        }
        try {
            dummyFeignClient.doSomething("errorAgain");
        } catch (Exception e) {
            // Ignore the error, we want to increase the error counts
        }
        dummyFeignClient.doSomething("");
        dummyFeignClient.doSomething("");

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("dummyFeignClient");
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(18);
        assertThat(
            circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(6);
    }
}
