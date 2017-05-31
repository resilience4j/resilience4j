package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.vavr.control.Try;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import javax.xml.ws.WebServiceException;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static io.github.resilience4j.retry.utils.AsyncUtils.awaitResult;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class AsyncRetryTest {

    private AsyncHelloWorldService helloWorldService;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Before
    public void setUp(){
        helloWorldService = Mockito.mock(AsyncHelloWorldService.class);
    }

    @Test
    public void shouldNotRetry() throws InterruptedException, ExecutionException, TimeoutException {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willReturn(completedFuture("Hello world"));
        // Create a Retry with default configuration
        AsyncRetry retryContext = AsyncRetry.ofDefaults("id");
        // Decorate the invocation of the HelloWorldService
        Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
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
    public void shouldRetryInCaseOfExceptionAtSyncStage() {
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"))
                .willReturn(completedFuture("Hello world"));

        // Create a Retry with default configuration
        AsyncRetry retryContext = AsyncRetry.ofDefaults("id");
        // Decorate the invocation of the HelloWorldService
        Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
                retryContext,
                scheduler,
                () -> helloWorldService.returnHelloWorld());

        // When
        String result = awaitResult(supplier.get());

        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        Assertions.assertThat(result).isEqualTo("Hello world");
    }

    @Test
    public void shouldRetryInCaseOfAnExceptionAtAsyncStage() {
        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new WebServiceException("BAM!"));

        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willReturn(failedFuture)
                .willReturn(completedFuture("Hello world"));

        // Create a Retry with default configuration
        AsyncRetry retryContext = AsyncRetry.ofDefaults("id");
        // Decorate the invocation of the HelloWorldService
        Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
                retryContext,
                scheduler,
                () -> helloWorldService.returnHelloWorld());

        // When
        String result = awaitResult(supplier.get());

        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        Assertions.assertThat(result).isEqualTo("Hello world");
    }

    @Test
    public void shouldCompleteFutureAfterOneAttemptInCaseOfExceptionAtSyncStage() {
        shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(1);
    }

    @Test
    public void shouldCompleteFutureAfterTwoAttemptsInCaseOfExceptionAtSyncStage() {
        shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(2);
    }

    @Test
    public void shouldCompleteFutureAfterThreeAttemptsInCaseOfExceptionAtSyncStage() {
        shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(3);
    }

    private void shouldCompleteFutureAfterAttemptsInCaseOfExceptionAtSyncStage(int noOfAttempts) {
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        // Create a Retry with default configuration
        AsyncRetry retryContext = AsyncRetry.of(
                "id",
                RetryConfig
                        .custom()
                        .maxAttempts(noOfAttempts)
                        .build());
        // Decorate the invocation of the HelloWorldService
        Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
                retryContext,
                scheduler,
                () -> helloWorldService.returnHelloWorld());

        // When
        Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

        // Then the helloWorldService should be invoked n + 1  times
        BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
        Assertions.assertThat(resultTry.isFailure()).isTrue();
        Assertions.assertThat(resultTry.getCause().getCause()).isInstanceOf(WebServiceException.class);
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
        failedFuture.completeExceptionally(new WebServiceException("BAM!"));

        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willReturn(failedFuture);

        // Create a Retry with default configuration
        AsyncRetry retryContext = AsyncRetry.of(
                "id",
                RetryConfig
                        .custom()
                        .maxAttempts(noOfAttempts)
                        .build());
        // Decorate the invocation of the HelloWorldService
        Supplier<CompletionStage<String>> supplier = AsyncRetry.decorateCompletionStage(
                retryContext,
                scheduler,
                () -> helloWorldService.returnHelloWorld());

        // When
        Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

        // Then the helloWorldService should be invoked n + 1 times
        BDDMockito.then(helloWorldService).should(Mockito.times(noOfAttempts)).returnHelloWorld();
        Assertions.assertThat(resultTry.isFailure()).isTrue();
        Assertions.assertThat(resultTry.getCause().getCause()).isInstanceOf(WebServiceException.class);
    }

}
