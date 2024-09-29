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

import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.github.resilience4j.test.HelloWorldException;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static io.github.resilience4j.retry.utils.AsyncUtils.awaitResult;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CompletionStageRetryTest {

    private AsyncHelloWorldService helloWorldService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Before
    public void setUp() {
        helloWorldService = mock(AsyncHelloWorldService.class);
    }

    @Test
    public void shouldNotRetry() {
        given(helloWorldService.returnHelloWorld())
            .willReturn(completedFuture("Hello world"));
        Retry retryContext = Retry.ofDefaults("id");
        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retryContext,
            scheduler,
            () -> helloWorldService.returnHelloWorld());

        String result = awaitResult(supplier);

        then(helloWorldService).should().returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    public void shouldNotRetryWhenReturnVoid() {
        given(helloWorldService.sayHelloWorld())
            .willReturn(completedFuture(null));
        Retry retryContext = Retry.ofDefaults("id");

        Supplier<CompletionStage<Void>> supplier = Retry.decorateCompletionStage(
            retryContext,
            scheduler,
            () -> helloWorldService.sayHelloWorld());
        awaitResult(supplier);

        then(helloWorldService).should().sayHelloWorld();
    }

    @Test
    public void shouldNotRetryWithThatResult() {
        given(helloWorldService.returnHelloWorld())
            .willReturn(completedFuture("Hello world"));
        final RetryConfig retryConfig = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("NoRetry"))
            .maxAttempts(1)
            .build();
        Retry retryContext = Retry.of("id", retryConfig);

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retryContext,
            scheduler,
            () -> helloWorldService.returnHelloWorld());
        String result = awaitResult(supplier);

        then(helloWorldService).should().returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
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
        given(helloWorldService.returnHelloWorld())
            .willThrow(new IllegalArgumentException("BAM!"));
        Retry retry = Retry.ofDefaults("id");

        retry.executeCompletionStage(
            scheduler,
            () -> helloWorldService.returnHelloWorld());
    }

    @Test
    public void shouldRetryInCaseOfAnExceptionAtAsyncStage() {
        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new HelloWorldException());
        given(helloWorldService.returnHelloWorld())
            .willReturn(failedFuture)
            .willReturn(completedFuture("Hello world"));
        Retry retryContext = Retry.ofDefaults("id");

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retryContext,
            scheduler,
            () -> helloWorldService.returnHelloWorld());
        String result = awaitResult(supplier.get());

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    public void shouldThrowOnceMaxAttemptsReachedIfConfigured() {
        given(helloWorldService.returnHelloWorld())
            .willReturn(CompletableFuture.completedFuture("invalid response"));
        RetryConfig retryConfig = RetryConfig.<String>custom()
            .retryOnResult(s -> s.equals("invalid response"))
            .maxAttempts(3)
            .failAfterMaxAttempts(true)
            .build();

        Retry retry = Retry.of("retry", retryConfig);
        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retry,
            scheduler,
            helloWorldService::returnHelloWorld
        );

        assertThat(supplier.get())
            .failsWithin(5, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(MaxRetriesExceededException.class)
            .withMessage("Retry 'retry' has exhausted all attempts (3)");
        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void shouldStopRetryingAndEmitProperEventsIfIntervalFunctionReturnsLessThanZero() {
        given(helloWorldService.returnHelloWorld())
            .willReturn(CompletableFuture.failedFuture(new HelloWorldException("Exceptional!")));

        AtomicInteger numberOfTimesIntervalFunctionCalled = new AtomicInteger(0);
        RetryConfig retryConfig = RetryConfig.<String>custom()
            .intervalFunction((ignored) -> {
                int numTimesCalled = numberOfTimesIntervalFunctionCalled.incrementAndGet();
                return numTimesCalled > 1 ? -1L : 0L;
            })
            .maxAttempts(3)
            .build();

        AtomicInteger numberOfRetryEvents = new AtomicInteger();
        AtomicBoolean onErrorEventOccurred = new AtomicBoolean(false);

        Retry retry = Retry.of("retry", retryConfig);
        retry.getEventPublisher().onRetry((ignored) -> numberOfRetryEvents.getAndIncrement());
        retry.getEventPublisher().onError((ignored) -> onErrorEventOccurred.set(true));

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retry,
            scheduler,
            helloWorldService::returnHelloWorld
        );

        assertThat(supplier.get())
            .failsWithin(5, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(HelloWorldException.class)
            .withMessage("Exceptional!");
        assertThat(numberOfRetryEvents).hasValue(1);
        assertThat(onErrorEventOccurred).isTrue();
        then(helloWorldService).should(times(2)).returnHelloWorld();
    }

    @Test
    public void shouldContinueRetryingAndEmitProperEventsIfIntervalFunctionReturnsZeroOrMore() {
        given(helloWorldService.returnHelloWorld())
            .willReturn(CompletableFuture.failedFuture(new HelloWorldException("Exceptional!")));

        AtomicInteger numberOfTimesIntervalFunctionCalled = new AtomicInteger(0);
        RetryConfig retryConfig = RetryConfig.<String>custom()
            .intervalFunction((ignored) -> {
                // Returns 0, 1, 2
                return (long) numberOfTimesIntervalFunctionCalled.getAndIncrement();
            })
            .maxAttempts(3)
            .build();

        AtomicInteger numberOfRetryEvents = new AtomicInteger();
        AtomicBoolean onErrorEventOccurred = new AtomicBoolean(false);

        Retry retry = Retry.of("retry", retryConfig);
        retry.getEventPublisher().onRetry((ignored) -> numberOfRetryEvents.getAndIncrement());
        retry.getEventPublisher().onError((ignored) -> onErrorEventOccurred.set(true));

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retry,
            scheduler,
            helloWorldService::returnHelloWorld
        );

        assertThat(supplier.get())
            .failsWithin(5, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(HelloWorldException.class)
            .withMessage("Exceptional!");
        assertThat(numberOfRetryEvents).hasValue(2);
        assertThat(onErrorEventOccurred).isTrue();
        then(helloWorldService).should(times(3)).returnHelloWorld();
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
        given(helloWorldService.returnHelloWorld())
            .willReturn(failedFuture);
        Retry retryContext = Retry.of(
            "id",
            RetryConfig
                .custom()
                .maxAttempts(noOfAttempts)
                .build());
        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retryContext,
            scheduler,
            () -> helloWorldService.returnHelloWorld());

        Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

        then(helloWorldService).should(times(noOfAttempts)).returnHelloWorld();
        assertThat(resultTry.isFailure()).isTrue();
        assertThat(resultTry.getCause().getCause()).isInstanceOf(HelloWorldException.class);
    }

    private void shouldCompleteFutureAfterAttemptsInCaseOfRetyOnResultAtAsyncStage(int noOfAttempts,
                                                                                   String retryResponse) {
        given(helloWorldService.returnHelloWorld())
            .willReturn(completedFuture("Hello world"));
        Retry retryContext = Retry.of(
            "id",
            RetryConfig
                .<String>custom()
                .maxAttempts(noOfAttempts)
                .retryOnResult(s -> s.contains(retryResponse))
                .build());
        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retryContext,
            scheduler,
            () -> helloWorldService.returnHelloWorld());

        Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

        then(helloWorldService).should(times(noOfAttempts)).returnHelloWorld();
        assertThat(resultTry.isSuccess()).isTrue();
    }

    @Test
    public void shouldCompleteExceptionallyWhenRetryOnExPredicateThrows() {
        given(helloWorldService.returnHelloWorld())
            .willReturn(CompletableFuture.failedFuture(new HelloWorldException()));
        final RetryConfig retryConfig = RetryConfig.custom()
            .retryOnException(__ -> {
                throw new RuntimeException();
            })
            .build();
        Retry retryContext = Retry.of("id", retryConfig);

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(
            retryContext,
            scheduler,
            () -> helloWorldService.returnHelloWorld());
        Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

        then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(resultTry.isFailure()).isTrue();
        assertThat(resultTry.getCause().getCause()).isInstanceOf(RuntimeException.class);
    }

}
