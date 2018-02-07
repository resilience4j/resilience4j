/*
 *
 *  Copyright 2018 Jan Sykora at GoodData(R) Corporation
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
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.ws.WebServiceException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class RetryBudgetImplTest {

    @Mock
    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Test
    public void testShouldNotRetryRunnable() {
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);

        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        runnable.run();
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void testShouldNotRetrySupplier() {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
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
    public void testShouldNotRetryCallable() throws Exception {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        Callable<String> callable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorld);

        // When
        String result = callable.call();
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void testShouldRetryRunnable() {
        // Given the HelloWorldService throws exception and then returns void
        BDDMockito.willThrow(RuntimeException.class).willNothing().given(helloWorldService).sayHelloWorld();
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);

        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        runnable.run();
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(500L);
    }

    @Test
    public void testShouldRetrySupplier() {
        // Given the HelloWorldService throws exception and then returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(RuntimeException.class).willReturn("Hello world");
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

        // When
        String result = supplier.get();
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(500L);
    }

    @Test
    public void testShouldRetryCallable() throws Exception {
        // Given the HelloWorldService throws exception and then returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(RuntimeException.class).willReturn("Hello world");
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        Callable<String> callable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorld);

        // When
        String result = callable.call();
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(500L);
    }

    @Test
    public void testShouldHitRetryMaxAttemptsRunnable() {
        // Given the HelloWorldService throws exception
        BDDMockito.willThrow(ArithmeticException.class).given(helloWorldService).sayHelloWorld();
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .maxAttempts(3)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);

        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.runRunnable(runnable::run);

        // Then the result should end in ArithmeticException
        Assertions.assertThat(result.isFailure()).isTrue();
        Assertions.assertThat(result.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the helloWorldService should be invoked 3 times
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(1000L);
    }

    @Test
    public void testShouldHitRetryMaxAttemptsSupplier() {
        // Given the HelloWorldService throws exception
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(ArithmeticException.class);
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .sleepFunction(sleep -> sleptTime += sleep)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

        // When
        Try<String> result = Try.ofSupplier(supplier);

        // Then the result should end in ArithmeticException
        Assertions.assertThat(result.isFailure()).isTrue();
        Assertions.assertThat(result.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the helloWorldService should be invoked 3 times
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
        assertThat(sleptTime).isEqualTo(1000L);
    }

    @Test
    public void testShouldHitRetryMaxAttemptsCallable() throws Exception {
        // Given the HelloWorldService throws exception
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(ArithmeticException.class);
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .maxAttempts(3)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        Callable<String> callable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorld);

        // When
        Try<String> result = Try.ofCallable(callable);

        // Then the result should end in ArithmeticException
        Assertions.assertThat(result.isFailure()).isTrue();
        Assertions.assertThat(result.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
        assertThat(sleptTime).isEqualTo(1000L);
    }

    @Test
    public void testShouldHitRetryThresholdRunnable() {
        // Given the HelloWorldService throws exception
        BDDMockito.willThrow(ArithmeticException.class).given(helloWorldService).sayHelloWorld();
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .bufferSize(5)
                .retryThreshold(0.4)
                .maxAttempts(5)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);

        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.runRunnable(runnable::run);

        // Then the result should end in ArithmeticException
        Assertions.assertThat(result.isFailure()).isTrue();
        Assertions.assertThat(result.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(500L);
    }

    @Test
    public void testShouldHitRetryThresholdSupplier() {
        // Given the HelloWorldService throws exception
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(ArithmeticException.class);
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .bufferSize(5)
                .retryThreshold(0.4)
                .maxAttempts(5)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

        // When
        Try<String> result = Try.ofSupplier(supplier);

        // Then the result should end in ArithmeticException
        Assertions.assertThat(result.isFailure()).isTrue();
        Assertions.assertThat(result.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        assertThat(sleptTime).isEqualTo(500L);
    }

    @Test
    public void testShouldHitRetryThresholdCallable() throws Exception {
        // Given the HelloWorldService throws exception
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(ArithmeticException.class);
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .bufferSize(5)
                .retryThreshold(0.4)
                .maxAttempts(5)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        Callable<String> callable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorld);

        // When
        Try<String> result = Try.ofCallable(callable);

        // Then the result should end in ArithmeticException
        Assertions.assertThat(result.isFailure()).isTrue();
        Assertions.assertThat(result.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        assertThat(sleptTime).isEqualTo(500L);
    }

    @Test
    public void testShouldRecoverFromHittingRetryThreshold() {
        // Given the HelloWorldService throws exception
        BDDMockito.willThrow(ArithmeticException.class)
                .willThrow(ArithmeticException.class)
                .willThrow(ArithmeticException.class)
                .willThrow(ArithmeticException.class)
                .willNothing()
                .willNothing()
                .willThrow(ArithmeticException.class)
                .willNothing()
                .given(helloWorldService).sayHelloWorld();
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .bufferSize(5)
                .retryThreshold(0.5)
                .maxAttempts(3)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);

        // Decorate the invocation of the HelloWorldService
        Runnable runnable1 = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);
        Runnable runnable2 = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);
        Runnable runnable3 = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);
        Runnable runnable4 = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);
        Runnable runnable5 = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);


        // When
        Try<Void> result1 = Try.runRunnable(runnable1::run);
        Try<Void> result2 = Try.runRunnable(runnable2::run);
        Try<Void> result3 = Try.runRunnable(runnable3::run);
        Try<Void> result4 = Try.runRunnable(runnable4::run);
        Try<Void> result5 = Try.runRunnable(runnable5::run);

        // Then the result1 should end in ArithmeticException
        Assertions.assertThat(result1.isFailure()).isTrue();
        Assertions.assertThat(result1.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the result2 should end in ArithmeticException
        Assertions.assertThat(result2.isFailure()).isTrue();
        Assertions.assertThat(result2.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the result3 should succeed
        Assertions.assertThat(result3.isSuccess()).isTrue();
        // Then the result4 should succeed
        Assertions.assertThat(result4.isSuccess()).isTrue();
        // Then the result5 should succeed
        Assertions.assertThat(result5.isSuccess()).isTrue();
        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(Mockito.times(8)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(1500L);
    }

    @Test
    public void testShouldRetryBasedOnExceptionPredicate() {
        // Given the HelloWorldService throws exception
        BDDMockito.willThrow(ArithmeticException.class)
                .willNothing().given(helloWorldService).sayHelloWorld();
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .retryOnException(e -> e instanceof ArithmeticException)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);

        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        runnable.run();
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(500L);
    }

    @Test
    public void testShouldNotRetryBasedOnExceptionPredicate() {
        // Given the HelloWorldService throws exception
        BDDMockito.willThrow(ArithmeticException.class).given(helloWorldService).sayHelloWorld();
        // Create a Retry with custom configuration
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .retryOnException(e -> e instanceof IllegalAccessError)
                .build();
        Retry retry = new RetryBudgetImpl("id", config);

        // Decorate the invocation of the HelloWorldService
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.runRunnable(runnable);
        // Then result should end in ArithmeticException
        Assertions.assertThat(result.isFailure()).isTrue();
        Assertions.assertThat(result.getCause()).isInstanceOf(ArithmeticException.class);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void testShouldUseIntervalFunctionForSleeping() {
        // Given the HelloWorldService throws an exception
        BDDMockito.willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

        // Create a Retry with a backoff function squaring the interval
        RetryConfig config = RetryConfig.custom()
                .sleepFunction(sleep -> sleptTime += sleep)
                .intervalFunction(IntervalFunction.of(Duration.ofMillis(500), x -> x * x))
                .build();

        Retry retry = new RetryBudgetImpl("id", config);
        // Decorate the invocation of the HelloWorldService
        CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        // When
        Try.run(retryableRunnable);

        // Then the slept time should be according to the backoff function
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).sayHelloWorld();
        Assertions.assertThat(sleptTime).isEqualTo(
                RetryConfig.DEFAULT_WAIT_DURATION +
                        RetryConfig.DEFAULT_WAIT_DURATION * RetryConfig.DEFAULT_WAIT_DURATION);
    }
}
