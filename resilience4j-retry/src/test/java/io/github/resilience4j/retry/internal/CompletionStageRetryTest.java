/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.github.resilience4j.test.HelloWorldException;
import io.vavr.control.Try;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static io.github.resilience4j.retry.utils.AsyncUtils.awaitResult;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class CompletionStageRetryTest {

	private AsyncHelloWorldService helloWorldService;
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Before
	public void setUp() {
		helloWorldService = Mockito.mock(AsyncHelloWorldService.class);
	}

	@Test
	public void shouldNotRetry() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willReturn(completedFuture("Hello world"));
		// Create a Retry with default configuration
		Retry retryContext = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		Assertions.assertThat(result).isEqualTo("Hello world");
	}

	@Test
	public void shouldNotRetryWhenReturnVoid() {
		BDDMockito.given(helloWorldService.sayHelloWorld())
				.willReturn(completedFuture(null));

		// Create a Retry with default configuration
		Retry retryContext = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<Void>> supplier = Retry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.sayHelloWorld());

		// When
		awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
	}

	@Test
	public void shouldNotRetryWithThatResult(){
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willReturn(completedFuture("Hello world"));
		// Create a Retry with default configuration
		final RetryConfig retryConfig = RetryConfig.<String>custom().retryOnResult(s -> s.contains("NoRetry"))
				.maxAttempts(1)
				.build();
		Retry retryContext = Retry.of("id", retryConfig);
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		Assertions.assertThat(result).isEqualTo("Hello world");
	}

	@Test
	public void shouldRetryInCaseOResultRetryMatchAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(1, "Hello world");
	}

	@Test
	public void shouldRetryTowAttemptsInCaseOResultRetryMatchAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(2, "Hello world");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRethrowExceptionInCaseOfExceptionAtSyncStage() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willThrow(new IllegalArgumentException("BAM!"));

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		retry.executeCompletionStage(
				scheduler,
				() -> helloWorldService.returnHelloWorld());
	}

	@Test
	public void shouldRetryInCaseOfAnExceptionAtAsyncStage() {
		CompletableFuture<String> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new HelloWorldException());

		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willReturn(failedFuture)
				.willReturn(completedFuture("Hello world"));

		// Create a Retry with default configuration
		Retry retryContext = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		String result = awaitResult(supplier.get());

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		Assertions.assertThat(result).isEqualTo("Hello world");
	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(int noOfAttempts) {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willThrow(new HelloWorldException());

		// Create a Retry with default configuration
		Retry retryContext = Retry.of(
				"id",
				RetryConfig
						.custom()
						.maxAttempts(noOfAttempts)
						.build());
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1  times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
		Assertions.assertThat(resultTry.isFailure()).isTrue();
		Assertions.assertThat(resultTry.getCause().getCause()).isInstanceOf(HelloWorldException.class);
	}

	@Test
	public void shouldCompleteFutureAfterOneAttemptInCaseOfExceptionAtAsyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(1);
	}

	@Test
	public void shouldCompleteFutureAfterTwoAttemptsInCaseOfExceptionAtAsyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(2);
	}

	@Test
	public void shouldCompleteFutureAfterThreeAttemptsInCaseOfExceptionAtAsyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(3);
	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStage(int noOfAttempts) {
		CompletableFuture<String> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new HelloWorldException());

		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willReturn(failedFuture);

		// Create a Retry with default configuration
		Retry retryContext = Retry.of(
				"id",
				RetryConfig
						.custom()
						.maxAttempts(noOfAttempts)
						.build());
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1 times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
		Assertions.assertThat(resultTry.isFailure()).isTrue();
		Assertions.assertThat(resultTry.getCause().getCause()).isInstanceOf(HelloWorldException.class);
	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(int noOfAttempts,
	                                                                               String retryResponse) {

		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willReturn(completedFuture("Hello world"));


		// Create a Retry with default configuration
		Retry retryContext = Retry.of(
				"id",
				RetryConfig
						.<String>custom()
						.maxAttempts(noOfAttempts)
						.retryOnResult(s -> s.contains(retryResponse))
						.build());
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorld());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1 times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
		Assert.assertTrue(resultTry.isSuccess());

	}

}
