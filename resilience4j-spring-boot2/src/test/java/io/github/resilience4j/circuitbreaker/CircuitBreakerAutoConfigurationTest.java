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
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTO;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerUpdateStateResponse;
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
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Test
    public void testCircuitBreakerActuatorEndpoint() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // when
        HttpEntity<String> forceOpenRequest = new HttpEntity<>("{\"updateState\":\"FORCE_OPEN\"}", headers);
        final ResponseEntity<CircuitBreakerUpdateStateResponse> backendAState = restTemplate
            .postForEntity("/actuator/circuitbreakers/backendA", forceOpenRequest, CircuitBreakerUpdateStateResponse.class);
        // then
        assertThat(backendAState.getBody()).isNotNull();
        assertThat(backendAState.getBody().getCurrentState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN.toString());
        assertThat(circuitBreakerRegistry.circuitBreaker("backendA").getState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN);

        // when sending non valid state change
        HttpEntity<String> nonValid = new HttpEntity<>("{\"updateState\":\"BLA_BLA\"}", headers);
        final ResponseEntity<CircuitBreakerUpdateStateResponse> nonValidResponse = restTemplate
            .postForEntity("/actuator/circuitbreakers/backendA", nonValid, CircuitBreakerUpdateStateResponse.class);
        // then
        assertThat(nonValidResponse.getBody()).isNotNull();
        assertThat(nonValidResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // when
        HttpEntity<String> disableRequest = new HttpEntity<>("{\"updateState\":\"DISABLE\"}", headers);
        final ResponseEntity<CircuitBreakerUpdateStateResponse> backendAStateDisabled = restTemplate
            .postForEntity("/actuator/circuitbreakers/backendA", disableRequest, CircuitBreakerUpdateStateResponse.class);
        // then
        assertThat(backendAStateDisabled.getBody()).isNotNull();
        assertThat(backendAStateDisabled.getBody().getCurrentState()).isEqualTo(CircuitBreaker.State.DISABLED.toString());
        assertThat(circuitBreakerRegistry.circuitBreaker("backendA").getState()).isEqualTo(CircuitBreaker.State.DISABLED);

        // when
        HttpEntity<String> closeRequest = new HttpEntity<>("{\"updateState\":\"CLOSE\"}", headers);
        final ResponseEntity<CircuitBreakerUpdateStateResponse> backendAStateClosed = restTemplate
            .postForEntity("/actuator/circuitbreakers/backendA", closeRequest, CircuitBreakerUpdateStateResponse.class);
        // then
        assertThat(backendAStateClosed.getBody()).isNotNull();
        assertThat(backendAStateClosed.getBody().getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED.toString());
        assertThat(circuitBreakerRegistry.circuitBreaker("backendA").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the
     * DummyService is invoked and that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfiguration() throws IOException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();

        List<CircuitBreakerEventDTO> circuitBreakerEventsBefore = getCircuitBreakersEvents();
        List<CircuitBreakerEventDTO> circuitBreakerEventsForABefore = getCircuitBreakerEvents("backendA");

        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyService.doSomething(false);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(DummyService.BACKEND);
        assertThat(circuitBreaker).isNotNull();

        // expect CircuitBreaker is configured as defined in application.yml
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(6);
        assertThat(
            circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(2);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(70f);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
            .isEqualTo(5000L);

        // Create CircuitBreaker dynamically with default config
        CircuitBreaker dynamicCircuitBreaker = circuitBreakerRegistry
            .circuitBreaker("dynamicBackend");

        // expect circuitbreaker-event actuator endpoint recorded all events
        assertThat(getCircuitBreakersEvents())
            .hasSize(circuitBreakerEventsBefore.size() + 2);
        assertThat(getCircuitBreakerEvents("backendA"))
            .hasSize(circuitBreakerEventsForABefore.size() + 2);

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<CompositeHealthResponse> healthResponse = restTemplate
            .getForEntity("/actuator/health/circuitBreakers", CompositeHealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendA")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendB")).isNull();
        assertThat(healthResponse.getBody().getDetails().get("backendSharedA")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendSharedB")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("dynamicBackend")).isNotNull();

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

        long defaultWaitDuration = 10_000;
        float defaultFailureRate = 60f;
        int defaultPermittedNumberOfCallsInHalfOpenState = 10;
        int defaultSlidingWindowSize = 100;
        // test the customizer effect which overload the sliding widow size
        assertThat(backendC.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(100);

        assertThat(backendB.getCircuitBreakerConfig().getSlidingWindowType())
            .isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);

        assertThat(sharedA.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(6);
        assertThat(sharedA.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(defaultPermittedNumberOfCallsInHalfOpenState);
        assertThat(sharedA.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(defaultFailureRate);
        assertThat(sharedA.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
            .isEqualTo(defaultWaitDuration);

        assertThat(sharedB.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(defaultSlidingWindowSize);
        assertThat(sharedB.getCircuitBreakerConfig().getSlidingWindowType())
            .isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        assertThat(sharedB.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(defaultPermittedNumberOfCallsInHalfOpenState);
        assertThat(sharedB.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(defaultFailureRate);
        assertThat(sharedB.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
            .isEqualTo(defaultWaitDuration);

        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize())
            .isEqualTo(defaultSlidingWindowSize);
        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig()
            .getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(defaultPermittedNumberOfCallsInHalfOpenState);
        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
            .isEqualTo(defaultFailureRate);
        assertThat(dynamicCircuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
            .isEqualTo(defaultWaitDuration);
    }

    @Test
    public void testCircuitBreakerRegistryAutoConfiguration() throws IOException {
        CircuitBreaker circuitBreakerA = circuitBreakerRegistry.circuitBreaker(DummyService.BACKEND);
        try {
            CircuitBreaker circuitBreakerA2 = CircuitBreaker.of(DummyService.BACKEND,
                CircuitBreakerConfig.ofDefaults(),
                circuitBreakerA.getTags());

            circuitBreakerRegistry.replace(DummyService.BACKEND, circuitBreakerA2);
            dummyService.doSomething(false);

            assertThat(getCircuitBreakersEvents()).hasSize(1);
            assertThat(getCircuitBreakerEvents(DummyService.BACKEND)).hasSize(1);
        } finally {
            // clean up
            circuitBreakerRegistry.replace(DummyService.BACKEND, circuitBreakerA);
        }
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
        List<CircuitBreakerEventDTO> circuitBreakerEventsBefore = getCircuitBreakersEvents();
        List<CircuitBreakerEventDTO> circuitBreakerEventsForABefore = getCircuitBreakerEvents("backendA");

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

        // expect circuitbreakers actuator endpoint contains both circuit breakers
        ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate
            .getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
        assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(6)
            .containsExactly("backendA", "backendB", "backendC", "backendSharedA", "backendSharedB",
                "dummyFeignClient");

        // expect circuitbreaker-event actuator endpoint recorded both events
        assertThat(getCircuitBreakersEvents())
            .hasSize(circuitBreakerEventsBefore.size() + 2);
        assertThat(getCircuitBreakerEvents("backendA"))
            .hasSize(circuitBreakerEventsForABefore.size() + 2);

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<CompositeHealthResponse> healthResponse = restTemplate
            .getForEntity("/actuator/health/circuitBreakers", CompositeHealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendA")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendB")).isNull();
        assertThat(healthResponse.getBody().getDetails().get("backendSharedA")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendSharedB")).isNotNull();
    }

    @Test
    public void shouldDefineWaitIntervalFunctionInOpenStateForCircuitBreakerAutoConfiguration() {
        //when
        final Optional<CircuitBreaker> backendC = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .filter(circuitBreaker -> circuitBreaker.getName().equalsIgnoreCase("backendC"))
            .findAny();
        //then
        assertThat(backendC).isPresent();
        CircuitBreakerConfig backendConfig = backendC.get().getCircuitBreakerConfig();

        assertThat(backendConfig.getWaitIntervalFunctionInOpenState()).isNotNull();
        assertThat(backendConfig.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(1000);
        assertThat(backendConfig.getWaitIntervalFunctionInOpenState().apply(2)).isEqualTo(1111);
        assertThat(backendConfig.getWaitDurationInOpenState())
            .isEqualByComparingTo(Duration.ofSeconds(1L));
        assertThat(backendConfig.getWaitIntervalFunctionInOpenState().apply(1))
            .isEqualByComparingTo(1000L);
    }

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
        assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1))
            .isEqualByComparingTo(5000L);

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

    private static final class CompositeHealthResponse {

        private String status;
        private Map<String, HealthResponse> details;

        public Map<String, HealthResponse> getDetails() {
            return details;
        }

        public void setDetails(Map<String, HealthResponse> details) {
            this.details = details;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    private static final class HealthResponse {

        private String status;

        private Map<String, Object> details;

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
