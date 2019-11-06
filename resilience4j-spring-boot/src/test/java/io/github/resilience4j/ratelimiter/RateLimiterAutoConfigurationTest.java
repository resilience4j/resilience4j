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

import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEndpointResponse;
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventDTO;
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventsEndpointResponse;
import io.github.resilience4j.ratelimiter.autoconfigure.RateLimiterProperties;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspect;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.TestApplication;
import io.prometheus.client.CollectorRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class RateLimiterAutoConfigurationTest {

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    private RateLimiterProperties rateLimiterProperties;

    @Autowired
    private RateLimiterAspect rateLimiterAspect;

    @Autowired
    @Qualifier("rateLimiterDummyService")
    private DummyService dummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void setUp() {
        // Need to clear this static registry out since multiple tests register collectors that could collide.
        CollectorRegistry.defaultRegistry.clear();
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
        assertThat(rateLimiter.getMetrics().getNumberOfWaitingThreads()).isEqualTo(0);

        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(10);
        assertThat(rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod())
            .isEqualTo(Duration.ofSeconds(1));
        assertThat(rateLimiter.getRateLimiterConfig().getTimeoutDuration())
            .isEqualTo(Duration.ofSeconds(0));

        // Test Actuator endpoints

        ResponseEntity<RateLimiterEndpointResponse> rateLimiterList = restTemplate
            .getForEntity("/ratelimiter", RateLimiterEndpointResponse.class);

        assertThat(rateLimiterList.getBody().getRateLimiters()).hasSize(2)
            .containsExactly("backendA", "backendB");

        try {
            for (int i = 0; i < 11; i++) {
                dummyService.doSomething(false);
            }
        } catch (RequestNotPermitted e) {
            // Do nothing
        }

        ResponseEntity<RateLimiterEventsEndpointResponse> rateLimiterEventList = restTemplate
            .getForEntity("/ratelimiter/events", RateLimiterEventsEndpointResponse.class);

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
}
