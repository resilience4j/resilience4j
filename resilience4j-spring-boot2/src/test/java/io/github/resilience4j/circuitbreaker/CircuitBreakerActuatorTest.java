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
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerUpdateStateResponse;
import io.github.resilience4j.service.test.TestApplication;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
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

}
