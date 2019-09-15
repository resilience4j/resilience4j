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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.github.resilience4j.feign.test.TestService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

/**
 * Tests the integration of the {@link Resilience4jFeign} with {@link RateLimiter}
 */
public class Resilience4jRateLimiterTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private static final RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(ofMillis(50))
            .limitRefreshPeriod(ofSeconds(1))
            .limitForPeriod(1)
            .build();

    private TestService testService;

    @Before
    public void setUp() {
        final RateLimiter rateLimiter = RateLimiter.of("backendName", config);
        final FeignDecorators decorators = FeignDecorators.builder().withRateLimiter(rateLimiter).build();
        testService = Resilience4jFeign.builder(decorators).target(TestService.class, "http://localhost:8080/");
    }

    @Test
    public void testSuccessfulCall() throws Exception {
        setupStub(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test(expected = RequestNotPermitted.class)
    public void testRateLimiterLimiting() {
        setupStub(200);

        testService.greeting();
        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test(expected = RequestNotPermitted.class)
    public void testRateLimiterNotLimiting() throws Exception {
        setupStub(200);

        testService.greeting();
        testService.greeting();
        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test(expected = FeignException.class)
    public void testFailedHttpCall() {
        setupStub(400);

        testService.greeting();
    }


    private void setupStub(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
