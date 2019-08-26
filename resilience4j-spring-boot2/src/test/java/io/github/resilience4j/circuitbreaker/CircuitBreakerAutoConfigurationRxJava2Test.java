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
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpointResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class)
public class CircuitBreakerAutoConfigurationRxJava2Test {

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
	public void testCircuitBreakerAutoConfigurationReactiveRxJava2() throws IOException {
		assertThat(circuitBreakerRegistry).isNotNull();
		assertThat(circuitBreakerProperties).isNotNull();

		try {
			reactiveDummyService.doSomethingFlowable(true).blockingSubscribe(String::toUpperCase, throwable -> System.out.println("Exception received:" + throwable.getMessage()));
		} catch (Exception ex) {
			// Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		reactiveDummyService.doSomethingFlowable(false).blockingSubscribe(String::toUpperCase, throwable -> System.out.println("Exception received:" + throwable.getMessage()));

		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(ReactiveDummyService.BACKEND);
		assertThat(circuitBreaker).isNotNull();

		assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);

		// expect circuitbreakers actuator endpoint contains both circuitbreakers
		ResponseEntity<CircuitBreakerEndpointResponse> circuitBreakerList = restTemplate.getForEntity("/actuator/circuitbreakers", CircuitBreakerEndpointResponse.class);
		assertThat(circuitBreakerList.getBody().getCircuitBreakers()).hasSize(5).containsExactly("backendA", "backendB", "backendSharedA", "backendSharedB", "dummyFeignClient");

		// expect circuitbreaker-event actuator endpoint recorded both events
		ResponseEntity<CircuitBreakerEventsEndpointResponse> circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		circuitBreakerEventList = restTemplate.getForEntity("/actuator/circuitbreakerevents/backendB", CircuitBreakerEventsEndpointResponse.class);
		assertThat(circuitBreakerEventList.getBody().getCircuitBreakerEvents()).hasSize(2);

		// expect no health indicator for backendB, as it is disabled via properties
		ResponseEntity<CompositeHealthResponse> healthResponse = restTemplate.getForEntity("/actuator/health", CompositeHealthResponse.class);
		assertThat(healthResponse.getBody().getDetails()).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("circuitBreakers")).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("circuitBreakers").getDetails().get("backendA")).isNotNull();
		assertThat(healthResponse.getBody().getDetails().get("circuitBreakers").getDetails().get("backendB")).isNull();

		// Observable test
		try {
			reactiveDummyService.doSomethingObservable(true).blockingSubscribe(String::toUpperCase, Throwable::getCause);
		} catch (IOException ex) {
			// Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		reactiveDummyService.doSomethingObservable(false).blockingSubscribe(String::toUpperCase, Throwable::getCause);

		assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(4);
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);

		// Maybe test
		try {
			reactiveDummyService.doSomethingMaybe(true).blockingGet("goo");
		} catch (Exception ex) {
			// Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		reactiveDummyService.doSomethingMaybe(false).blockingGet();

		assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(6);
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(3);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);

		// single test
		try {
			reactiveDummyService.doSomethingSingle(true).blockingGet();
		} catch (Exception ex) {
			// Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		reactiveDummyService.doSomethingSingle(false).blockingGet();

		assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(8);
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(4);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(4);

		// Completable test

		try {
			reactiveDummyService.doSomethingCompletable(true).blockingAwait();
		} catch (Exception ex) {
			// Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		reactiveDummyService.doSomethingCompletable(false).blockingAwait();

		assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(10);
		assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(5);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(5);

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
