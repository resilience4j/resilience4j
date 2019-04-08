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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CircuitBreakerTest {

    private HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        helloWorldService = Mockito.mock(HelloWorldService.class);
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithSuccess() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        //When
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, helloWorldService::returnHelloWorld);

        //Then
        assertThat(supplier.get()).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        //When
        String result = circuitBreaker.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
    }


    @Test
    public void shouldDecorateSupplierAndReturnWithException() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));

        //When
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, helloWorldService::returnHelloWorld);

        //Then
        Try<String> result = Try.ofSupplier(supplier);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        //When
        CheckedFunction0<String> checkedSupplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, helloWorldService::returnHelloWorldWithException);

        //Then
        assertThat(checkedSupplier.apply()).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithException();
    }


    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new RuntimeException("BAM!"));

        //When
        CheckedFunction0<String> checkedSupplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, helloWorldService::returnHelloWorldWithException);

        //Then
        Try<String> result = Try.of(checkedSupplier);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        //When
        Callable<String> callable = CircuitBreaker.decorateCallable(circuitBreaker, helloWorldService::returnHelloWorldWithException);

        //Then
        assertThat(callable.call()).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallableAndReturnWithSuccess()  throws Throwable {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        //When
        String result = circuitBreaker.executeCallable(helloWorldService::returnHelloWorldWithException);

        //Then
        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new RuntimeException("BAM!"));

        //When
        Callable<String> callable = CircuitBreaker.decorateCallable(circuitBreaker, helloWorldService::returnHelloWorldWithException);

        //Then
        Try<String> result = Try.of(callable::call);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        CheckedRunnable checkedRunnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, helloWorldService::sayHelloWorldWithException);

        //Then
        checkedRunnable.run();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        CheckedRunnable checkedRunnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, () -> {
            throw new RuntimeException("BAM!");
        });

        //Then
        Try<Void> result = Try.run(checkedRunnable);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        Runnable runnable = CircuitBreaker.decorateRunnable(circuitBreaker, helloWorldService::sayHelloWorld);

        //Then
        runnable.run();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
    }

    @Test
    public void shouldExecuteRunnableAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        circuitBreaker.executeRunnable(helloWorldService::sayHelloWorld);

        //Then
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
    }


    @Test
    public void shouldDecorateRunnableAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        Runnable runnable = CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
            throw new RuntimeException("BAM!");
        });

        //Then
        Try<Void> result = Try.run(runnable::run);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        Consumer<String> consumer = CircuitBreaker.decorateConsumer(circuitBreaker, helloWorldService::sayHelloWorldWithName);

        //Then
        consumer.accept("Tom");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        Consumer<String> consumer = CircuitBreaker.decorateConsumer(circuitBreaker, (value) -> {
            throw new RuntimeException("BAM!");
        });

        //Then
        Try<Void> result = Try.run(() -> consumer.accept("Tom"));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        CheckedConsumer<String> checkedConsumer = CircuitBreaker.decorateCheckedConsumer(circuitBreaker, helloWorldService::sayHelloWorldWithNameWithException);

        //Then
        checkedConsumer.accept("Tom");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //When
        CheckedConsumer<String> checkedConsumer = CircuitBreaker.decorateCheckedConsumer(circuitBreaker, (value) -> {
            throw new RuntimeException("BAM!");
        });

        //Then
        Try<Void> result = Try.run(() -> checkedConsumer.accept("Tom"));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");

        //When
        Function<String, String> function = CircuitBreaker.decorateFunction(circuitBreaker, helloWorldService::returnHelloWorldWithName);

        //Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willThrow(new RuntimeException("BAM!"));

        //When
        Function<String, String> function = CircuitBreaker.decorateFunction(circuitBreaker, helloWorldService::returnHelloWorldWithName);

        //Then
        Try<String> result = Try.of(() -> function.apply("Tom"));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willReturn("Hello world Tom");

        //When
        CheckedFunction1<String, String> function = CircuitBreaker.decorateCheckedFunction(circuitBreaker, helloWorldService::returnHelloWorldWithNameWithException);

        //Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithNameWithException("Tom");
    }


    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithException() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willThrow(new RuntimeException("BAM!"));

        //When
        CheckedFunction1<String, String> function  = CircuitBreaker.decorateCheckedFunction(circuitBreaker, helloWorldService::returnHelloWorldWithNameWithException);

        //Then
        Try<String> result = Try.of(() -> function.apply("Tom"));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldReturnFailureWithCircuitBreakerOpenException() {
        // Given
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(2)
                .ringBufferSizeInHalfOpenState(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build();

        // Create a CircuitBreakerRegistry with a custom global configuration
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);

        circuitBreaker.onError(0, new RuntimeException());
        circuitBreaker.onError(0, new RuntimeException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);

        //When
        CheckedRunnable checkedRunnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, () -> {
            throw new RuntimeException("BAM!");
        });
        Try result = Try.run(checkedRunnable);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void shouldReturnFailureWithRuntimeException() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);

        //When
        CheckedRunnable checkedRunnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, () -> {
            throw new RuntimeException("BAM!");
        });
        Try result = Try.run(checkedRunnable);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldNotRecordIOExceptionAsAFailure() {
        // tag::shouldNotRecordIOExceptionAsAFailure[]
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(2)
                .ringBufferSizeInHalfOpenState(2)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .recordFailure(throwable -> Match(throwable).of(
                        Case($(instanceOf(WebServiceException.class)), true),
                        Case($(), false)))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);

        // Simulate a failure attempt
        circuitBreaker.onError(0, new WebServiceException());
        // CircuitBreaker is still CLOSED, because 1 failure is allowed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        //When
        CheckedRunnable checkedRunnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, () -> {
            throw new SocketTimeoutException("BAM!");
        });
        Try result = Try.run(checkedRunnable);

        //Then
        assertThat(result.isFailure()).isTrue();
        // CircuitBreaker is still CLOSED, because SocketTimeoutException has not been recorded as a failure
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(result.failed().get()).isInstanceOf(IOException.class);
        // end::shouldNotRecordIOExceptionAsAFailure[]

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldInvokeRecoverFunction() {
        // tag::shouldInvokeRecoverFunction[]
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        // When I decorate my function and invoke the decorated function
        CheckedFunction0<String> checkedSupplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> {
            throw new RuntimeException("BAM!");
        });
        Try<String> result = Try.of(checkedSupplier)
                .recover(throwable -> "Hello Recovery");

        // Then the function should be a success, because the exception could be recovered
        assertThat(result.isSuccess()).isTrue();
        // and the result must match the result of the recovery function.
        assertThat(result.get()).isEqualTo("Hello Recovery");
        // end::shouldInvokeRecoverFunction[]
    }

    @Test
    public void shouldInvokeMap() {
        // tag::shouldInvokeMap[]
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        // When I decorate my function
        CheckedFunction0<String> decoratedSupplier = CircuitBreaker
                .decorateCheckedSupplier(circuitBreaker, () -> "This can be any method which returns: 'Hello");

        // and chain an other function with map
        Try<String> result = Try.of(decoratedSupplier)
                        .map(value -> value + " world'");

        // Then the Try Monad returns a Success<String>, if all functions ran successfully.
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("This can be any method which returns: 'Hello world'");
        // end::shouldInvokeMap[]
    }

    @Test
    public void shouldThrowCircuitBreakerOpenException() {
        // tag::shouldThrowCircuitBreakerOpenException[]
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(2)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);

        // Simulate a failure attempt
        circuitBreaker.onError(0, new RuntimeException());
        // CircuitBreaker is still CLOSED, because 1 failure is allowed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Simulate a failure attempt
        circuitBreaker.onError(0, new RuntimeException());
        // CircuitBreaker is OPEN, because the failure rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When I decorate my function and invoke the decorated function
        Try<String> result = Try.of(CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> "Hello"))
                .map(value -> value + " world");

        // Then the call fails, because CircuitBreaker is OPEN
        assertThat(result.isFailure()).isTrue();
        // Exception is CircuitBreakerOpenException
        assertThat(result.failed().get()).isInstanceOf(CircuitBreakerOpenException.class);
        // end::shouldThrowCircuitBreakerOpenException[]

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
    }

    @Test
    public void shouldInvokeAsyncApply() throws ExecutionException, InterruptedException {
        // tag::shouldInvokeAsyncApply[]
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // When
        Supplier<String> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> "This can be any method which returns: 'Hello");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier)
                .thenApply(value -> value + " world'");

        //Then
        assertThat(future.get()).isEqualTo("This can be any method which returns: 'Hello world'");
        // end::shouldInvokeAsyncApply[]

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithSuccess() throws ExecutionException, InterruptedException {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello");
        // When

        Supplier<CompletionStage<String>> completionStageSupplier =
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
                CircuitBreaker.decorateCompletionStage(circuitBreaker, completionStageSupplier);
        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier
                .get()
                .thenApply(value -> value + " world");

        // Then the helloWorldService should be invoked 1 time
        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }


    @Test
    public void shouldExecuteCompletionStageAndReturnWithSuccess() throws ExecutionException, InterruptedException {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello");
        // When

        CompletionStage<String> decoratedCompletionStage = circuitBreaker
                .executeCompletionStage(() -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld))
                .thenApply(value -> value + " world");

        // Then the helloWorldService should be invoked 1 time
        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldRethrowExceptionAndNotRecordAsAFailure() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Given the HelloWorldService throws an exception

        // When
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new WebServiceException("BAM! At sync stage");
        };

        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
                CircuitBreaker.decorateCompletionStage(circuitBreaker, completionStageSupplier);
        Try<CompletionStage<String>> result = Try.of(decoratedCompletionStageSupplier::get);

        assertThat(result.isFailure()).isEqualTo(true);
        assertThat(result.failed().get()).isInstanceOf(WebServiceException.class);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtAsyncStage() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM! At async stage"));

        // When
        Supplier<CompletionStage<String>> completionStageSupplier =
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
                CircuitBreaker.decorateCompletionStage(circuitBreaker, completionStageSupplier);
        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get();

        // Then the helloWorldService should be invoked 1 time
        assertThatThrownBy(decoratedCompletionStage.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class).hasCause(new RuntimeException("BAM! At async stage"));
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCompletionStageAndIgnoreWebServiceException() {
        // Given
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .ignoreExceptions(WebServiceException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("backendName", config);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Given the HelloWorldService throws an exception
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new WebServiceException("BAM! At async stage"));

        // When
        Supplier<CompletionStage<String>> completionStageSupplier =
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

        CompletionStage<String> stringCompletionStage = circuitBreaker.executeCompletionStage(completionStageSupplier);

        // Then the helloWorldService should be invoked 1 time
        assertThatThrownBy(stringCompletionStage.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class).hasCause(new WebServiceException("BAM! At async stage"));
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        // WebServiceException should be ignored
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldChainDecoratedFunctions() {
        // tag::shouldChainDecoratedFunctions[]
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker anotherCircuitBreaker = CircuitBreaker.ofDefaults("anotherTestName");

        // When I create a Supplier and a Function which are decorated by different CircuitBreakers
        CheckedFunction0<String> decoratedSupplier = CircuitBreaker
                .decorateCheckedSupplier(circuitBreaker, () -> "Hello");

        CheckedFunction1<String, String> decoratedFunction = CircuitBreaker
                .decorateCheckedFunction(anotherCircuitBreaker, (input) -> input + " world");

        // and I chain a function with map
        Try<String> result = Try.of(decoratedSupplier)
                .mapTry(decoratedFunction::apply);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
        // end::shouldChainDecoratedFunctions[]

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);


        metrics = anotherCircuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

}
