/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class BulkheadTest {

    private HelloWorldService helloWorldService;
    private BulkheadConfig config;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .build();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithSuccess() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Supplier<String> supplier = Bulkhead
            .decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);

        String result = supplier.get();

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));
        Supplier<String> supplier = Bulkhead
            .decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(supplier::get);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CheckedSupplier<String> checkedSupplier = Bulkhead
            .decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        String result = checkedSupplier.get();

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        CheckedSupplier<String> checkedSupplier = Bulkhead
            .decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(() -> checkedSupplier.get());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        Callable<String> callable = Bulkhead
            .decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);

        String result = callable.call();

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallableAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        String result = bulkhead.executeCallable(helloWorldService::returnHelloWorldWithException);

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithException())
            .willThrow(new RuntimeException("BAM!"));
        Callable<String> callable = Bulkhead
            .decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);

        Try<String> result = Try.of(callable::call);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        Bulkhead.decorateCheckedRunnable(bulkhead, helloWorldService::sayHelloWorldWithException)
            .run();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithException() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        CheckedRunnable checkedRunnable = Bulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(() -> checkedRunnable.run());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithSuccess() {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        Bulkhead.decorateRunnable(bulkhead, helloWorldService::sayHelloWorld)
            .run();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldExecuteRunnableAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        bulkhead.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        Runnable runnable = Bulkhead.decorateRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(runnable::run);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        Bulkhead.decorateConsumer(bulkhead, helloWorldService::sayHelloWorldWithName)
            .accept("Tom");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        Consumer<String> consumer = Bulkhead.decorateConsumer(bulkhead, (value) -> {
            throw new RuntimeException("BAM!");
        });

        Try<Void> result = Try.run(() -> consumer.accept("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        Bulkhead.decorateCheckedConsumer(bulkhead,
            helloWorldService::sayHelloWorldWithNameWithException)
            .accept("Tom");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).sayHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        CheckedConsumer<String> checkedConsumer = Bulkhead
            .decorateCheckedConsumer(bulkhead, (value) -> {
                throw new RuntimeException("BAM!");
            });

        Try<Void> result = Try.run(() -> checkedConsumer.accept("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");
        Function<String, String> function = Bulkhead
            .decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithName("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        Function<String, String> function = Bulkhead
            .decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);

        Try<String> result = Try.of(() -> function.apply("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willReturn("Hello world Tom");

        String result = Bulkhead
            .decorateCheckedFunction(bulkhead,
                helloWorldService::returnHelloWorldWithNameWithException)
            .apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithException() throws Throwable {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willThrow(new RuntimeException("BAM!"));
        CheckedFunction<String, String> function = Bulkhead
            .decorateCheckedFunction(bulkhead,
                helloWorldService::returnHelloWorldWithNameWithException);

        Try<String> result = Try.of(() -> function.apply("Tom"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldReturnFailureWithBulkheadFullException() {
        // tag::bulkheadFullException[]
        BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(2).build();
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.tryAcquirePermission();
        bulkhead.tryAcquirePermission();
        CheckedRunnable checkedRunnable = Bulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try result = Try.run(() -> checkedRunnable.run());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(BulkheadFullException.class);
        // end::bulkheadFullException[]
    }

    @Test
    public void shouldReturnFailureWithRuntimeException() {
        BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(2).build();
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.tryAcquirePermission();
        CheckedRunnable checkedRunnable = Bulkhead.decorateCheckedRunnable(bulkhead, () -> {
            throw new RuntimeException("BAM!");
        });

        Try result = Try.run(() -> checkedRunnable.run());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldInvokeAsyncApply() throws ExecutionException, InterruptedException {
        // tag::shouldInvokeAsyncApply[]
        Bulkhead bulkhead = Bulkhead.of("test", config);
        Supplier<String> decoratedSupplier = Bulkhead
            .decorateSupplier(bulkhead, () -> "This can be any method which returns: 'Hello");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier)
            .thenApply(value -> value + " world'");

        String result = future.get();

        assertThat(result).isEqualTo("This can be any method which returns: 'Hello world'");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldInvokeAsyncApply[]
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello");
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            Bulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier
            .get()
            .thenApply(value -> value + " world");

        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteCompletionStageAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello");

        CompletionStage<String> decoratedCompletionStage = bulkhead
            .executeCompletionStage(
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld))
            .thenApply(value -> value + " world");

        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtSyncStage() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new HelloWorldException();
        };
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            Bulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        // NOTE: Try.of does not detect a completion stage that has been completed with failure!
        Try<CompletionStage<String>> result = Try.of(decoratedCompletionStageSupplier::get);

        then(helloWorldService).should(times(0)).returnHelloWorld();
        assertThat(result.isSuccess()).isTrue();
        result.get().exceptionally(error -> {
                // NOTE: Try.of does not detect a completion stage that has been completed with failure!
                assertThat(error).isInstanceOf(HelloWorldException.class);
                return null;
            }
        );
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtAsyncStage() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new RuntimeException("BAM! At async stage"));
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
            Bulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get();

        assertThatThrownBy(decoratedCompletionStage.toCompletableFuture()::get)
            .isInstanceOf(ExecutionException.class)
            .hasCause(new RuntimeException("BAM! At async stage"));
        then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldChainDecoratedFunctions() {
        // tag::shouldChainDecoratedFunctions[]
        Bulkhead bulkhead = Bulkhead.of("test", config);
        Bulkhead anotherBulkhead = Bulkhead.of("testAnother", config);
        // When I create a Supplier and a Function which are decorated by different Bulkheads
        CheckedSupplier<String> decoratedSupplier
            = Bulkhead.decorateCheckedSupplier(bulkhead, () -> "Hello");
        CheckedFunction<String, String> decoratedFunction
            = Bulkhead.decorateCheckedFunction(anotherBulkhead, (input) -> input + " world");

        // and I chain a function with map
        Try<String> result = Try.of(() -> decoratedSupplier.get())
            .mapTry(value -> decoratedFunction.apply(value));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        assertThat(anotherBulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldChainDecoratedFunctions[]
    }

    @Test
    public void shouldInvokeMap() {
        // tag::shouldInvokeMap[]
        Bulkhead bulkhead = Bulkhead.of("testName", config);
        // When I decorate my function
        CheckedSupplier<String> decoratedSupplier = Bulkhead.decorateCheckedSupplier(bulkhead,
            () -> "This can be any method which returns: 'Hello");

        // and chain an other function with map
        Try<String> result = Try.of(() -> decoratedSupplier.get())
            .map(value -> value + " world'");

        // Then the Try Monad returns a Success<String>, if all functions ran successfully.
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("This can be any method which returns: 'Hello world'");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldInvokeMap[]
    }

}
