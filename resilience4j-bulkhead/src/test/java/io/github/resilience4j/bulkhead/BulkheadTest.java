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

import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class BulkheadTest {

    private HelloWorldService helloWorldService;
    private BulkheadConfig config;

    @Before
    public void setUp(){
        helloWorldService = Mockito.mock(HelloWorldService.class);
        config = BulkheadConfig.custom()
                   .maxConcurrentCalls(1)
                   .build();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithSuccess() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // When
        Supplier<String> supplier = Bulkhead.decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);

        // Then
        assertThat(supplier.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // When
        String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

        // Then
        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));

        // When
        Supplier<String> supplier = Bulkhead.decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);
        Try<String> result = Try.of(supplier::get);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // When
        CheckedFunction0<String> checkedSupplier = Bulkhead.decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);

        // Then
        assertThat(checkedSupplier.apply()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedSupplierAndReturnWithException() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new RuntimeException("BAM!"));

        // When
        CheckedFunction0<String> checkedSupplier = Bulkhead.decorateCheckedSupplier(bulkhead, helloWorldService::returnHelloWorldWithException);
        Try<String> result = Try.of(checkedSupplier);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // When
        Callable<String> callable = Bulkhead.decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);

        // Then
        assertThat(callable.call()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallableAndReturnWithSuccess()  throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // When
        String result = bulkhead.executeCallable(helloWorldService::returnHelloWorldWithException);

        // Then
        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallableAndReturnWithException() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willThrow(new RuntimeException("BAM!"));

        // When
        Callable<String> callable = Bulkhead.decorateCallable(bulkhead, helloWorldService::returnHelloWorldWithException);
        Try<String> result = Try.of(callable::call);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        Bulkhead.decorateCheckedRunnable(bulkhead, helloWorldService::sayHelloWorldWithException)
                .run();

        // Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithException() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        CheckedRunnable checkedRunnable = Bulkhead.decorateCheckedRunnable(bulkhead, () -> {throw new RuntimeException("BAM!");});
        Try<Void> result = Try.run(checkedRunnable);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        //When
        Bulkhead.decorateRunnable(bulkhead, helloWorldService::sayHelloWorld)
                .run();

        //Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldExecuteRunnableAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        bulkhead.executeRunnable(helloWorldService::sayHelloWorld);

        // Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldDecorateRunnableAndReturnWithException() throws Throwable {
      
        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        Runnable runnable = Bulkhead.decorateRunnable(bulkhead, () -> {throw new RuntimeException("BAM!");});
        Try<Void> result = Try.run(runnable::run);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        Bulkhead.decorateConsumer(bulkhead, helloWorldService::sayHelloWorldWithName)
                .accept("Tom");

        // Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateConsumerAndReturnWithException() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        Consumer<String> consumer = Bulkhead.decorateConsumer(bulkhead, (value) -> {throw new RuntimeException("BAM!");});
        Try<Void> result = Try.run(() -> consumer.accept("Tom"));

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        Bulkhead.decorateCheckedConsumer(bulkhead, helloWorldService::sayHelloWorldWithNameWithException)
                .accept("Tom");

        // Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedConsumerAndReturnWithException() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        CheckedConsumer<String> checkedConsumer = Bulkhead.decorateCheckedConsumer(bulkhead, (value) -> {
            throw new RuntimeException("BAM!");
        });
        Try<Void> result = Try.run(() -> checkedConsumer.accept("Tom"));

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");

        // When
        Function<String, String> function = Bulkhead.decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);

        // Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateFunctionAndReturnWithException() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willThrow(new RuntimeException("BAM!"));

        // When
        Function<String, String> function = Bulkhead.decorateFunction(bulkhead, helloWorldService::returnHelloWorldWithName);
        Try<String> result = Try.of(() -> function.apply("Tom"));

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willReturn("Hello world Tom");

        // When
        String result = Bulkhead.decorateCheckedFunction(bulkhead, helloWorldService::returnHelloWorldWithNameWithException)
                                .apply("Tom");

        // Then
        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithNameWithException("Tom");
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithException() throws Throwable {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willThrow(new RuntimeException("BAM!"));

        // When
        CheckedFunction1<String, String> function  = Bulkhead.decorateCheckedFunction(bulkhead, helloWorldService::returnHelloWorldWithNameWithException);
        Try<String> result = Try.of(() -> function.apply("Tom"));

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldReturnFailureWithBulkheadFullException() {
        // tag::bulkheadFullException[]
        // Given
        BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(2).build();
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.tryAcquirePermission();
        bulkhead.tryAcquirePermission();

        // When
        CheckedRunnable checkedRunnable = Bulkhead.decorateCheckedRunnable(bulkhead, () -> {throw new RuntimeException("BAM!");});
        Try result = Try.run(checkedRunnable);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(BulkheadFullException.class);
        // end::bulkheadFullException[]
    }

    @Test
    public void shouldReturnFailureWithRuntimeException() {

        // Given
        BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(2).build();
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.tryAcquirePermission();

        //v When
        CheckedRunnable checkedRunnable = Bulkhead.decorateCheckedRunnable(bulkhead, () -> {throw new RuntimeException("BAM!");});
        Try result = Try.run(checkedRunnable);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldInvokeAsyncApply() throws ExecutionException, InterruptedException {
        // tag::shouldInvokeAsyncApply[]
        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        Supplier<String> decoratedSupplier = Bulkhead.decorateSupplier(bulkhead, () -> "This can be any method which returns: 'Hello");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier)
                                                            .thenApply(value -> value + " world'");

        String result = future.get();

        // Then
        assertThat(result).isEqualTo("This can be any method which returns: 'Hello world'");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldInvokeAsyncApply[]
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithSuccess() throws ExecutionException, InterruptedException {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello");

        // When

        Supplier<CompletionStage<String>> completionStageSupplier =
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
                Bulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier
                .get()
                .thenApply(value -> value + " world");

        // Then
        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteCompletionStageAndReturnWithSuccess() throws ExecutionException, InterruptedException {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello");

        // When
        CompletionStage<String> decoratedCompletionStage = bulkhead
                .executeCompletionStage(() -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld))
                .thenApply(value -> value + " world");

        // Then
        assertThat(decoratedCompletionStage.toCompletableFuture().get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtSyncStage() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new HelloWorldException();
        };

        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
                Bulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);

        // NOTE: Try.of does not detect a completion stage that has been completed with failure !
        Try<CompletionStage<String>> result = Try.of(decoratedCompletionStageSupplier::get);

        // Then the helloWorldService should be invoked 0 times
        BDDMockito.then(helloWorldService).should(times(0)).returnHelloWorld();
        assertThat(result.isSuccess()).isTrue();
        result.get()
              .exceptionally(
                  error -> {
                      // NOTE: Try.of does not detect a completion stage that has been completed with failure !
                      assertThat(error).isInstanceOf(HelloWorldException.class);
                      return null;
                  }
              );
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldDecorateCompletionStageAndReturnWithExceptionAtAsyncStage() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM! At async stage"));

        // When
        Supplier<CompletionStage<String>> completionStageSupplier =
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        Supplier<CompletionStage<String>> decoratedCompletionStageSupplier =
                Bulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);
        CompletionStage<String> decoratedCompletionStage = decoratedCompletionStageSupplier.get();

        // Then the helloWorldService should be invoked 1 time
        assertThatThrownBy(decoratedCompletionStage.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class).hasCause(new RuntimeException("BAM! At async stage"));
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldChainDecoratedFunctions() {
        // tag::shouldChainDecoratedFunctions[]
        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        Bulkhead anotherBulkhead = Bulkhead.of("testAnother", config);

        // When I create a Supplier and a Function which are decorated by different Bulkheads
        CheckedFunction0<String> decoratedSupplier
            = Bulkhead.decorateCheckedSupplier(bulkhead, () -> "Hello");

        CheckedFunction1<String, String> decoratedFunction
            = Bulkhead.decorateCheckedFunction(anotherBulkhead, (input) -> input + " world");

        // and I chain a function with map
        Try<String> result = Try.of(decoratedSupplier)
                                .mapTry(decoratedFunction);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        assertThat(anotherBulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldChainDecoratedFunctions[]
    }


    @Test
    public void shouldInvokeMap() {
        // tag::shouldInvokeMap[]
        // Given
        Bulkhead bulkhead = Bulkhead.of("testName", config);

        // When I decorate my function
        CheckedFunction0<String> decoratedSupplier = Bulkhead.decorateCheckedSupplier(bulkhead, () -> "This can be any method which returns: 'Hello");

        // and chain an other function with map
        Try<String> result = Try.of(decoratedSupplier)
                                .map(value -> value + " world'");

        // Then the Try Monad returns a Success<String>, if all functions ran successfully.
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("This can be any method which returns: 'Hello world'");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        // end::shouldInvokeMap[]
    }

    @Test
    public void shouldDecorateTrySupplierAndReturnWithSuccess() {
        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnTry()).willReturn(Try.success("Hello world"));

        // When
        Try<String> result = bulkhead.executeTrySupplier(helloWorldService::returnTry);

        // Then
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnTry();
    }

    @Test
    public void shouldDecorateTrySupplierAndReturnWithException() {
        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnTry()).willReturn(Try.failure(new RuntimeException("BAM!")));

        // When
        Try<String> result = bulkhead.executeTrySupplier(helloWorldService::returnTry);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnTry();
    }

    @Test
    public void shouldDecorateEitherSupplierAndReturnWithSuccess() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnEither()).willReturn(Either.right("Hello world"));

        // When
        Either<Exception, String> result = bulkhead.executeEitherSupplier(helloWorldService::returnEither);

        // Then
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnEither();
    }

    @Test
    public void shouldDecorateEitherSupplierAndReturnWithException() {
        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnEither()).willReturn(Either.left(new HelloWorldException()));

        // When
        Either<Exception, String> result = bulkhead.executeEitherSupplier(helloWorldService::returnEither);

        //Then
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(RuntimeException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnEither();
    }

    @Test
    public void shouldDecorateTrySupplierAndReturnWithBulkheadFullException() {
        // Given
        Bulkhead bulkhead = Mockito.mock(Bulkhead.class, RETURNS_DEEP_STUBS);
        BDDMockito.given(bulkhead.tryAcquirePermission()).willReturn(false);

        // When
        Try<String> result = Bulkhead.decorateTrySupplier(bulkhead, helloWorldService::returnTry).get();

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(BulkheadFullException.class);
        BDDMockito.then(helloWorldService).should(never()).returnTry();
    }

    @Test
    public void shouldDecorateEitherSupplierAndReturnWithBulkheadFullException() {
        // Given
        Bulkhead bulkhead = Mockito.mock(Bulkhead.class, RETURNS_DEEP_STUBS);
        BDDMockito.given(bulkhead.tryAcquirePermission()).willReturn(false);

        // When
        Either<Exception, String> result = Bulkhead.decorateEitherSupplier(bulkhead, helloWorldService::returnEither).get();

        //Then
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(BulkheadFullException.class);
        BDDMockito.then(helloWorldService).should(never()).returnEither();
    }


}
