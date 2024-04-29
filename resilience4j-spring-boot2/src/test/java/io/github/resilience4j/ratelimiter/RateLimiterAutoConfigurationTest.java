/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEndpointResponse;
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventDTO;
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventsEndpointResponse;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.autoconfigure.RateLimiterProperties;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspect;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.TestApplication;
import io.github.resilience4j.service.test.ratelimiter.RateLimiterDummyFeignClient;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static io.github.resilience4j.service.test.ratelimiter.RateLimiterDummyFeignClient.RATE_LIMITER_FEIGN_CLIENT_NAME;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class RateLimiterAutoConfigurationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    @Autowired
    private RateLimiterProperties rateLimiterProperties;
    @Autowired
    private RateLimiterAspect rateLimiterAspect;
    @Autowired
    private DummyService dummyService;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private RateLimiterDummyFeignClient rateLimiterDummyFeignClient;
    @Autowired
    private EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry;

    /**
     * This test verifies that the combination of @FeignClient and @RateLimiter annotation works as
     * same as @Bulkhead alone works with any normal service class
     */
    @Test
    @Ignore
    public void testFeignClient() {
        WireMock.stubFor(WireMock
            .get(WireMock.urlEqualTo("/limit/"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("This is successful call"))
        );
        WireMock.stubFor(WireMock.get(WireMock.urlMatching("^.*\\/limit\\/error.*$"))
            .willReturn(WireMock.aResponse().withStatus(400).withBody("This is error")));

        assertThat(rateLimiterRegistry).isNotNull();
        assertThat(rateLimiterProperties).isNotNull();

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(RATE_LIMITER_FEIGN_CLIENT_NAME);
        assertThat(rateLimiter).isNotNull();
        rateLimiter.acquirePermission();
        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() -> rateLimiter.getMetrics().getAvailablePermissions() == 10);

        try {
            rateLimiterDummyFeignClient.doSomething("error");
        } catch (Exception ex) {
            // Do nothing.
        }
        rateLimiterDummyFeignClient.doSomething(EMPTY);

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(8);
        assertThat(rateLimiter.getMetrics().getNumberOfWaitingThreads()).isZero();

        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(10);
        assertThat(rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod())
            .isEqualTo(Duration.ofSeconds(1));
        assertThat(rateLimiter.getRateLimiterConfig().getTimeoutDuration())
            .isEqualTo(Duration.ofSeconds(0));

        // Test Actuator endpoints

        ResponseEntity<RateLimiterEndpointResponse> rateLimiterList = restTemplate
            .getForEntity("/actuator/ratelimiters", RateLimiterEndpointResponse.class);

        assertThat(rateLimiterList.getBody().getRateLimiters()).hasSize(4)
            .containsExactly("backendA", "backendB", "backendCustomizer",
                "rateLimiterDummyFeignClient");

        try {
            for (int i = 0; i < 11; i++) {
                rateLimiterDummyFeignClient.doSomething(EMPTY);
            }
        } catch (RequestNotPermitted e) {
            // Do nothing
        }

        ResponseEntity<RateLimiterEventsEndpointResponse> rateLimiterEventList = restTemplate
            .getForEntity("/actuator/ratelimiterevents", RateLimiterEventsEndpointResponse.class);

        List<RateLimiterEventDTO> eventsList = rateLimiterEventList.getBody()
            .getRateLimiterEvents();
        assertThat(eventsList).isNotEmpty();
        RateLimiterEventDTO lastEvent = eventsList.get(eventsList.size() - 1);
        assertThat(lastEvent.getType()).isEqualTo(RateLimiterEvent.Type.FAILED_ACQUIRE);

        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() -> rateLimiter.getMetrics().getAvailablePermissions() == 10);

        assertThat(rateLimiterAspect.getOrder()).isEqualTo(401);
    }

    /**
     * The test verifies that a RateLimiter instance is created and configured properly when the
     * DummyService is invoked and that the RateLimiter records successful and failed calls.
     */
    @Test
    public void testRateLimiterAutoConfiguration() throws IOException {
        assertThat(rateLimiterRegistry).isNotNull();
        assertThat(rateLimiterProperties).isNotNull();

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(DummyService.BACKEND);
        assertThat(rateLimiter).isNotNull();
        rateLimiter.acquirePermission();
        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() -> rateLimiter.getMetrics().getAvailablePermissions() == 10);

        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing.
        }
        dummyService.doSomething(false);

        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(8);
        assertThat(rateLimiter.getMetrics().getNumberOfWaitingThreads()).isZero();

        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(10);
        assertThat(rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod())
            .isEqualTo(Duration.ofSeconds(1));
        assertThat(rateLimiter.getRateLimiterConfig().getTimeoutDuration())
            .isEqualTo(Duration.ofSeconds(0));

        // Test Actuator endpoints

        ResponseEntity<RateLimiterEndpointResponse> rateLimiterList = restTemplate
            .getForEntity("/actuator/ratelimiters", RateLimiterEndpointResponse.class);

        assertThat(rateLimiterList.getBody().getRateLimiters()).hasSize(4)
            .containsExactly("backendA", "backendB", "backendCustomizer",
                "rateLimiterDummyFeignClient");

        try {
            for (int i = 0; i < 11; i++) {
                dummyService.doSomething(false);
            }
        } catch (RequestNotPermitted e) {
            // Do nothing
        }

        ResponseEntity<RateLimiterEventsEndpointResponse> rateLimiterEventList = restTemplate
            .getForEntity("/actuator/ratelimiterevents", RateLimiterEventsEndpointResponse.class);

        List<RateLimiterEventDTO> eventsList = rateLimiterEventList.getBody()
            .getRateLimiterEvents();
        assertThat(eventsList).isNotEmpty();
        RateLimiterEventDTO lastEvent = eventsList.get(eventsList.size() - 1);
        assertThat(lastEvent.getType()).isEqualTo(RateLimiterEvent.Type.FAILED_ACQUIRE);

        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() -> rateLimiter.getMetrics().getAvailablePermissions() == 10);

        assertThat(rateLimiterAspect.getOrder()).isEqualTo(401);

        // test the customizer impact for specific instance
        RateLimiter backendCustomizer = rateLimiterRegistry.rateLimiter("backendCustomizer");
        assertThat(backendCustomizer.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(200);

    }


    @Test
    public void testPermitsInRateLimiterAnnotation() {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(DummyService.BACKEND);
        await()
                .atMost(2, TimeUnit.SECONDS)
                .until(() -> rateLimiter.getMetrics().getAvailablePermissions() == 10);

        dummyService.doSomethingExpensive();
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(0);

        assertThat(eventConsumerRegistry.getEventConsumer(DummyService.BACKEND).getBufferedEvents()).last()
                .satisfies(event -> {
                    assertThat(event.getEventType()).isEqualTo(RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE);
                    assertThat(event.getNumberOfPermits()).isEqualTo(10);
                });
    }
}
