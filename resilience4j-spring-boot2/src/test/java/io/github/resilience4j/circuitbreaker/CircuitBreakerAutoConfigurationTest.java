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

import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.ReactiveDummyService;
import io.github.resilience4j.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
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
	 * The test verifies that a CircuitBreaker instance is created and configured properly when the DummyService is invoked and
	 * that the CircuitBreaker records successful and failed calls.
	 */
	@Test
	@DirtiesContext
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

		// expect circuitbreaker is configured as defined in application.yml
		assertThat(circuitBreaker.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(6);
		assertThat(circuitBreaker.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(2);
		assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(70f);
		assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState()).isEqualByComparingTo(Duration.ofSeconds(5L));

		// expect circuitbreakers actuator endpoint contains both circuitbreakers
		ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate.getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
		assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(2).containsExactly("backendA", "backendB");

		// expect circuitbreaker-event actuator endpoint recorded both events
		ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents/backendA", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		// expect no health indicator for backendB, as it is disabled via properties
		ResponseEntity<HealthResponse> healthResponse = restTemplate.getForEntity("/actuator/health", HealthResponse.class);
		assertThat(healthResponse.getBody().getDetails()).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("backendACircuitBreaker")).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("backendBCircuitBreaker")).isNull();

		assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordFailurePredicate().test(new RecordedException())).isTrue();
		assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordFailurePredicate().test(new IgnoredException())).isFalse();

		// Verify that an exception for which recordFailurePredicate returns false and it is not included in
		// recordExceptions evaluates to false.
		assertThat(circuitBreaker.getCircuitBreakerConfig().getRecordFailurePredicate().test(new Exception())).isFalse();

		// expect aspect configured as defined in application.yml
		assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);
	}


	/**
	 * The test verifies that a CircuitBreaker instance is created and configured properly when the DummyService is invoked and
	 * that the CircuitBreaker records successful and failed calls.
	 */
	@Test
	@DirtiesContext
	public void testCircuitBreakerAutoConfigurationAsync() throws IOException, ExecutionException, InterruptedException {
		assertThat(circuitBreakerRegistry).isNotNull();
		assertThat(circuitBreakerProperties).isNotNull();

		try {
			dummyService.doSomethingAsync(true);
		} catch (IOException ex) {
			// Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		final CompletableFuture<String> stringCompletionStage = dummyService.doSomethingAsync(false);
		assertThat(stringCompletionStage.get()).isEqualTo("Test result");

		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(DummyService.BACKEND);
		assertThat(circuitBreaker).isNotNull();

		assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);

		// expect circuitbreakers actuator endpoint contains both circuitbreakers
		ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate.getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
		assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(2).containsExactly("backendA", "backendB");

		// expect circuitbreaker-event actuator endpoint recorded both events
		ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents/backendA", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		// expect no health indicator for backendB, as it is disabled via properties
		ResponseEntity<HealthResponse> healthResponse = restTemplate.getForEntity("/actuator/health", HealthResponse.class);
		assertThat(healthResponse.getBody().getDetails()).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("backendACircuitBreaker")).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("backendBCircuitBreaker")).isNull();


	}

	/**
	 * The test verifies that a CircuitBreaker instance is created and configured properly when the DummyService is invoked and
	 * that the CircuitBreaker records successful and failed calls.
	 */
	@Test
	@DirtiesContext
	public void testCircuitBreakerAutoConfigurationReactive() throws IOException {
		assertThat(circuitBreakerRegistry).isNotNull();
		assertThat(circuitBreakerProperties).isNotNull();

		try {
			reactiveDummyService.doSomethingFlux(true).subscribe(String::toUpperCase, throwable -> System.out.println("Exception received:" + throwable.getMessage()));
		} catch (IOException ex) {
			// Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		reactiveDummyService.doSomethingFlux(false).subscribe(String::toUpperCase, throwable -> System.out.println("Exception received:" + throwable.getMessage()));

		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(ReactiveDummyService.BACKEND);
		assertThat(circuitBreaker).isNotNull();


		// expect circuitbreaker is configured as defined in application.yml
		assertThat(circuitBreaker.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(10);
		assertThat(circuitBreaker.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(5);
		assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50f);
		assertThat(circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState()).isEqualByComparingTo(Duration.ofSeconds(5L));

		// expect circuitbreakers actuator endpoint contains both circuitbreakers
		ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate.getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
		assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(2).containsExactly("backendA", "backendB");

		// expect circuitbreaker-event actuator endpoint recorded both events
		ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents/backendB", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		// expect no health indicator for backendB, as it is disabled via properties
		ResponseEntity<HealthResponse> healthResponse = restTemplate.getForEntity("/actuator/health", HealthResponse.class);
		assertThat(healthResponse.getBody().getDetails()).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("backendACircuitBreaker")).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("backendBCircuitBreaker")).isNull();

		// expect aspect configured as defined in application.yml
		assertThat(circuitBreakerAspect.getOrder()).isEqualTo(400);
		assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
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
