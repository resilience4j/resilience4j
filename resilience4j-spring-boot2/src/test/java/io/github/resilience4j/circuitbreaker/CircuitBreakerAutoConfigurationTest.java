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
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyFeignClient;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.ReactiveDummyService;
import io.github.resilience4j.service.test.TestApplication;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class CircuitBreakerAutoConfigurationTest {

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

    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the
     * DummyService is invoked and that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfiguration() throws IOException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();

        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(
            "/actuator/circuitbreakerevents");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsForABefore = circuitBreakerEvents(
            "/actuator" +
                "/circuitbreakerevents/backendA");

        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyService.doSomething(false);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(DummyService.BACKEND);
        assertThat(circuitBreaker).isNotNull();

        // expect circuitbreaker is configured as defined in application.yml
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(6);
        assertThat(
            circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(2);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(70f);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState())
            .isEqualByComparingTo(Duration.ofSeconds(5L));

        // Create CircuitBreaker dynamically with default config
        CircuitBreaker dynamicCircuitBreaker = circuitBreakerRegistry
            .circuitBreaker("dynamicBackend");

        // expect circuitbreaker-event actuator endpoint recorded all events
        CircuitBreakerEventsEndpointResponse circuitBreakerEventList = circuitBreakerEvents(
            "/actuator/circuitbreakerevents");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventsBefore.getCircuitBreakerEvents().size() + 2);

        circuitBreakerEventList = circuitBreakerEvents("/actuator/circuitbreakerevents/backendA");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventsForABefore.getCircuitBreakerEvents().size() + 2);

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<CompositeHealthResponse> healthResponse = restTemplate
            .getForEntity("/actuator/health", CompositeHealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("circuitBreakers")).isNotNull();
        HealthResponse circuitBreakerHealth = healthResponse.getBody().getDetails()
            .get("circuitBreakers");
        assertThat(circuitBreakerHealth.getDetails().get("backendA")).isNotNull();
        assertThat(circuitBreakerHealth.getDetails().get("backendB")).isNull();
        assertThat(circuitBreakerHealth.getDetails().get("backendSharedA")).isNotNull();
        assertThat(circuitBreakerHealth.getDetails().get("backendSharedB")).isNotNull();
        assertThat(circuitBreakerHealth.getDetails().get("dynamicBackend")).isNotNull();

        assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordExceptionPredicate()
            .test(new RecordedException())).isTrue();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getIgnoreExceptionPredicate()
            .test(new IgnoredException())).isTrue();

        // Verify that an exception for which setRecordFailurePredicate returns false and it is not included in
        // setRecordExceptions evaluates to false.
        assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordExceptionPredicate()
            .test(new Exception())).isFalse();

        assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);

        // expect all shared configs share the same values and are from the application.yml file
        CircuitBreaker sharedA = circuitBreakerRegistry.circuitBreaker("backendSharedA");
        CircuitBreaker sharedB = circuitBreakerRegistry.circuitBreaker("backendSharedB");
        CircuitBreaker backendB = circuitBreakerRegistry.circuitBreaker("backendB");
        CircuitBreaker backendC = circuitBreakerRegistry.circuitBreaker("backendC");

        Duration defaultWaitDuration = Duration.ofSeconds(10);
        float defaultFailureRate = 60f;
        int defaultPermittedNumberOfCallsInHalfOpenState = 10;
        int defaultRingBufferSizeInClosedState = 100;
        // test the customizer effect which overload the sliding widow size
        assertThat(backendC.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(100);

        assertThat(backendB.getCircuitBreakerConfig().getSlidingWindowType())
            .isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);

        assertThat(sharedA.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(6);
        assertThat(sharedA.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(defaultPermittedNumberOfCallsInHalfOpenState);
        assertThat(sharedA.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(defaultFailureRate);
        assertThat(sharedA.getCircuitBreakerConfig().getWaitDurationInOpenState())
            .isEqualTo(defaultWaitDuration);

        assertThat(sharedB.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(defaultRingBufferSizeInClosedState);
        assertThat(sharedB.getCircuitBreakerConfig().getSlidingWindowType())
            .isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        assertThat(sharedB.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(defaultPermittedNumberOfCallsInHalfOpenState);
        assertThat(sharedB.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(defaultFailureRate);
        assertThat(sharedB.getCircuitBreakerConfig().getWaitDurationInOpenState())
            .isEqualTo(defaultWaitDuration);

        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(defaultRingBufferSizeInClosedState);
        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig()
            .getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(defaultPermittedNumberOfCallsInHalfOpenState);
        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(defaultFailureRate);
        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState())
            .isEqualTo(defaultWaitDuration);
    }


    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the
     * DummyService is invoked and that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfigurationAsync()
        throws IOException, ExecutionException, InterruptedException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();

        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(
            "/actuator/circuitbreakerevents");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsForABefore = circuitBreakerEvents(
            "/actuator" +
                "/circuitbreakerevents/backendA");

        try {
            dummyService.doSomethingAsync(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        final CompletableFuture<String> stringCompletionStage = dummyService
            .doSomethingAsync(false);
        assertThat(stringCompletionStage.get()).isEqualTo("Test result");

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(DummyService.BACKEND);
        assertThat(circuitBreaker).isNotNull();

        // expect circuitbreakers actuator endpoint contains both circuitbreakers
        ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate
            .getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
        assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(6)
            .containsExactly("backendA", "backendB", "backendC", "backendSharedA", "backendSharedB",
                "dummyFeignClient");

        // expect circuitbreaker-event actuator endpoint recorded both events
        CircuitBreakerEventsEndpointResponse circuitBreakerEventList = circuitBreakerEvents(
            "/actuator/circuitbreakerevents");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventsBefore.getCircuitBreakerEvents().size() + 2);

        circuitBreakerEventList = circuitBreakerEvents("/actuator/circuitbreakerevents/backendA");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventsForABefore.getCircuitBreakerEvents().size() + 2);

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<CompositeHealthResponse> healthResponse = restTemplate
            .getForEntity("/actuator/health", CompositeHealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("circuitBreakers")).isNotNull();
        HealthResponse circuitBreakerHealth = healthResponse.getBody().getDetails()
            .get("circuitBreakers");
        assertThat(circuitBreakerHealth.getDetails().get("backendA")).isNotNull();
        assertThat(circuitBreakerHealth.getDetails().get("backendB")).isNull();
        assertThat(circuitBreakerHealth.getDetails().get("backendSharedA")).isNotNull();
        assertThat(circuitBreakerHealth.getDetails().get("backendSharedB")).isNotNull();


    }


    @Test
    public void shouldDefineWaitIntervalFunctionInOpenStateForCircuitBreakerAutoConfiguration() {
        //when
        final CircuitBreaker backendC = circuitBreakerRegistry.getAllCircuitBreakers()
            .filter(circuitBreaker -> circuitBreaker.getName().equalsIgnoreCase("backendC"))
            .get();
        //then
        assertThat(backendC).isNotNull();
        CircuitBreakerConfig backendConfig = backendC.getCircuitBreakerConfig();

        assertThat(backendConfig.getWaitIntervalFunctionInOpenState()).isNotNull();
        assertThat(backendConfig.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(1000);
        assertThat(backendConfig.getWaitDurationInOpenState())
            .isEqualByComparingTo(Duration.ofSeconds(1L));

    }

    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the
     * DummyService is invoked and that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfigurationReactive() throws IOException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();

        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(
            "/actuator" +
                "/circuitbreakerevents");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsForBBefore = circuitBreakerEvents(
            "/actuator/circuitbreakerevents/backendB");

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

        // expect circuitbreaker is configured as defined in application.yml
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
        CircuitBreakerEventsEndpointResponse circuitBreakerEventList = circuitBreakerEvents(
            "/actuator/circuitbreakerevents");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventsBefore.getCircuitBreakerEvents().size() + 2);

        circuitBreakerEventList = circuitBreakerEvents("/actuator/circuitbreakerevents/backendB");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventsForBBefore.getCircuitBreakerEvents().size() + 2);

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<CompositeHealthResponse> healthResponse = restTemplate
            .getForEntity("/actuator/health", CompositeHealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("circuitBreakers")).isNotNull();
        HealthResponse circuitBreakerHealth = healthResponse.getBody().getDetails()
            .get("circuitBreakers");
        assertThat(circuitBreakerHealth.getDetails().get("backendA")).isNotNull();
        assertThat(circuitBreakerHealth.getDetails().get("backendB")).isNull();

        assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);
    }

    private CircuitBreakerEventsEndpointResponse circuitBreakerEvents(String s) {
        return restTemplate.getForEntity(s, CircuitBreakerEventsEndpointResponse.class).getBody();
    }

    private static final class CompositeHealthResponse {

        private Map<String, HealthResponse> details;

        public Map<String, HealthResponse> getDetails() {
            return details;
        }

        public void setDetails(Map<String, HealthResponse> details) {
            this.details = details;
        }
    }

    private static final class HealthResponse {

        private Map<String, Object> details;

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }
}
