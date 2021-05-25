/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.service.test.DummyFeignClient;
import io.github.resilience4j.service.test.TestApplication;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class CircuitBreakerFeignTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private DummyFeignClient dummyFeignClient;


    /**
     * This test verifies that the combination of @FeignClient and @CircuitBreaker annotation works
     * as same as @CircuitBreaker alone works with any normal service class
     */
    @Test
    public void testFeignClient() {

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/sample/"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("This is successful call")));
        WireMock.stubFor(WireMock.get(WireMock.urlMatching("^.*\\/sample\\/error.*$"))
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
        dummyFeignClient.doSomething(StringUtils.EMPTY);
        dummyFeignClient.doSomething(StringUtils.EMPTY);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("dummyFeignClient");
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(18);
        assertThat(
            circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(6);
    }
}
