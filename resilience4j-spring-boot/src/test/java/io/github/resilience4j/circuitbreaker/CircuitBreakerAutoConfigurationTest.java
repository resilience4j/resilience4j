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

import io.prometheus.client.CollectorRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.Duration;

import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.TestApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
@ContextConfiguration(classes = CircuitBreakerAutoConfigurationTest.AdditionalConfiguration.class)
public class CircuitBreakerAutoConfigurationTest {

    @Configuration
    public static class AdditionalConfiguration {

        // Shows that a circuit breaker can be created in code and still use the shared configuration.
        @Bean
        public CircuitBreaker otherCircuitBreaker(CircuitBreakerRegistry registry, CircuitBreakerProperties properties) {
            return registry.circuitBreaker("backendSharedC", properties.createCircuitBreakerConfigFromShared("default"));
        }
    }

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    CircuitBreakerProperties circuitBreakerProperties;

    @Autowired
    CircuitBreakerAspect circuitBreakerAspect;

    @Autowired
    @Qualifier("circuitBreakerDummyService")
    DummyService dummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void setUp() {
        // Need to clear this static registry out since multiple tests register collectors that could collide.
        CollectorRegistry.defaultRegistry.clear();
    }

    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the DummyService is invoked and
     * that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfiguration() throws IOException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();

        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyService.doSomething(false);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(DummyService.BACKEND);
        assertThat(circuitBreaker).isNotNull();

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);

        assertThat(circuitBreaker.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(6);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(2);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(70f);

        // Test Actuator endpoints

        ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate.getForEntity("/circuitbreaker", CircuitBreakerEndpointResponse.class);
        assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(5).containsExactly("backendA", "backendB", "backendSharedA", "backendSharedB", "backendSharedC");


        ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = restTemplate.getForEntity("/circuitbreaker/events", CircuitBreakerEventsEndpointResponse.class);
        assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

        assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordFailurePredicate().test(new RecordedException())).isTrue();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordFailurePredicate().test(new IgnoredException())).isFalse();

        // expect no health indicator for backendB, as it is disabled via properties
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/health", String.class);
        assertThat(healthResponse.getBody()).isNotNull();
        assertThat(healthResponse.getBody()).contains("backendACircuitBreaker");
        assertThat(healthResponse.getBody()).doesNotContain("backendBCircuitBreaker");

        // Verify that an exception for which recordFailurePredicate returns false and it is not included in
        // recordExceptions evaluates to false.
        assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordFailurePredicate().test(new Exception())).isFalse();

        assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);

        // expect all shared configs share the same values and are from the application.yml file
        CircuitBreaker sharedA = circuitBreakerRegistry.circuitBreaker("backendSharedA");
        CircuitBreaker sharedB = circuitBreakerRegistry.circuitBreaker("backendSharedB");
        CircuitBreaker sharedC = circuitBreakerRegistry.circuitBreaker("backendSharedC");
        assertThat(sharedA.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(100);
        assertThat(sharedA.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        assertThat(sharedA.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(60f);
        assertThat(sharedA.getCircuitBreakerConfig().getWaitDurationInOpenState()).isEqualByComparingTo(Duration.ofSeconds(10L));
        assertEquals(sharedA.getCircuitBreakerConfig().getRingBufferSizeInClosedState(), sharedB.getCircuitBreakerConfig().getRingBufferSizeInClosedState());
        assertEquals(sharedA.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState(), sharedB.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState());
        assertThat(sharedB.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(60f);
        assertEquals(sharedA.getCircuitBreakerConfig().getWaitDurationInOpenState(), sharedB.getCircuitBreakerConfig().getWaitDurationInOpenState());
        assertEquals(sharedA.getCircuitBreakerConfig().getRingBufferSizeInClosedState(), sharedC.getCircuitBreakerConfig().getRingBufferSizeInClosedState());
        assertEquals(sharedA.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState(), sharedC.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState());
        assertThat(sharedC.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(60f);
        assertEquals(sharedA.getCircuitBreakerConfig().getWaitDurationInOpenState(), sharedC.getCircuitBreakerConfig().getWaitDurationInOpenState());
    }
}
