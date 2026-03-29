/*
 *
 *  Copyright 2026 Robert Winkler
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

import static io.github.resilience4j.circuitbreaker.utils.CircuitBreakerResultUtils.ifFailedWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;

class CircuitBreakerTest {

    private HelloWorldService helloWorldService;

    @BeforeEach
    void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    void shouldDecorateSupplierAndReturnWithSuccess() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Supplier<String> supplier = circuitBreaker
            .decorateSupplier(helloWorldService::returnHelloWorld);

        String result = supplier.get();

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    void shouldExecuteSupplierAndReturnWithSuccess() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String result = circuitBreaker.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    void shouldDecorateSupplierAndReturnWithException() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));
        Supplier<String> supplier = circuitBreaker
            .decorateSupplier(helloWorldService::returnHelloWorld);

        assertThatThrownBy(supplier::get)
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    void shouldDecorateCheckedSupplierAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CheckedSupplier<String> checkedSupplier = circuitBreaker
            .decorateCheckedSupplier(helloWorldService::returnHelloWorldWithException);

        String result = checkedSupplier.get();

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorldWithException();
    }


    @Test
    void shouldDecorateCheckedSupplierAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        CheckedSupplier<String> checkedSupplier = circuitBreaker
            .decorateCheckedSupplier(helloWorldService::returnHelloWorldWithException);

        assertThatThrownBy(checkedSupplier::get)
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    void shouldDecorateCallableAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        Callable<String> callable = circuitBreaker
            .decorateCallable(helloWorldService::returnHelloWorldWithException);

        String result = callable.call();

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    void shouldExecuteCallableAndReturnWithSuccess() throws Throwable {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        String result = circuitBreaker
            .executeCallable(helloWorldService::returnHelloWorldWithException);

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    void shouldDecorateCallableAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        Callable<String> callable = circuitBreaker
            .decorateCallable(helloWorldService::returnHelloWorldWithException);

        assertThatThrownBy(callable::call)
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        CheckedRunnable checkedRunnable = circuitBreaker
            .decorateCheckedRunnable(helloWorldService::sayHelloWorldWithException);

        checkedRunnable.run();

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().sayHelloWorldWithException();
    }

    @Test
    void shouldDecorateCheckedRunnableAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        CheckedRunnable checkedRunnable = circuitBreaker.decorateCheckedRunnable(() -> {
            throw new RuntimeException("BAM!");
        });

        assertThatThrownBy(checkedRunnable::run)
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
    }

    @Test
    void shouldDecorateRunnableAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        Runnable runnable = circuitBreaker.decorateRunnable(helloWorldService::sayHelloWorld);

        runnable.run();

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().sayHelloWorld();
    }

    @Test
    void shouldExecuteRunnableAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        circuitBreaker.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().sayHelloWorld();
    }

    @Test
    void shouldDecorateRunnableAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        Runnable runnable = circuitBreaker.decorateRunnable(() -> {
            throw new RuntimeException("BAM!");
        });

        assertThatThrownBy(runnable::run)
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
    }

    @Test
    void shouldDecorateConsumerAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        Consumer<String> consumer = circuitBreaker
            .decorateConsumer(helloWorldService::sayHelloWorldWithName);

        consumer.accept("Tom");

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().sayHelloWorldWithName("Tom");
    }

    @Test
    void shouldDecorateConsumerAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        Consumer<String> consumer = circuitBreaker.decorateConsumer((value) -> {
            throw new RuntimeException("BAM!");
        });

        assertThatThrownBy(() -> consumer.accept("Tom"))
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
    }

    @Test
    void shouldDecorateCheckedConsumerAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        CheckedConsumer<String> checkedConsumer = circuitBreaker
            .decorateCheckedConsumer(helloWorldService::sayHelloWorldWithNameWithException);

        checkedConsumer.accept("Tom");

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().sayHelloWorldWithNameWithException("Tom");
    }

    @Test
    void shouldDecorateCheckedConsumerAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        int numberOfBufferedCallsBefore = metrics.getNumberOfBufferedCalls();
        CheckedConsumer<String> checkedConsumer = circuitBreaker
            .decorateCheckedConsumer((value) -> {
                throw new RuntimeException("BAM!");
            });

        assertThatThrownBy(() -> checkedConsumer.accept("Tom"))
            .isInstanceOf(RuntimeException.class);
        assertThat(numberOfBufferedCallsBefore).isZero();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
    }

    @Test
    void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");
        Function<String, String> function = CircuitBreaker
            .decorateFunction(circuitBreaker, helloWorldService::returnHelloWorldWithName);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorldWithName("Tom");
    }

    @Test
    void shouldDecorateFunctionAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithName("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        Function<String, String> function = CircuitBreaker
            .decorateFunction(circuitBreaker, helloWorldService::returnHelloWorldWithName);

        assertThatThrownBy(() -> function.apply("Tom"))
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
    }

    @Test
    void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willReturn("Hello world Tom");
        CheckedFunction<String, String> function = CircuitBreaker
            .decorateCheckedFunction(circuitBreaker,
                helloWorldService::returnHelloWorldWithNameWithException);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorldWithNameWithException("Tom");
    }


    @Test
    void shouldDecorateCheckedFunctionAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        CheckedFunction<String, String> function = CircuitBreaker
            .decorateCheckedFunction(circuitBreaker,
                helloWorldService::returnHelloWorldWithNameWithException);

        assertThatThrownBy(() -> function.apply("Tom"))
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
    }

    @Test
    void shouldReturnFailureWithCircuitBreakerOpenException() {
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .permittedNumberOfCallsInHalfOpenState(2)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        CheckedRunnable checkedRunnable = circuitBreaker.decorateCheckedRunnable(() -> {
            throw new RuntimeException("BAM!");
        });

        // When
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);

        // When
        assertThatThrownBy(checkedRunnable::run)
            .isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void shouldReturnFailureWithCircuitBreakerOpenIfCheckRequestedTransition() {
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .transitionOnResult(ifFailedWith(AccessBanException.class)
                .thenOpenFor(AccessBanException::getBanDuration))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        CheckedRunnable checkedRunnable = circuitBreaker.decorateCheckedRunnable(() -> {
            throw new AccessBanException(Duration.ofHours(2));
        });

        // When
        assertThatThrownBy(checkedRunnable::run)
            .isInstanceOf(AccessBanException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldReturnFailureWithCircuitBreakerClosedIfCheckDidNotRequestTransition() {
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .transitionOnResult(ifFailedWith(AccessBanException.class)
                .thenOpenFor(AccessBanException::getBanDuration))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        CheckedRunnable checkedRunnable = circuitBreaker.decorateCheckedRunnable(() -> {
            throw new RuntimeException();
        });

        // When
        assertThatThrownBy(checkedRunnable::run)
            .isInstanceOf(RuntimeException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void shouldReturnFailureWithRuntimeException() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        CheckedRunnable checkedRunnable = circuitBreaker.decorateCheckedRunnable(() -> {
            throw new RuntimeException("BAM!");
        });

        assertThatThrownBy(checkedRunnable::run)
            .isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void shouldNotRecordIOExceptionAsAFailure() {
        // tag::shouldNotRecordIOExceptionAsAFailure[]
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .permittedNumberOfCallsInHalfOpenState(2)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .ignoreExceptions(IOException.class)
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        // Simulate a failure attempt
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new HelloWorldException());
        // CircuitBreaker is still CLOSED, because 1 failure is allowed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        CheckedRunnable checkedRunnable = circuitBreaker.decorateCheckedRunnable(() -> {
            throw new SocketTimeoutException("BAM!");
        });

        assertThatThrownBy(checkedRunnable::run)
            .isInstanceOf(IOException.class);
        // CircuitBreaker is still CLOSED, because SocketTimeoutException has not been recorded as a failure
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // end::shouldNotRecordIOExceptionAsAFailure[]
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void shouldThrowCircuitBreakerOpenException() {
        // tag::shouldThrowCircuitBreakerOpenException[]
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        // Simulate a failure attempt
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        // CircuitBreaker is still CLOSED, because 1 failure is allowed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Simulate a failure attempt
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        // CircuitBreaker is OPEN, because the failure rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When I decorate my function and invoke the decorated function
        assertThatThrownBy(() -> circuitBreaker.decorateCheckedSupplier(() -> "Hello").get())
            .isInstanceOf(CallNotPermittedException.class);

        // Then the call fails, because CircuitBreaker is OPEN
        // Exception is CallNotPermittedException
        // end::shouldThrowCircuitBreakerOpenException[]
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
    }

    @Test
    void shouldInvokeAsyncApply() throws Exception {
        // tag::shouldInvokeAsyncApply[]
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        Supplier<String> decoratedSupplier = circuitBreaker
            .decorateSupplier(() -> "This can be any method which returns: 'Hello");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier)
            .thenApply(value -> value + " world'");

        assertThat(future.get()).isEqualTo("This can be any method which returns: 'Hello world'");
        // end::shouldInvokeAsyncApply[]
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
    }

    @Test
    void shouldDecorateCompletionStageAndReturnWithSuccess()
        throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello");
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            circuitBreaker.decorateCompletionStage(completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier
            .get()
            .thenApply(value -> value + " world");

        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        then(helloWorldService).should().returnHelloWorld();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
    }


    @Test
    void shouldExecuteCompletionStageAndReturnWithSuccess()
        throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello");

        CompletionStage<String> decoratedCompletionStage = circuitBreaker
            .executeCompletionStage(
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld))
            .thenApply(value -> value + " world");

        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        then(helloWorldService).should().returnHelloWorld();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
    }

    @Test
    void shouldExecuteVoidCompletionStageAndReturnWithSuccess()
        throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        CompletionStage<Void> decoratedCompletionStage = circuitBreaker
            .executeCompletionStage(
                () -> CompletableFuture.runAsync(helloWorldService::sayHelloWorld));

        decoratedCompletionStage.toCompletableFuture().get();

        then(helloWorldService).should().sayHelloWorld();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
    }

    @Test
    void shouldDecorateCompletionStageAndReturnWithExceptionAtSyncStage() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new CompletionException(new RuntimeException("BAM! At sync stage"));
        };
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            circuitBreaker.decorateCompletionStage(completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get();

        assertThatThrownBy(decoratedCompletionStage.toCompletableFuture()::get)
            .isInstanceOf(ExecutionException.class)
            .hasCause(new RuntimeException("BAM! At sync stage"));
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void shouldDecorateCompletionStageAndReturnWithExceptionAtAsyncStage() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new RuntimeException("BAM! At async stage"));
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            circuitBreaker.decorateCompletionStage(completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get();

        assertThatThrownBy(decoratedCompletionStage.toCompletableFuture()::get)
            .isInstanceOf(ExecutionException.class)
            .hasCause(new RuntimeException("BAM! At async stage"));
        then(helloWorldService).should().returnHelloWorld();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
    }

    @Test
    void shouldDecorateCompletionStageAndIgnoreHelloWorldException() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .ignoreExceptions(HelloWorldException.class)
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("backendName", config);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

        CompletionStage<String> stringCompletionStage = circuitBreaker
            .executeCompletionStage(completionStageSupplier);

        assertThatThrownBy(stringCompletionStage.toCompletableFuture()::get)
            .isInstanceOf(ExecutionException.class).hasCause(new HelloWorldException());
        then(helloWorldService).should().returnHelloWorld();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
    }

    @Test
    void shouldChainDecoratedFunctions() throws Throwable {
        // tag::shouldChainDecoratedFunctions[]
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker anotherCircuitBreaker = CircuitBreaker.ofDefaults("anotherTestName");
        // When I create a Supplier and a Function which are decorated by different CircuitBreakers
        CheckedSupplier<String> decoratedSupplier = CircuitBreaker
            .decorateCheckedSupplier(circuitBreaker, () -> "Hello");
        CheckedFunction<String, String> decoratedFunction = CircuitBreaker
            .decorateCheckedFunction(anotherCircuitBreaker, (input) -> input + " world");

        // and I chain a function with map
        String result = decoratedFunction.apply(decoratedSupplier.get());

        assertThat(result).isEqualTo("Hello world");
        // end::shouldChainDecoratedFunctions[]
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        CircuitBreaker.Metrics metrics2 = anotherCircuitBreaker.getMetrics();
        assertThat(metrics2.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics2.getNumberOfFailedCalls()).isZero();
    }

    @Test
    void createWithNullConfig() {
        assertThatThrownBy(() -> CircuitBreaker.of("test", (CircuitBreakerConfig) null))
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    void shouldNotMeasureErrorsAsFailures() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        given(helloWorldService.returnHelloWorld()).willThrow(new StackOverflowError("BAM!"));
        Supplier<String> supplier = circuitBreaker
            .decorateSupplier(helloWorldService::returnHelloWorld);

        assertThatThrownBy(supplier::get).isInstanceOf(StackOverflowError.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorld();

    }

    @Test
    void shouldDecorateFutureSupplierAndReturnSuccess() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willReturn("Hello World");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        String value = supplier.get().get();

        assertThat(value).isEqualTo("Hello World");
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureSupplierAndCBLogicEvalOnlyOnceSuccess() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willReturn("Hello World");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        //fetching value multiple time should evaluate CB logic only once
        Future<String> decoratedFuture = supplier.get();
        decoratedFuture.get();
        decoratedFuture.get();
        decoratedFuture.get();

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isOne();
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should(times(3)).get();
    }

    @Test
    void shouldDecorateFutureSupplierAndCBLogicEvalOnlyOnceWithException() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        //create a Future
        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new ExecutionException(new RuntimeException("BAM!")));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        //fetching value multiple time should evaluate CB logic only once
        Future<String> decoratedFuture = supplier.get();

        //evaluate future three times
        Throwable thrown = catchThrowable(() -> decoratedFuture.get());
        catchThrowable(() -> decoratedFuture.get());
        catchThrowable(() -> decoratedFuture.get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class);
        assertThat(thrown.getCause().getMessage()).isEqualTo("BAM!");

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should(times(3)).get();
    }

    @Test
    void shouldDecorateFutureSupplierAndReturnWithExceptionEvenBeforeFutureIsCreated() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        given(helloWorldService.returnHelloWorldFuture()).willThrow(new RuntimeException("BAM!"));

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get());

        assertThat(thrown).isInstanceOf(RuntimeException.class)
            .hasMessage("BAM!");

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldFuture();
    }

    @Test
    void shouldDecorateFutureSupplierAndFutureReturnException() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        //create a Future
        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new ExecutionException(new RuntimeException("BAM!")));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class);
        assertThat(thrown.getCause().getMessage()).isEqualTo("BAM!");

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureWithSupplierAndCallCancel() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new CancellationException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(CancellationException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        assertThat(metrics.getNumberOfFailedCalls()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureSupplierAndInterruptByTaskThread() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new ExecutionException(new InterruptedException()));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        // If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(InterruptedException.class);

        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureSupplierAndInterruptedByCallingThread() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new InterruptedException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> decoratedFuture = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get().get());

        assertThat(thrown).isInstanceOf(InterruptedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureSupplierTimeout() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get(anyLong(), any(TimeUnit.class))).willThrow(new TimeoutException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        //Decorating future likely to throw timeout exception
        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(TimeoutException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        assertThat(metrics.getNumberOfFailedCalls()).isOne();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldReturnFailureWithCircuitBreakerOpenExceptionWithFutures() throws Exception {
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .permittedNumberOfCallsInHalfOpenState(2)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willReturn("Hello World");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> futureSupplier = circuitBreaker
            .decorateFuture(helloWorldService::returnHelloWorldFuture);

        // When
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException());

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);

        // When
        Throwable thrown = catchThrowable(() -> futureSupplier.get().get());

        // Then
        assertThat(thrown)
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(CallNotPermittedException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isOne();

        then(helloWorldService).shouldHaveNoInteractions();
        then(future).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotRecordIOExceptionAsAFailureWithFuture() throws Exception {
        // tag::shouldNotRecordIOExceptionAsAFailure[]
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .permittedNumberOfCallsInHalfOpenState(2)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .ignoreExceptions(IOException.class)
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        // Simulate a failure attempt
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new HelloWorldException());
        // CircuitBreaker is still CLOSED, because 1 failure is allowed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        @SuppressWarnings("unchecked")
        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new ExecutionException(new SocketTimeoutException("BAM!")));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);
        Supplier<Future<String>> futureSupplier = circuitBreaker
            .decorateFuture(helloWorldService::returnHelloWorldFuture);

        // When
        Throwable thrown = catchThrowable(() -> futureSupplier.get().get());

        //Then
        // CircuitBreaker is still CLOSED, because SocketTimeoutException has not been recorded as a failure
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(thrown)
                // end::shouldNotRecordIOExceptionAsAFailure[]
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IOException.class);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isOne();
        //this failure is because of HelloWorldException thrown earlier
        assertThat(metrics.getNumberOfFailedCalls()).isOne();

        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    void shouldDecorateFutureAndReturnWithCallNotPermittedException() {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        circuitBreaker.transitionToOpenState();

        //If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        Supplier<Future<String>> futureSupplier = circuitBreaker
            .decorateFuture(helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> futureSupplier.get().get());
        assertThat(thrown)
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(CallNotPermittedException.class);

        assertThat(metrics.getNumberOfBufferedCalls()).isZero();
        assertThat(metrics.getNumberOfNotPermittedCalls()).isOne();
    }

    private static class AccessBanException extends Exception {

        private final Duration banDuration;

        public AccessBanException(Duration banDuration) {
            this.banDuration = banDuration;
        }

        public Duration getBanDuration() {
            return banDuration;
        }
    }
}
