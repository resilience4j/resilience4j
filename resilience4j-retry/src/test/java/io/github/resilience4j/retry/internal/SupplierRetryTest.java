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

import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.API;
import io.vavr.CheckedFunction0;
import io.vavr.Predicates;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static io.github.resilience4j.retry.utils.AsyncUtils.awaitResult;
import static io.vavr.API.$;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

public class SupplierRetryTest {
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private HelloWorldService helloWorldService;
	private AsyncHelloWorldService helloWorldServiceAsync;
	private long sleptTime = 0L;

	@Before
	public void setUp() {
		helloWorldService = Mockito.mock(HelloWorldService.class);
		helloWorldServiceAsync = Mockito.mock(AsyncHelloWorldService.class);
		RetryImpl.sleepFunction = sleep -> sleptTime += sleep;
	}

	@Test
	public void shouldNotRetry() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		String result = supplier.get();
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(0);
	}

	@Test
	public void shouldNotRetryWithResult() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom().retryOnResult(s -> s.contains("tryAgain"))
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);
		// When
		String result = supplier.get();
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(0);
	}

	@Test
	public void shouldRetryWithResult() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom().retryOnResult(s -> s.contains("Hello world"))
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);
		// When
		String result = supplier.get();
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		assertThat(result).isEqualTo("Hello world");
	}

	@Test
	public void shouldNotRetryDecoratedTry() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnTry()).willReturn(Try.success("Hello world"));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Try<String> result = retry.executeTrySupplier(helloWorldService::returnTry);

		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnTry();
		assertThat(result.get()).isEqualTo("Hello world");
	}

	@Test
	public void shouldNotRetryDecoratedEither() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnEither()).willReturn(Either.right("Hello world"));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Either<HelloWorldException, String> result = retry.executeEitherSupplier(helloWorldService::returnEither);

		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnEither();
		assertThat(result.get()).isEqualTo("Hello world");
	}

	@Test
	public void shouldRetryDecoratedTry() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnTry()).willReturn(Try.success("Hello world"));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.retryOnResult(s -> s.contains("Hello world"))
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Try<String> result = retry.executeTrySupplier(helloWorldService::returnTry);

		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnTry();
		assertThat(result.get()).isEqualTo("Hello world");
	}

	@Test
	public void shouldRetryDecoratedEither() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnEither()).willReturn(Either.right("Hello world"));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.retryOnResult(s -> s.contains("Hello world"))
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Either<HelloWorldException, String> result = retry.executeEitherSupplier(helloWorldService::returnEither);

		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnEither();
		assertThat(result.get()).isEqualTo("Hello world");
	}

	@Test
	public void shouldFailToRetryDecoratedTry() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnTry()).willReturn(Try.failure(new HelloWorldException()));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Try<String> result = retry.executeTrySupplier(helloWorldService::returnTry);

		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnTry();
		assertThat(result.isFailure()).isTrue();
		assertThat(result.getCause()).isInstanceOf(HelloWorldException.class);
	}

	@Test
	public void shouldFailToRetryDecoratedEither() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnEither()).willReturn(Either.left(new HelloWorldException()));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Either<HelloWorldException, String> result = retry.executeEitherSupplier(helloWorldService::returnEither);

		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnEither();
		assertThat(result.isLeft()).isTrue();
		assertThat(result.getLeft()).isInstanceOf(HelloWorldException.class);
	}

	@Test
	public void shouldIgnoreExceptionOfDecoratedTry() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnTry()).willReturn(Try.failure(new HelloWorldException()));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.ignoreExceptions(HelloWorldException.class)
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Try<String> result = retry.executeTrySupplier(helloWorldService::returnTry);

		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnTry();
		assertThat(result.isFailure()).isTrue();
		assertThat(result.getCause()).isInstanceOf(HelloWorldException.class);
	}

	@Test
	public void shouldIgnoreExceptionOfDecoratedEither() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnEither()).willReturn(Either.left(new HelloWorldException()));
		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom()
				.ignoreExceptions(HelloWorldException.class)
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Either<HelloWorldException, String> result = retry.executeEitherSupplier(helloWorldService::returnEither);

		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnEither();
		assertThat(result.isLeft()).isTrue();
		assertThat(result.getLeft()).isInstanceOf(HelloWorldException.class);
	}

	@Test
	public void testDecorateSupplier() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		String result = supplier.get();

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
	}

	@Test
	public void testDecorateSupplierAndInvokeTwice() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld())
				.willThrow(new HelloWorldException())
				.willReturn("Hello world")
				.willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		String result = supplier.get();
		String result2 = supplier.get();

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(4)).returnHelloWorld();
		assertThat(result).isEqualTo("Hello world");
		assertThat(result2).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
		assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(2);
	}

	@Test
	public void testDecorateCallable() throws Exception {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Callable<String> callable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

		// When
		String result = callable.call();

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorldWithException();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
	}

	@Test
	public void testDecorateCallableWithRetryResult() throws Exception {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom().retryOnResult(s -> s.contains("Hello world"))
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		Callable<String> callable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

		// When
		String result = callable.call();

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorldWithException();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
	}

	@Test
	public void testExecuteCallable() throws Exception {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService

		String result = retry.executeCallable(helloWorldService::returnHelloWorldWithException);

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorldWithException();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
	}


	@Test
	public void testExecuteSupplier() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService

		String result = retry.executeSupplier(helloWorldService::returnHelloWorld);

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
	}

	@Test
	public void testExecuteSupplierWithResult() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom().retryOnResult(s -> s.contains("Hello world"))
				.maxAttempts(2).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService

		String result = retry.executeSupplier(helloWorldService::returnHelloWorld);

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		assertThat(result).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
	}

	@Test
	public void shouldReturnSuccessfullyAfterSecondAttempt() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException())
				.willReturn("Hello world");

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		CheckedFunction0<String> retryableSupplier = Retry
				.decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		Try<String> result = Try.of(retryableSupplier);

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
		assertThat(result.get()).isEqualTo("Hello world");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
	}

	@Test
	public void shouldReturnAfterThreeAttempts() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		CheckedFunction0<String> retryableSupplier = Retry
				.decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		Try<String> result = Try.of(retryableSupplier);

		// Then the helloWorldService should be invoked 3 times
		BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
		// and the result should be a failure
		assertThat(result.isFailure()).isTrue();
		// and the returned exception should be of type RuntimeException
		assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
	}

	@Test
	public void shouldReturnAfterOneAttempt() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());

		// Create a Retry with custom configuration
		RetryConfig config = RetryConfig.custom().maxAttempts(1).build();
		Retry retry = Retry.of("id", config);
		// Decorate the invocation of the HelloWorldService
		CheckedFunction0<String> retryableSupplier = Retry
				.decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		Try<String> result = Try.of(retryableSupplier);

		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		// and the result should be a failure
		assertThat(result.isFailure()).isTrue();
		// and the returned exception should be of type RuntimeException
		assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
		assertThat(sleptTime).isEqualTo(0);
	}

	@Test
	public void shouldReturnAfterOneAttemptAndIgnoreException() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());

		// Create a Retry with default configuration
		RetryConfig config = RetryConfig.custom()
				.retryOnException(throwable -> API.Match(throwable).of(
						API.Case($(Predicates.instanceOf(HelloWorldException.class)), false),
						API.Case($(), true)))
				.build();
		Retry retry = Retry.of("id", config);
		// Decorate the invocation of the HelloWorldService
		CheckedFunction0<String> retryableSupplier = Retry
				.decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		Try<String> result = Try.of(retryableSupplier);

		// Then the helloWorldService should be invoked only once, because the exception should be rethrown immediately.
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
		// and the result should be a failure
		assertThat(result.isFailure()).isTrue();
		// and the returned exception should be of type RuntimeException
		assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
		assertThat(sleptTime).isEqualTo(0);
	}

	@Test
	public void shouldReturnAfterThreeAttemptsAndRecover() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		CheckedFunction0<String> retryableSupplier = Retry
				.decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		Try<String> result = Try.of(retryableSupplier).recover((throwable) -> "Hello world from recovery function");
		assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);

		// Then the helloWorldService should be invoked 3 times
		BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();

		// and the returned exception should be of type RuntimeException
		assertThat(result.get()).isEqualTo("Hello world from recovery function");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
	}

	@Test
	public void shouldReturnAfterThreeAttemptsAndRecoverWithResult() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException())
				.willReturn("Hello world")
				.willThrow(new HelloWorldException());

		// Create a Retry with default configuration
		final RetryConfig tryAgain = RetryConfig.<String>custom().retryOnResult(s -> s.contains("Hello world"))
				.maxAttempts(3).build();
		Retry retry = Retry.of("id", tryAgain);
		// Decorate the invocation of the HelloWorldService
		CheckedFunction0<String> retryableSupplier = Retry
				.decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		Try<String> result = Try.of(retryableSupplier).recover((throwable) -> "Hello world from recovery function");

		// Then the helloWorldService should be invoked 3 times
		BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();

		// and the returned exception should be of type RuntimeException
		assertThat(result.get()).isEqualTo("Hello world from recovery function");
		assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
	}

	@Test
	public void shouldTakeIntoAccountBackoffFunction() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());

		// Create a Retry with a backoff function doubling the interval
		RetryConfig config = RetryConfig.custom().intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2.0))
				.build();
		Retry retry = Retry.of("id", config);
		// Decorate the invocation of the HelloWorldService
		CheckedFunction0<String> retryableSupplier = Retry
				.decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

		// When
		Try<String> result = Try.of(retryableSupplier);

		// Then the slept time should be according to the backoff function
		BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
		assertThat(sleptTime).isEqualTo(
				RetryConfig.DEFAULT_WAIT_DURATION +
						RetryConfig.DEFAULT_WAIT_DURATION * 2);
	}

	@Test
	public void shouldRetryInCaseOResultRetryMatchAtSyncStage() {
		shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(1, "Hello world");
	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(int noOfAttempts,
	                                                                               String retryResponse) {

		// Given the HelloWorldService return
		BDDMockito.given(helloWorldServiceAsync.returnHelloWorld())
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
				() -> helloWorldServiceAsync.returnHelloWorld());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1 times
		BDDMockito.then(helloWorldServiceAsync).should(Mockito.times(noOfAttempts)).returnHelloWorld();
		Assert.assertTrue(resultTry.isSuccess());

	}
}
