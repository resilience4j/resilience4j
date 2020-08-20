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
import io.vavr.API;
import io.vavr.CheckedFunction0;
import io.vavr.Predicates;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import static io.vavr.API.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class SupplierVavrRetryTest {
    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        RetryImpl.setSleepFunction(sleep -> sleptTime += sleep);
    }

    @Test
    public void shouldNotRetryDecoratedTry() {
        given(helloWorldService.returnTry()).willReturn(Try.success("Hello world"));
        final RetryConfig tryAgain = RetryConfig.<String>custom()
            .maxAttempts(2).build();
        Retry retry = Retry.of("id", tryAgain);
        Try<String> result = VavrRetry.executeTrySupplier(retry, helloWorldService::returnTry);
        then(helloWorldService).should().returnTry();
        assertThat(result.get()).isEqualTo("Hello world");
    }

    @Test
    public void shouldReturnSuccessfullyAfterSecondAttempt() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Retry retry = Retry.ofDefaults("id");
        CheckedFunction0<String> retryableSupplier = VavrRetry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(retryableSupplier);

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
    }

    @Test
    public void shouldReturnAfterThreeAttempts() {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        Retry retry = Retry.ofDefaults("id");
        CheckedFunction0<String> retryableSupplier = VavrRetry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(retryableSupplier);

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
        CheckedFunction0<String> retryableSupplier = VavrRetry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(retryableSupplier);

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
        CheckedFunction0<String> retryableSupplier = VavrRetry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(retryableSupplier);

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
        CheckedFunction0<String> retryableSupplier = VavrRetry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(retryableSupplier)
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
        CheckedFunction0<String> retryableSupplier = VavrRetry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(retryableSupplier)
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
        CheckedFunction0<String> retryableSupplier = VavrRetry
            .decorateCheckedSupplier(retry, helloWorldService::returnHelloWorld);

        Try.of(retryableSupplier);

        then(helloWorldService).should(times(3)).returnHelloWorld();
        assertThat(sleptTime).isEqualTo(
            RetryConfig.DEFAULT_WAIT_DURATION +
                RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }
}
