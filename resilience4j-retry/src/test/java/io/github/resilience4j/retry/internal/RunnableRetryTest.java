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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedRunnable;
import io.vavr.Predicates;
import io.vavr.control.Try;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.time.Duration;

import static io.vavr.API.*;

public class RunnableRetryTest {

    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp(){
        helloWorldService = Mockito.mock(HelloWorldService.class);
        RetryImpl.sleepFunction = sleep -> sleptTime += sleep;
    }

    @Test
    public void shouldNotRetry() {
        // Create a Retry with default configuration
        Retry retryContext = Retry.ofDefaults("id");

        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        runnable.run();
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void testDecorateRunnable() {
        // Given the HelloWorldService throws an exception
        BDDMockito.willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();

        // Create a Retry with default configuration
        Retry retry = Retry.ofDefaults("id");
        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(runnable::run);

        // Then the helloWorldService should be invoked 3 times
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).sayHelloWorld();
        // and the result should be a failure
        Assertions.assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        Assertions.assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        Assertions.assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION*2);
    }

    @Test
    public void testExecuteRunnable() {
        // Create a Retry with default configuration
        Retry retry = Retry.ofDefaults("id");
        // Decorate the invocation of the HelloWorldService
        retry.executeRunnable(helloWorldService::sayHelloWorld);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldReturnAfterThreeAttempts() {
        // Given the HelloWorldService throws an exception
        BDDMockito.willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();

        // Create a Retry with default configuration
        Retry retry = Retry.ofDefaults("id");
        // Decorate the invocation of the HelloWorldService
        CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked 3 times
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).sayHelloWorld();
        // and the result should be a failure
        Assertions.assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        Assertions.assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        Assertions.assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION*2);
    }

    @Test
    public void shouldReturnAfterOneAttempt() {
        // Given the HelloWorldService throws an exception
        BDDMockito.willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();

        // Create a Retry with default configuration
        RetryConfig config = RetryConfig.custom().maxAttempts(1).build();
        Retry retry = Retry.of("id", config);
        // Decorate the invocation of the HelloWorldService
        CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
        // and the result should be a failure
        Assertions.assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        Assertions.assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        Assertions.assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldReturnAfterOneAttemptAndIgnoreException() {
        // Given the HelloWorldService throws an exception
        BDDMockito.willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();

        // Create a Retry with default configuration
        RetryConfig config = RetryConfig.custom()
                .retryOnException(throwable -> Match(throwable).of(
                        Case($(Predicates.instanceOf(HelloWorldException.class)), false),
                        Case($(), true)))
                .build();
        Retry retry = Retry.of("id", config);

        // Decorate the invocation of the HelloWorldService
        CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked only once, because the exception should be rethrown immediately.
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
        // and the result should be a failure
        Assertions.assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        Assertions.assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        Assertions.assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldTakeIntoAccountBackoffFunction() {
        // Given the HelloWorldService throws an exception
        BDDMockito.willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();

        // Create a Retry with a backoff function squaring the interval
        RetryConfig config = RetryConfig
                .custom()
                .intervalFunction(IntervalFunction.of(Duration.ofMillis(500), x -> x * x))
                .build();

        Retry retry = Retry.of("id", config);
        // Decorate the invocation of the HelloWorldService
        CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the slept time should be according to the backoff function
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(
                RetryConfig.DEFAULT_WAIT_DURATION +
                        RetryConfig.DEFAULT_WAIT_DURATION*RetryConfig.DEFAULT_WAIT_DURATION);
    }
}
