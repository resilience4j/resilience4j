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

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.API;
import io.vavr.Predicates;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static io.github.resilience4j.retry.utils.AsyncUtils.awaitResult;
import static io.vavr.API.$;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class SupplierRetryTest {

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HelloWorldService helloWorldService;
    private AsyncHelloWorldService helloWorldServiceAsync;
    private long sleptTime = 0L;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        helloWorldServiceAsync = mock(AsyncHelloWorldService.class);
        RetryImpl.sleepFunction = sleep -> sleptTime += sleep;
    }

    @Test
    public void shouldNotRetry() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");
        Supplier<String> supplier = Retry
            .decorateSupplier(retry, helloWorldService::returnHelloWorld);

        String result = supplier.get();

        then(helloWorldService).should().returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldNotRetryWithResult() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("tryAgain"))
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);
        Supplier<String> supplier = Retry
            .decorateSupplier(retry, helloWorldService::returnHelloWorld);

        String result = supplier.get();
        then(helloWorldService).should().returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldDeprecatedOnSuccessCallOnFinish() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("tryAgain"))
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Supplier<String> supplier = decorateSupplierWithOnSuccess(retry,
            helloWorldService::returnHelloWorld);
        String result = supplier.get();
        then(helloWorldService).should().returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(0);
    }

    private <T> Supplier<T> decorateSupplierWithOnSuccess(Retry retry, Supplier<T> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                try {
                    T result = supplier.get();
                    final boolean validationOfResult = context.onResult(result);
                    if (!validationOfResult) {
                        context.onSuccess();
                        return result;
                    }
                } catch (RuntimeException runtimeException) {
                    context.onRuntimeError(runtimeException);
                }
            } while (true);
        };
    }

    @Test
    public void shouldRetryWithResult() {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("Hello world"))
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);
        Supplier<String> supplier = Retry
            .decorateSupplier(retry, helloWorldService::returnHelloWorld);

        String result = supplier.get();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    public void testDecorateSupplier() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");
        Supplier<String> supplier = Retry
            .decorateSupplier(retry, helloWorldService::returnHelloWorld);

        String result = supplier.get();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void testDecorateSupplierAndInvokeTwice() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world")
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");
        Supplier<String> supplier = Retry
            .decorateSupplier(retry, helloWorldService::returnHelloWorld);

        String result = supplier.get();
        String result2 = supplier.get();

        then(helloWorldService).should(times(4)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(result2).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(2);
    }

    @Test
    public void testDecorateCallable() throws Exception {
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");
        Callable<String> callable = Retry
            .decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

        String result = callable.call();

        then(helloWorldService).should(times(2)).returnHelloWorldWithException();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void testDecorateCallableWithRetryResult() throws Exception {
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("Hello world"))
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);
        Callable<String> callable = Retry
            .decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

        String result = callable.call();

        then(helloWorldService).should(times(2)).returnHelloWorldWithException();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void testExecuteCallable() throws Exception {
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");

        String result = retry.executeCallable(helloWorldService::returnHelloWorldWithException);

        then(helloWorldService).should(times(2)).returnHelloWorldWithException();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void testExecuteSupplier() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");

        String result = retry.executeSupplier(helloWorldService::returnHelloWorld);

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void testExecuteSupplierWithResult() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("Hello world"))
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        String result = retry.executeSupplier(helloWorldService::returnHelloWorld);

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void shouldReturnSuccessfullyAfterSecondAttempt() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");
        CheckedSupplier<String> retryableSupplier = Retry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(() -> retryableSupplier.get());

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void shouldReturnAfterThreeAttempts() {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        Retry retry = Retry.ofDefaults("id");
        CheckedSupplier<String> retryableSupplier = Retry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(() -> retryableSupplier.get());

        then(helloWorldService).should(times(3)).returnHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void shouldReturnAfterOneAttempt() {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        RetryConfig config = RetryConfig.custom().maxAttempts(1).build();
        Retry retry = Retry.of("id", config);
        CheckedSupplier<String> retryableSupplier = Retry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(() -> retryableSupplier.get());

        then(helloWorldService).should().returnHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldReturnAfterOneAttemptAndIgnoreException() {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        RetryConfig config = RetryConfig.custom()
            .retryOnException(throwable -> API.Match(throwable).of(
                API.Case($(Predicates.instanceOf(HelloWorldException.class)), false),
                API.Case($(), true)))
            .build();
        Retry retry = Retry.of("id", config);
        CheckedSupplier<String> retryableSupplier = Retry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(() -> retryableSupplier.get());

        // Because the exception should be rethrown immediately.
        then(helloWorldService).should().returnHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldReturnAfterThreeAttemptsAndRecover() {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        Retry retry = Retry.ofDefaults("id");
        CheckedSupplier<String> retryableSupplier = Retry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(() -> retryableSupplier.get())
            .recover((throwable) -> "Hello world from recovery function");
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);

        then(helloWorldService).should(times(3)).returnHelloWorld();
        assertThat(result.get()).isEqualTo("Hello world from recovery function");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void shouldReturnAfterThreeAttemptsAndRecoverWithResult() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world")
            .willThrow(new HelloWorldException());
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("Hello world"))
            .maxAttempts(3).build();
        Retry retry = Retry.of("id", tryAgain);
        CheckedSupplier<String> retryableSupplier = Retry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(() -> retryableSupplier.get())
            .recover((throwable) -> "Hello world from recovery function");

        then(helloWorldService).should(times(3)).returnHelloWorld();
        assertThat(result.get()).isEqualTo("Hello world from recovery function");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void shouldTakeIntoAccountBackoffFunction() {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        RetryConfig config = RetryConfig.custom()
            .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2.0))
            .build();
        Retry retry = Retry.of("id", config);
        CheckedSupplier<String> retryableSupplier = Retry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try.of(() -> retryableSupplier.get());

        then(helloWorldService).should(times(3)).returnHelloWorld();
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
        given(helloWorldServiceAsync.returnHelloWorld())
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
            () -> helloWorldServiceAsync.returnHelloWorld());

        Try<String> resultTry = Try.of(() -> awaitResult(supplier.get()));

        then(helloWorldServiceAsync).should(times(noOfAttempts)).returnHelloWorld();
        assertThat(resultTry.isSuccess()).isTrue();
    }
}
