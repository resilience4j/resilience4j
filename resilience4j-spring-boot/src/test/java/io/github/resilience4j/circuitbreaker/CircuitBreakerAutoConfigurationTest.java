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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
        assertThat(config.getRecordFailurePredicate().test(new RuntimeException())).isEqualTo(false);
        assertThat(config.getRecordFailurePredicate().test(new IOException())).isEqualTo(true);

        // Test Actuator endpoints
        ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = restTemplate.getForEntity("/circuitbreaker/events", CircuitBreakerEventsEndpointResponse.class);
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

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

        ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate.getForEntity("/circuitbreaker", CircuitBreakerEndpointResponse.class);
        assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(2).containsExactly("backendA", "backendB");

        circuitBreakerEventList = restTemplate.getForEntity("/circuitbreaker/events", CircuitBreakerEventsEndpointResponse.class);
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(4);

        assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);
    }
}
