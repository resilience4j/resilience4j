/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry;

import static io.github.resilience4j.service.test.retry.ReactiveRetryDummyService.BACKEND_C;
import static io.github.resilience4j.service.test.retry.RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME;
import static io.github.resilience4j.service.test.retry.RetryDummyService.RETRY_BACKEND_A;
import static io.github.resilience4j.service.test.retry.RetryDummyService.RETRY_BACKEND_B;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.service.test.retry.RetryDummyFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.github.resilience4j.circuitbreaker.IgnoredException;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEndpointResponse;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventsEndpointResponse;
import io.github.resilience4j.retry.autoconfigure.RetryProperties;
import io.github.resilience4j.retry.configure.RetryAspect;
import io.github.resilience4j.service.test.TestApplication;
import io.github.resilience4j.service.test.retry.RetryDummyService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class)
public class RetryAutoConfigurationTest {

	@Autowired
	RetryRegistry retryRegistry;

	@Autowired
	RetryProperties retryProperties;

	@Autowired
	RetryAspect retryAspect;

	@Autowired
	RetryDummyService retryDummyService;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private RetryDummyFeignClient retryDummyFeignClient;

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(8090);

	/**
	 * This test verifies that the combination of @FeignClient and @Retry annotation works as same as @Retry alone works with any normal service class
	 */
	@Test
	@DirtiesContext
	public void testFeignClient() {

		WireMock.stubFor(WireMock
				.get(WireMock.urlEqualTo("/retry/"))
				.willReturn(WireMock.aResponse().withStatus(200).withBody("This is successful call"))
		);
		WireMock.stubFor(WireMock.get(WireMock.urlMatching("^.*\\/retry\\/error.*$"))
				.willReturn(WireMock.aResponse().withStatus(400).withBody("This is error")));


		assertThat(retryRegistry).isNotNull();
		assertThat(retryProperties).isNotNull();

		try {
			retryDummyFeignClient.doSomething("error");
		} catch (Exception ex) {
			// Do nothing. The IOException is recorded by the retry as it is one of failure exceptions
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		retryDummyFeignClient.doSomething(StringUtils.EMPTY);

		Retry retry = retryRegistry.retry(RETRY_DUMMY_FEIGN_CLIENT_NAME);
		assertThat(retry).isNotNull();

		// expect retry is configured as defined in application.yml
		assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
		assertThat(retry.getName()).isEqualTo(RETRY_DUMMY_FEIGN_CLIENT_NAME);
		assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();

		assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
		assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
		assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
		assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);

		// expect retry actuator endpoint contains both retries
		ResponseEntity<RetryEndpointResponse> retriesList = restTemplate.getForEntity("/actuator/retries", RetryEndpointResponse.class);
		assertThat(retriesList.getBody().getRetries()).hasSize(4).containsOnly(RETRY_BACKEND_A, RETRY_BACKEND_B, BACKEND_C, RETRY_DUMMY_FEIGN_CLIENT_NAME);

		// expect retry-event actuator endpoint recorded both events
		ResponseEntity<RetryEventsEndpointResponse> retryEventList = restTemplate.getForEntity("/actuator/retryevents", RetryEventsEndpointResponse.class);
		assertThat(retryEventList.getBody().getRetryEvents()).hasSize(3);

		retryEventList = restTemplate.getForEntity("/actuator/retryevents/" + RETRY_DUMMY_FEIGN_CLIENT_NAME, RetryEventsEndpointResponse.class);
		assertThat(retryEventList.getBody().getRetryEvents()).hasSize(3);

		assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();
		assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IgnoredException())).isFalse();
		assertThat(retryAspect.getOrder()).isEqualTo(399);
	}

	/**
	 * The test verifies that a Retry instance is created and configured properly when the RetryDummyService is invoked and
	 * that the Retry logic is properly handled
	 */
	@Test
	@DirtiesContext
	public void testRetryAutoConfiguration() throws IOException {
		assertThat(retryRegistry).isNotNull();
		assertThat(retryProperties).isNotNull();

		try {
			retryDummyService.doSomething(true);
		} catch (IOException ex) {
			// Do nothing. The IOException is recorded by the retry as it is one of failure exceptions
		}
		// The invocation is recorded by the CircuitBreaker as a success.
		retryDummyService.doSomething(false);

		Retry retry = retryRegistry.retry(RETRY_BACKEND_A);
		assertThat(retry).isNotNull();

		// expect retry is configured as defined in application.yml
		assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
		assertThat(retry.getName()).isEqualTo(RETRY_BACKEND_A);
		assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();

		assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
		assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
		assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
		assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);

		// expect retry actuator endpoint contains both retries
		ResponseEntity<RetryEndpointResponse> retriesList = restTemplate.getForEntity("/actuator/retries", RetryEndpointResponse.class);
		assertThat(retriesList.getBody().getRetries()).hasSize(4).containsOnly(RETRY_BACKEND_A, RETRY_BACKEND_B, BACKEND_C, RETRY_DUMMY_FEIGN_CLIENT_NAME);

		// expect retry-event actuator endpoint recorded both events
		ResponseEntity<RetryEventsEndpointResponse> retryEventList = restTemplate.getForEntity("/actuator/retryevents", RetryEventsEndpointResponse.class);
		assertThat(retryEventList.getBody().getRetryEvents()).hasSize(3);

		retryEventList = restTemplate.getForEntity("/actuator/retryevents/" + RETRY_BACKEND_A, RetryEventsEndpointResponse.class);
		assertThat(retryEventList.getBody().getRetryEvents()).hasSize(3);

		assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();
		assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IgnoredException())).isFalse();
		assertThat(retryAspect.getOrder()).isEqualTo(399);
	}
}
