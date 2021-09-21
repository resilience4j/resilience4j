/*
 *
 *  Copyright 2020: KrnSaurabh
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
package io.github.resilience4j.retry;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.internal.RetryImpl;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedRunnable;
import io.vavr.Predicates;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static io.vavr.API.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class VavrRetryTest {

    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        RetryImpl.setSleepFunction(sleep -> sleptTime += sleep);
    }

    @Test
    public void shouldReturnAfterThreeAttempts() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();
        Retry retry = Retry.ofDefaults("id");
        CheckedRunnable retryableRunnable = VavrRetry
            .decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        Try<Void> result = Try.run(retryableRunnable);

        then(helloWorldService).should(times(3)).sayHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void shouldReturnAfterOneAttempt() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();
        RetryConfig config = RetryConfig.custom().maxAttempts(1).build();
        Retry retry = Retry.of("id", config);
        CheckedRunnable retryableRunnable = VavrRetry
            .decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        Try<Void> result = Try.run(retryableRunnable);

        then(helloWorldService).should().sayHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldReturnAfterOneAttemptAndIgnoreException() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();
        RetryConfig config = RetryConfig.custom()
            .retryOnException(throwable -> Match(throwable).of(
                Case($(Predicates.instanceOf(HelloWorldException.class)), false),
                Case($(), true)))
            .build();
        Retry retry = Retry.of("id", config);
        CheckedRunnable retryableRunnable = VavrRetry
            .decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        Try<Void> result = Try.run(retryableRunnable);

        // because the exception should be rethrown immediately
        then(helloWorldService).should().sayHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldTakeIntoAccountBackoffFunction() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();
        RetryConfig config = RetryConfig
            .custom()
            .intervalFunction(IntervalFunction.of(Duration.ofMillis(500), x -> x * x))
            .build();
        Retry retry = Retry.of("id", config);
        CheckedRunnable retryableRunnable = VavrRetry
            .decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        Try.run(retryableRunnable);

        then(helloWorldService).should(times(3)).sayHelloWorld();
        assertThat(sleptTime).isEqualTo(
            RetryConfig.DEFAULT_WAIT_DURATION +
                RetryConfig.DEFAULT_WAIT_DURATION * RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void shouldNotRetryDecoratedEither() {
        given(helloWorldService.returnEither()).willReturn(Either.right("Hello world"));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Either<HelloWorldException, String> result = VavrRetry
            .executeEitherSupplier(retry, helloWorldService::returnEither);

        then(helloWorldService).should().returnEither();
        assertThat(result.get()).isEqualTo("Hello world");
    }

    @Test
    public void shouldRetryDecoratedTry() {
        given(helloWorldService.returnTry()).willReturn(Try.success("Hello world"));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("Hello world"))
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Try<String> result = VavrRetry.executeTrySupplier(retry, helloWorldService::returnTry);

        then(helloWorldService).should(times(2)).returnTry();
        assertThat(result.get()).isEqualTo("Hello world");
    }

    @Test
    public void shouldRetryDecoratedEither() {
        given(helloWorldService.returnEither()).willReturn(Either.right("Hello world"));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .retryOnResult(s -> s.contains("Hello world"))
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Either<HelloWorldException, String> result = VavrRetry
            .executeEitherSupplier(retry, helloWorldService::returnEither);

        then(helloWorldService).should(times(2)).returnEither();
        assertThat(result.get()).isEqualTo("Hello world");
    }

    @Test
    public void shouldFailToRetryDecoratedTry() {
        given(helloWorldService.returnTry()).willReturn(Try.failure(new HelloWorldException()));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Try<String> result = VavrRetry.executeTrySupplier(retry, helloWorldService::returnTry);

        then(helloWorldService).should(times(2)).returnTry();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(HelloWorldException.class);
    }

    @Test
    public void shouldFailToRetryDecoratedEither() {
        given(helloWorldService.returnEither()).willReturn(Either.left(new HelloWorldException()));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Either<HelloWorldException, String> result = VavrRetry
            .executeEitherSupplier(retry, helloWorldService::returnEither);

        then(helloWorldService).should(times(2)).returnEither();
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(HelloWorldException.class);
    }

    @Test
    public void shouldIgnoreExceptionOfDecoratedTry() {
        given(helloWorldService.returnTry()).willReturn(Try.failure(new HelloWorldException()));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .ignoreExceptions(HelloWorldException.class)
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Try<String> result = VavrRetry.executeTrySupplier(retry, helloWorldService::returnTry);

        then(helloWorldService).should().returnTry();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(HelloWorldException.class);
    }

    @Test
    public void shouldIgnoreExceptionOfDecoratedEither() {
        given(helloWorldService.returnEither()).willReturn(Either.left(new HelloWorldException()));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .ignoreExceptions(HelloWorldException.class)
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);

        Either<HelloWorldException, String> result = VavrRetry
            .executeEitherSupplier(retry, helloWorldService::returnEither);

        then(helloWorldService).should().returnEither();
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(HelloWorldException.class);
    }
}
