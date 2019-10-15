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

public class FutureRetryTest {

	private AsyncHelloWorldService helloWorldService;
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Before
	public void setUp() {
		helloWorldService = Mockito.mock(AsyncHelloWorldService.class);
	}


	@Test
	public void shouldNotRetryFuture() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnHelloWorldFuture())
				.willReturn(completedFuture("Hello world"));
		// Create a Retry with default configuration
		Retry retryContext = Retry.ofDefaults("id");

		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateFuture(retryContext, scheduler,
				() -> helloWorldService.returnHelloWorldFuture());

		// When
		String result = awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldFuture();
		Assertions.assertThat(result).isEqualTo("Hello world");
	}

	@Test
	public void shouldNotRetryWhenReturnVoidFuture() {
		BDDMockito.given(helloWorldService.sayHelloWorldFuture())
				.willReturn(completedFuture(null));

		// Create a Retry with default configuration
		Retry retryContext = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<Void>> supplier = Retry.decorateFuture(
				retryContext,
				scheduler,
				() -> helloWorldService.sayHelloWorldFuture());

		// When
		awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorldFuture();
	}

	@Test
	public void shouldNotRetryWithThatResultFuture() {
		// Given the HelloWorldService returns Hello world
		BDDMockito.given(helloWorldService.returnHelloWorldFuture())
				.willReturn(completedFuture("Hello world"));
		// Create a Retry with default configuration
		final RetryConfig retryConfig = RetryConfig.<String>custom().retryOnResult(s -> s.contains("NoRetry"))
				.maxAttempts(1)
				.build();
		Retry retryContext = Retry.of("id", retryConfig);
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateFuture(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorldFuture());

		// When
		String result = awaitResult(supplier);
		// Then the helloWorldService should be invoked 1 time
		BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldFuture();
		Assertions.assertThat(result).isEqualTo("Hello world");
	}


	@Test
	public void shouldRetryInCaseOfResultRetryMatchAtSyncStageFuture() {
		shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStageFuture(1, "Hello world");
	}

	@Test
	public void shouldRetryTowAttemptsInCaseOResultRetryMatchAtSyncStageFuture() {
		shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStageFuture(2, "Hello world");
	}


	@Test(expected = IllegalArgumentException.class)
	public void shouldRethrowExceptionInCaseOfExceptionAtSyncStageFuture() {
		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorldFuture())
				.willThrow(new IllegalArgumentException("BAM!"));

		// Create a Retry with default configuration
		Retry retry = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		retry.executeFuture(scheduler,
				() -> helloWorldService.returnHelloWorldFuture());
	}

	@Test
	public void shouldRetryInCaseOfAnExceptionAtAsyncStageFuture() {
		CompletableFuture<String> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new HelloWorldException());

		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorldFuture())
				.willReturn(failedFuture)
				.willReturn(completedFuture("Hello world"));

		// Create a Retry with default configuration
		Retry retryContext = Retry.ofDefaults("id");
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateFuture(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorldFuture());

		// When
		String result = awaitResult(supplier.get());

		// Then the helloWorldService should be invoked 2 times
		BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorldFuture();
		Assertions.assertThat(result).isEqualTo("Hello world");
	}


	@Test
	public void shouldCompleteFutureAfterOneAttemptInCaseOfExceptionAtAsyncStageFuture() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStageFuture(1);
	}

	@Test
	public void shouldCompleteFutureAfterThreeAttemptsInCaseOfExceptionAtAsyncStageFuture() {
		shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStageFuture(3);
	}


	private void shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtAsyncStageFuture(int noOfAttempts) {
		CompletableFuture<String> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new HelloWorldException());

		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorldFuture())
				.willReturn(failedFuture);

		// Create a Retry with default configuration
		Retry retryContext = Retry.of(
				"id",
				RetryConfig
						.custom()
						.maxAttempts(noOfAttempts)
						.build());
		// Decorate the invocation of the HelloWorldService
		Supplier<CompletionStage<String>> supplier = Retry.decorateFuture(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorldFuture());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1 times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorldFuture();
		Assertions.assertThat(resultTry.isFailure()).isTrue();
		Assertions.assertThat(resultTry.getCause().getCause()).isInstanceOf(HelloWorldException.class);
	}

	private void shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStageFuture(int noOfAttempts,
																						 String retryResponse) {

		// Given the HelloWorldService throws an exception
		BDDMockito.given(helloWorldService.returnHelloWorldFuture())
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
		Supplier<CompletionStage<String>> supplier = Retry.decorateFuture(
				retryContext,
				scheduler,
				() -> helloWorldService.returnHelloWorldFuture());

		// When
		Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

		// Then the helloWorldService should be invoked n + 1 times
		BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorldFuture();
		Assert.assertTrue(resultTry.isSuccess());

	}
}
