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

import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.TestApplication;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

import java.io.IOException;

import static io.github.resilience4j.service.test.DummyService.BACKEND_A;
import static io.github.resilience4j.service.test.DummyService.BACKEND_B;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class)
public class CircuitBreakerAutoConfigurationTest {

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    CircuitBreakerProperties circuitBreakerProperties;

    @Autowired
    CircuitBreakerAspect circuitBreakerAspect;

    @Autowired
    @Qualifier(BACKEND_A)
    DummyService dummyServiceA;

    @Autowired
    @Qualifier(BACKEND_B)
    DummyService dummyServiceB;
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the DummyService is invoked and
     * that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfiguration() throws Throwable {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();

        try {
            dummyServiceA.doSomething(true);
        } catch (Throwable ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyServiceA.doSomething(false);

        CircuitBreaker circuitBreakerA = circuitBreakerRegistry.circuitBreaker(BACKEND_A);
        assertThat(circuitBreakerA).isNotNull();

        final CircuitBreaker.Metrics metrics = circuitBreakerA.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);

        final CircuitBreakerConfig config = circuitBreakerA.getCircuitBreakerConfig();
        assertThat(config.getRingBufferSizeInClosedState()).isEqualTo(6);
        assertThat(config.getRingBufferSizeInHalfOpenState()).isEqualTo(2);
        assertThat(config.getFailureRateThreshold()).isEqualTo(70f);
        assertThat(config.getWaitDurationInOpenState()).isEqualByComparingTo(Duration.ofSeconds(5L));
        assertThat(config.getRecordFailurePredicate().test(new RuntimeException())).isEqualTo(false);
        assertThat(config.getRecordFailurePredicate().test(new IOException())).isEqualTo(true);

        // expect circuitbreakers actuator endpoint contains both circuitbreakers
        ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate.getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
        assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(2).containsExactly("backendA", "backendB");


// expect circuitbreaker-event actuator endpoint recorded both events        ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = getEventList();
        ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = getEventList();
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

        circuitBreakerEventList = getEventList("backendA");
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<HealthResponse> healthResponse = restTemplate.getForEntity("/actuator/health", HealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendACircuitBreaker")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendBCircuitBreaker")).isNull();


        try {
            dummyServiceB.doSomething(true);
        } catch (Throwable ex) {
            // Do nothing. The Exception is not recorded by the CircuitBreaker.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyServiceB.doSomething(false);

        CircuitBreaker circuitBreakerB = circuitBreakerRegistry.circuitBreaker(BACKEND_B);
        assertThat(circuitBreakerB).isNotNull();

        final CircuitBreaker.Metrics metricsB = circuitBreakerB.getMetrics();
        assertThat(metricsB.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metricsB.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(metricsB.getNumberOfFailedCalls()).isEqualTo(0);

        final CircuitBreakerConfig configB = circuitBreakerB.getCircuitBreakerConfig();
        assertThat(configB.getRingBufferSizeInClosedState()).isEqualTo(10);
        assertThat(configB.getRingBufferSizeInHalfOpenState()).isEqualTo(5);
        assertThat(configB.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(configB.getRecordFailurePredicate().test(new RuntimeException())).isEqualTo(true);
        assertThat(configB.getRecordFailurePredicate().test(new IOException())).isEqualTo(false);

        // Test Actuator endpoint


        circuitBreakerEventList = getEventList();
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(4);

        circuitBreakerEventList = getEventList("backendA");
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

        circuitBreakerEventList = getEventList("backendB");
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);
        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<HealthResponse> healthResponse = restTemplate.getForEntity("/actuator/health", HealthResponse.class);
        assertThat(healthResponse.getBody().getDetails()).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendACircuitBreaker")).isNotNull();
        assertThat(healthResponse.getBody().getDetails().get("backendBCircuitBreaker")).isNull();

        // expect aspect configured as defined in application.yml
        assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);
    }

    private ResponseEntity<CircuitBreakerEventsEndpointResponse> getEventList() {
        return getEventList(null);
    }
    private ResponseEntity<CircuitBreakerEventsEndpointResponse> getEventList(@Nullable String backend) {
        String url = "/actuator/circuitbreaker-events";
        if(!StringUtils.isEmpty(backend)){
            // FIX hack for Spring boot actuators bug with selector and parameters active
            url += String.format("/%s?name=%s", backend, backend);
        }
        return restTemplate.getForEntity(url, CircuitBreakerEventsEndpointResponse.class);
    }

    private final static class HealthResponse {
    	private Map<String, Object> details;

    	public Map<String, Object> getDetails() {
			return details;
		}

    	public void setDetails(Map<String, Object> details) {
			this.details = details;
		}
    }
}
