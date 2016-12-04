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
package io.github.robwin.retry;

import io.github.robwin.test.HelloWorldService;
import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import javax.xml.ws.WebServiceException;

import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;
import static org.assertj.core.api.BDDAssertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class RunnableRetryTest {

    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
        RetryContext.sleepFunction = sleep -> sleptTime += sleep;
    }

    @Test
    public void shouldNotRetry() {
        // Create a Retry with default configuration
        Retry retryContext = Retry.ofDefaults();
        // Decorate the invocation of the HelloWorldService
        Try.CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
        // and the result should be a success
        assertThat(result.isSuccess()).isTrue();
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldReturnAfterThreeAttempts() {
        // Given the HelloWorldService throws an exception
        willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

        // Create a Retry with default configuration
        Retry retryContext = Retry.ofDefaults();
        // Decorate the invocation of the HelloWorldService
        Try.CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked 3 times
        BDDMockito.then(helloWorldService).should(times(3)).sayHelloWorld();
        // and the result should be a failure
        assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        assertThat(result.failed().get()).isInstanceOf(WebServiceException.class);
        assertThat(sleptTime).isEqualTo(RetryContext.DEFAULT_WAIT_DURATION*2);
    }

    @Test
    public void shouldReturnAfterOneAttempt() {
        // Given the HelloWorldService throws an exception
        willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

        // Create a Retry with default configuration
        Retry retryContext = Retry.custom().maxAttempts(1).build();
        // Decorate the invocation of the HelloWorldService
        Try.CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
        // and the result should be a failure
        assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        assertThat(result.failed().get()).isInstanceOf(WebServiceException.class);
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldReturnAfterOneAttemptAndIgnoreException() {
        // Given the HelloWorldService throws an exception
        willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

        // Create a Retry with default configuration
        Retry retryContext = Retry.custom()
                .retryOnException(throwable -> Match(throwable).of(
                        Case(instanceOf(WebServiceException.class), false),
                        Case($(), true)))
                .build();
        // Decorate the invocation of the HelloWorldService
        Try.CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked only once, because the exception should be rethrown immediately.
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
        // and the result should be a failure
        assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        assertThat(result.failed().get()).isInstanceOf(WebServiceException.class);
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void shouldTakeIntoAccountBackoffFunction() {
        // Given the HelloWorldService throws an exception
        willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

        // Create a Retry with a backoff function squaring the interval
        Retry retryContext = Retry.custom().backoffFunction(x -> x.multipliedBy(x.toMillis())).build();
        // Decorate the invocation of the HelloWorldService
        Try.CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the slept time should be according to the backoff function
        BDDMockito.then(helloWorldService).should(times(3)).sayHelloWorld();
        assertThat(sleptTime).isEqualTo(
            RetryContext.DEFAULT_WAIT_DURATION +
            RetryContext.DEFAULT_WAIT_DURATION*RetryContext.DEFAULT_WAIT_DURATION);
    }
}
