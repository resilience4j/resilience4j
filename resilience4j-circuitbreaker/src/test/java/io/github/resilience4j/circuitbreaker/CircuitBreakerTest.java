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

import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CircuitBreakerTest {

    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithSuccess() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Supplier<String> supplier = circuitBreaker
            .decorateSupplier(helloWorldService::returnHelloWorld);

        String result = supplier.get();

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String result = circuitBreaker.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));
        Supplier<String> supplier = circuitBreaker
            .decorateSupplier(helloWorldService::returnHelloWorld);

        Try<String> result = Try.ofSupplier(supplier);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        Callable<String> callable = circuitBreaker
            .decorateCallable(helloWorldService::returnHelloWorldWithException);

        String result = callable.call();

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallableAndReturnWithSuccess() throws Throwable {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        String result = circuitBreaker
            .executeCallable(helloWorldService::returnHelloWorldWithException);

        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        Callable<String> callable = circuitBreaker
            .decorateCallable(helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(callable::call);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        Runnable runnable = circuitBreaker.decorateRunnable(helloWorldService::sayHelloWorld);

        runnable.run();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorld();
    }

    @Test
    public void shouldExecuteRunnableAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        circuitBreaker.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorld();
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        Runnable runnable = circuitBreaker.decorateRunnable(() -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(runnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        Consumer<String> consumer = circuitBreaker
            .decorateConsumer(helloWorldService::sayHelloWorldWithName);

        consumer.accept("Tom");

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().sayHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        Consumer<String> consumer = circuitBreaker.decorateConsumer((value) -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(() -> consumer.accept("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");
        Function<String, String> function = CircuitBreaker
            .decorateFunction(circuitBreaker, helloWorldService::returnHelloWorldWithName);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithException() throws Throwable {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        given(helloWorldService.returnHelloWorldWithName("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        Function<String, String> function = CircuitBreaker
            .decorateFunction(circuitBreaker, helloWorldService::returnHelloWorldWithName);

        Try<String> result = Try.of(() -> function.apply("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    public void shouldInvokeAsyncApply() throws ExecutionException, InterruptedException {
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
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
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
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }


    @Test
    public void shouldExecuteCompletionStageAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
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
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldExecuteVoidCompletionStageAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        CompletionStage<Void> decoratedCompletionStage = circuitBreaker
            .executeCompletionStage(
                () -> CompletableFuture.runAsync(helloWorldService::sayHelloWorld));

        decoratedCompletionStage.toCompletableFuture().get();

        then(helloWorldService).should().sayHelloWorld();
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtSyncStage() {
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
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtAsyncStage() {
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
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCompletionStageAndIgnoreHelloWorldException() {
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
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void testCreateWithNullConfig() {
        assertThatThrownBy(() -> CircuitBreaker.of("test", (CircuitBreakerConfig) null))
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    public void shouldNotMeasureErrorsAsFailures() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        given(helloWorldService.returnHelloWorld()).willThrow(new StackOverflowError("BAM!"));
        Supplier<String> supplier = circuitBreaker
            .decorateSupplier(helloWorldService::returnHelloWorld);

        assertThatThrownBy(supplier::get).isInstanceOf(StackOverflowError.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorld();

    }

    @Test
    public void shouldDecorateFutureSupplierAndReturnSuccess() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        Future<String> future = mock(Future.class);
        given(future.get()).willReturn("Hello World");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        String value = supplier.get().get();

        assertThat(value).isEqualTo("Hello World");
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureSupplierAndCBLogicEvalOnlyOnceSuccess() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

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

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should(times(3)).get();
    }

    @Test
    public void shouldDecorateFutureSupplierAndCBLogicEvalOnlyOnceWithException() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //create a Future
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

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should(times(3)).get();
    }

    @Test
    public void shouldDecorateFutureSupplierAndReturnWithExceptionEvenBeforeFutureIsCreated() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        given(helloWorldService.returnHelloWorldFuture()).willThrow(new RuntimeException("BAM!"));

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get());

        assertThat(thrown).isInstanceOf(RuntimeException.class)
            .hasMessage("BAM!");

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldFuture();
    }

    @Test
    public void shouldDecorateFutureSupplierAndFutureReturnException() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        //create a Future
        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new ExecutionException(new RuntimeException("BAM!")));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class);
        assertThat(thrown.getCause().getMessage()).isEqualTo("BAM!");

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureWithSupplierAndCallCancel() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new CancellationException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(CancellationException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureSupplierAndInterruptByTaskThread() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new ExecutionException(new InterruptedException()));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        // If interrupt is called on the Task thread than InterruptedException is thrown wrapped in
        // ExecutionException where as if current thread gets interrupted it throws
        // InterruptedException directly.
        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(ExecutionException.class);
        assertThat(thrown).hasCauseInstanceOf(InterruptedException.class);

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureSupplierAndInterruptedByCallingThread() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        Future<String> future = mock(Future.class);
        given(future.get()).willThrow(new InterruptedException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> decoratedFuture = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> decoratedFuture.get().get());

        assertThat(thrown).isInstanceOf(InterruptedException.class);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureSupplierTimeout() throws Exception {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);

        Future<String> future = mock(Future.class);
        given(future.get(anyLong(), any(TimeUnit.class))).willThrow(new TimeoutException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        //Decorating future likely to throw timeout exception
        Supplier<Future<String>> supplier = circuitBreaker.decorateFuture(
            helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(TimeoutException.class);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldReturnFailureWithCircuitBreakerOpenExceptionWithFutures() throws Exception {
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .permittedNumberOfCallsInHalfOpenState(2)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);

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
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(1);

        then(helloWorldService).shouldHaveZeroInteractions();
        then(future).shouldHaveZeroInteractions();
    }

    @Test
    public void shouldNotRecordIOExceptionAsAFailureWithFuture() throws Exception {
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
        // end::shouldNotRecordIOExceptionAsAFailure[]
        assertThat(thrown).isInstanceOf(ExecutionException.class);
        assertThat(thrown).hasCauseInstanceOf(IOException.class);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        //this failure is because of HelloWorldException thrown earlier
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);

        then(helloWorldService).should().returnHelloWorldFuture();
        then(future).should().get();
    }

    @Test
    public void shouldDecorateFutureAndReturnWithCallNotPermittedException() {

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
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

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(1);
    }

}
