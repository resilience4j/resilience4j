/*
 * Copyright 2025 Robert Winkler, Artur Havliukovskyi
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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerDetails;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerUpdateStateResponse;
import io.github.resilience4j.springboot.service.test.TestApplication;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
@AutoConfigureTestRestTemplate
public class CircuitBreakerActuatorTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testUpdateCircuitBreakerState() {
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

    @Test
    public void testCircuitBreakerDetails() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // when
        HttpEntity<String> forceOpenRequest = new HttpEntity<>("{\"updateState\":\"CLOSE\"}", headers);
        final ResponseEntity<CircuitBreakerUpdateStateResponse> backendAState = restTemplate
            .postForEntity("/actuator/circuitbreakers/backendA", forceOpenRequest, CircuitBreakerUpdateStateResponse.class);
        // then
        assertThat(backendAState.getBody()).isNotNull();
        assertThat(backendAState.getBody().getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED.toString());
        assertThat(circuitBreakerRegistry.circuitBreaker("backendA").getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // when get circuit breakers
        final ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakersResponse = restTemplate
            .getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
        // then
        assertThat(circuitBreakersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(circuitBreakersResponse.getBody()).isNotNull();
        assertThat(circuitBreakersResponse.getBody().getCircuitBreakers()).isNotNull();
        final CircuitBreakerDetails cbDetailsA = circuitBreakersResponse.getBody().getCircuitBreakers().get("backendA");
        final CircuitBreaker cbA = circuitBreakerRegistry.circuitBreaker("backendA");
        final CircuitBreaker.Metrics metrics = cbA.getMetrics();
        final CircuitBreakerConfig config = cbA.getCircuitBreakerConfig();
        assertThat(cbDetailsA.getFailureRate()).isEqualTo(metrics.getFailureRate() + "%");
        assertThat(cbDetailsA.getFailureRateThreshold()).isEqualTo(config.getFailureRateThreshold() + "%");
        assertThat(cbDetailsA.getSlowCallRate()).isEqualTo(metrics.getSlowCallRate() + "%");
        assertThat(cbDetailsA.getSlowCallRateThreshold()).isEqualTo(config.getSlowCallRateThreshold() + "%");
        assertThat(cbDetailsA.getBufferedCalls()).isEqualTo(metrics.getNumberOfBufferedCalls());
        assertThat(cbDetailsA.getSlowCalls()).isEqualTo(metrics.getNumberOfSlowCalls());
        assertThat(cbDetailsA.getSlowFailedCalls()).isEqualTo(metrics.getNumberOfSlowFailedCalls());
        assertThat(cbDetailsA.getFailedCalls()).isEqualTo(metrics.getNumberOfFailedCalls());
        assertThat(cbDetailsA.getNotPermittedCalls()).isEqualTo(metrics.getNumberOfNotPermittedCalls());
        assertThat(cbDetailsA.getState()).isEqualTo(cbA.getState());
    }

}
