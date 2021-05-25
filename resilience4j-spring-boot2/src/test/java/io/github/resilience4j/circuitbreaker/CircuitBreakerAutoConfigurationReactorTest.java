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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTO;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.ReactiveDummyService;
import io.github.resilience4j.service.test.TestApplication;
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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class CircuitBreakerAutoConfigurationReactorTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    CircuitBreakerProperties circuitBreakerProperties;
    @Autowired
    CircuitBreakerAspect circuitBreakerAspect;
    @Autowired
    DummyService dummyService;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ReactiveDummyService reactiveDummyService;

    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the
     * DummyService is invoked and that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfigurationReactive() throws IOException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();

        List<CircuitBreakerEventDTO> circuitBreakerEventsBefore = getCircuitBreakersEvents();
        List<CircuitBreakerEventDTO> circuitBreakerEventsForBBefore = getCircuitBreakerEvents("backendB");

        try {
            reactiveDummyService.doSomethingFlux(true).subscribe(String::toUpperCase,
                throwable -> System.out.println("Exception received:" + throwable.getMessage()));
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        reactiveDummyService.doSomethingFlux(false).subscribe(String::toUpperCase,
            throwable -> System.out.println("Exception received:" + throwable.getMessage()));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker(ReactiveDummyService.BACKEND);
        assertThat(circuitBreaker).isNotNull();

        // expect CircuitBreaker is configured as defined in application.yml
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
        assertThat(
            circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(5);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(50f);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState())
            .isEqualByComparingTo(Duration.ofSeconds(5L));

        // expect circuitbreakers actuator endpoint contains all circuitbreakers
        ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate
            .getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
        assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(6)
            .containsExactly("backendA", "backendB", "backendC", "backendSharedA", "backendSharedB",
                "dummyFeignClient");

        // expect circuitbreaker-event actuator endpoint recorded both events
        assertThat(getCircuitBreakersEvents())
            .hasSize(circuitBreakerEventsBefore.size() + 2);
        assertThat(getCircuitBreakerEvents("backendB"))
            .hasSize(circuitBreakerEventsForBBefore.size() + 2);

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<CompositeHealthResponse> healthResponse = restTemplate
            .getForEntity("/actuator/health/circuitBreakers", CompositeHealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendA")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendB")).isNull();

        assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);
    }

    private List<CircuitBreakerEventDTO> getCircuitBreakersEvents() {
        return getEventsFrom("/actuator/circuitbreakerevents");
    }

    private List<CircuitBreakerEventDTO> getCircuitBreakerEvents(String name) {
        return getEventsFrom("/actuator/circuitbreakerevents/" + name);
    }

    private List<CircuitBreakerEventDTO> getEventsFrom(String path) {
        return restTemplate.getForEntity(path, CircuitBreakerEventsEndpointResponse.class)
            .getBody().getCircuitBreakerEvents();
    }
}
