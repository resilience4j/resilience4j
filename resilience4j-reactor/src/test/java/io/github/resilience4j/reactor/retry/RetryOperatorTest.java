/*
 * Copyright 2017 Dan Maas
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

package io.github.resilience4j.reactor.retry;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


public class RetryOperatorTest {

	private HelloWorldService helloWorldService;

	@Before
	public void setUp() {
		helloWorldService = Mockito.mock(HelloWorldService.class);
	}

	@Test
	public void returnOnCompleteUsingMono() {
		//Given
		RetryConfig config = retryConfig();
		Retry retry = Retry.of("testName", config);
		RetryOperator<String> retryOperator = RetryOperator.of(retry);

		given(helloWorldService.returnHelloWorld())
				.willReturn("Hello world")
				.willThrow(new WebServiceException("BAM!"))
				.willThrow(new WebServiceException("BAM!"))
				.willReturn("Hello world");

		//When
		Mono.fromCallable(helloWorldService::returnHelloWorld).compose(retryOperator).block(Duration.ofMillis(100));
		Mono.fromCallable(helloWorldService::returnHelloWorld).compose(retryOperator).block(Duration.ofMillis(100));
		//Then
		BDDMockito.then(helloWorldService).should(Mockito.times(4)).returnHelloWorld();
		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
		assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
		assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
	}


	@Test(expected = StackOverflowError.class)
	public void shouldNotRetryUsingMonoStackOverFlow() {
		//Given
		RetryConfig config = retryConfig();
		Retry retry = Retry.of("testName", config);
		RetryOperator<String> retryOperator = RetryOperator.of(retry);

		given(helloWorldService.returnHelloWorld())
				.willThrow(new StackOverflowError("BAM!"));
		//When
		StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
				.compose(retryOperator))
				.expectSubscription()
				.expectError(StackOverflowError.class)
				.verify(Duration.ofMillis(50));

		//Then
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
	}

	@Test
	public void shouldNotRetryWhenItThrowErrorMono() {
		//Given
		RetryConfig config = retryConfig();
		Retry retry = Retry.of("testName", config);
		RetryOperator<String> retryOperator = RetryOperator.of(retry);

		given(helloWorldService.returnHelloWorld())
				.willThrow(new Error("BAM!"));

		//When
		StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
				.compose(retryOperator))
				.expectSubscription()
				.expectError(Error.class)
				.verify(Duration.ofMillis(50));
		//Then
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
	}


	@Test
	public void returnOnErrorUsingMono() {
		//Given
		RetryConfig config = retryConfig();
		Retry retry = Retry.of("testName", config);
		RetryOperator<String> retryOperator = RetryOperator.of(retry);

		given(helloWorldService.returnHelloWorld())
				.willThrow(new WebServiceException("BAM!"));

		//When
		StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
				.compose(retryOperator))
				.expectSubscription()
				.expectError(RetryExceptionWrapper.class)
				.verify(Duration.ofMillis(50));

		StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
				.compose(retryOperator))
				.expectSubscription()
				.expectError(RetryExceptionWrapper.class)
				.verify(Duration.ofMillis(50));

		//Then
		BDDMockito.then(helloWorldService).should(Mockito.times(6)).returnHelloWorld();
		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
	}

	@Test
	public void doNotRetryFromPredicateUsingMono() {
		//Given
		RetryConfig config = RetryConfig.custom()
				.retryOnException(t -> t instanceof IOException)
				.waitDuration(Duration.ofMillis(50))
				.maxAttempts(3).build();
		Retry retry = Retry.of("testName", config);
		given(helloWorldService.returnHelloWorld())
				.willThrow(new WebServiceException("BAM!"));

		//When
		StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
				.compose(RetryOperator.of(retry)))
				.expectSubscription()
				.expectError(RetryExceptionWrapper.class)
				.verify(Duration.ofMillis(50));
		//Then
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
		assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
	}

	@Test
	public void retryOnResultUsingMono() {
		//Given
		RetryConfig config = RetryConfig.<String>custom()
				.retryOnResult("retry"::equals)
				.waitDuration(Duration.ofMillis(50))
				.maxAttempts(3).build();
		Retry retry = Retry.of("testName", config);
		given(helloWorldService.returnHelloWorld())
				.willReturn("retry")
				.willReturn("success");

		//When
		StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
				.compose(RetryOperator.of(retry)))
				.expectSubscription()
				.expectNext("success")
				.expectComplete().verify(Duration.ofMillis(50));
		//Then
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
	}

	@Test
	public void retryOnResultFailAfterMaxAttemptsUsingMono() {
		//Given
		RetryConfig config = RetryConfig.<String>custom()
				.retryOnResult("retry"::equals)
				.waitDuration(Duration.ofMillis(50))
				.maxAttempts(3).build();
		Retry retry = Retry.of("testName", config);
		given(helloWorldService.returnHelloWorld())
				.willReturn("retry");

		//When
		StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
				.compose(RetryOperator.of(retry)))
				.expectSubscription()
				.expectNextCount(1)
				.expectComplete().verify(Duration.ofMillis(50));
		//Then
		BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
	}



	//Flux test

	@Test
	public void shouldFailWithExceptionFlux() {
		//Given
		RetryConfig config = retryConfig();
		Retry retry = Retry.of("testName", config);
		RetryOperator<Object> retryOperator = RetryOperator.of(retry);

		//When
		StepVerifier.create(Flux.error(new WebServiceException("BAM!")).compose(retryOperator))
				.expectSubscription()
				.expectError(RetryExceptionWrapper.class)
				.verify(Duration.ofMillis(50));
		//Then

		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
	}

	@Test
	public void retryOnResultUsingFlux() {
		//Given
		RetryConfig config = RetryConfig.<String>custom()
				.retryOnResult("retry"::equals)
				.waitDuration(Duration.ofMillis(50))
				.maxAttempts(3).build();
		Retry retry = Retry.of("testName", config);

		//When
		StepVerifier.create(Flux.just("retry", "success")
				.compose(RetryOperator.of(retry)))
				.expectSubscription()
				.expectNext("retry")
				.expectNext("success")
				.expectComplete().verify(Duration.ofMillis(50));
		//Then

		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
	}

	@Test
	public void retryOnResultFailAfterMaxAttemptsUsingFlux() {
		//Given
		RetryConfig config = RetryConfig.<String>custom()
				.retryOnResult("retry"::equals)
				.waitDuration(Duration.ofMillis(50))
				.maxAttempts(3).build();
		Retry retry = Retry.of("testName", config);

		//When
		StepVerifier.create(Flux.just("retry")
				.compose(RetryOperator.of(retry)))
				.expectSubscription()
				.expectNextCount(1)
				.expectComplete().verify(Duration.ofMillis(50));


		Retry.Metrics metrics = retry.getMetrics();

		assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
		assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
	}

	private RetryConfig retryConfig() {
		return RetryConfig.custom().waitDuration(Duration.ofMillis(50)).build();
	}
}
