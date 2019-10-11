/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import static com.jayway.awaitility.Awaitility.await;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
public class RateLimiterTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);

    private RateLimiterConfig config;
    private RateLimiter limit;

    @Before
    public void init() {
        config = RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
        limit = mock(RateLimiter.class);
        given(limit.getRateLimiterConfig()).willReturn(config);
    }

    @Test
    public void decorateCheckedSupplier() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        CheckedFunction0 decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission()).willReturn(false);
        Try decoratedSupplierResult = Try.of(decorated);
        assertThat(decoratedSupplierResult.isFailure()).isTrue();
        assertThat(decoratedSupplierResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).apply();
        given(limit.acquirePermission()).willReturn(true);

        Try secondSupplierResult = Try.of(decorated);

        assertThat(secondSupplierResult.isSuccess()).isTrue();
        then(supplier).should().apply();
    }

    @Test
    public void decorateCheckedRunnable() throws Throwable {
        CheckedRunnable runnable = mock(CheckedRunnable.class);
        CheckedRunnable decorated = RateLimiter.decorateCheckedRunnable(limit, runnable);
        given(limit.acquirePermission()).willReturn(false);
        Try decoratedRunnableResult = Try.run(decorated);
        assertThat(decoratedRunnableResult.isFailure()).isTrue();
        assertThat(decoratedRunnableResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(runnable).should(never()).run();
        given(limit.acquirePermission()).willReturn(true);

        Try secondRunnableResult = Try.run(decorated);

        assertThat(secondRunnableResult.isSuccess()).isTrue();
        then(runnable).should().run();
    }

    @Test
    public void decorateCheckedFunction() throws Throwable {
        CheckedFunction1<Integer, String> function = mock(CheckedFunction1.class);
        CheckedFunction1<Integer, String> decorated = RateLimiter.decorateCheckedFunction(limit, function);
        given(limit.acquirePermission()).willReturn(false);
        Try<String> decoratedFunctionResult = Try.success(1).mapTry(decorated);
        assertThat(decoratedFunctionResult.isFailure()).isTrue();
        assertThat(decoratedFunctionResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(function).should(never()).apply(any());
        given(limit.acquirePermission()).willReturn(true);

        Try secondFunctionResult = Try.success(1).mapTry(decorated);

        assertThat(secondFunctionResult.isSuccess()).isTrue();
        then(function).should().apply(1);
    }

    @Test
    public void decorateSupplier() {
        Supplier supplier = mock(Supplier.class);
        Supplier decorated = RateLimiter.decorateSupplier(limit, supplier);
        given(limit.acquirePermission()).willReturn(false);
        Try decoratedSupplierResult = Try.success(decorated).map(Supplier::get);
        assertThat(decoratedSupplierResult.isFailure()).isTrue();
        assertThat(decoratedSupplierResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();
        given(limit.acquirePermission()).willReturn(true);

        Try secondSupplierResult = Try.success(decorated).map(Supplier::get);

        assertThat(secondSupplierResult.isSuccess()).isTrue();
        then(supplier).should().get();
    }

    @Test
    public void decorateConsumer() {
        Consumer<Integer> consumer = mock(Consumer.class);
        Consumer<Integer> decorated = RateLimiter.decorateConsumer(limit, consumer);
        given(limit.acquirePermission()).willReturn(false);
        Try<Integer> decoratedConsumerResult = Try.success(1).andThen(decorated);
        assertThat(decoratedConsumerResult.isFailure()).isTrue();
        assertThat(decoratedConsumerResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(consumer).should(never()).accept(any());
        given(limit.acquirePermission()).willReturn(true);

        Try secondConsumerResult = Try.success(1).andThen(decorated);

        assertThat(secondConsumerResult.isSuccess()).isTrue();
        then(consumer).should().accept(1);
    }

    @Test
    public void decorateRunnable() {
        Runnable runnable = mock(Runnable.class);
        Runnable decorated = RateLimiter.decorateRunnable(limit, runnable);
        given(limit.acquirePermission()).willReturn(false);
        Try decoratedRunnableResult = Try.success(decorated).andThen(Runnable::run);
        assertThat(decoratedRunnableResult.isFailure()).isTrue();
        assertThat(decoratedRunnableResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(runnable).should(never()).run();
        given(limit.acquirePermission()).willReturn(true);

        Try secondRunnableResult = Try.success(decorated).andThen(Runnable::run);

        assertThat(secondRunnableResult.isSuccess()).isTrue();
        then(runnable).should().run();
    }

    @Test
    public void decorateFunction() {
        Function<Integer, String> function = mock(Function.class);
        Function<Integer, String> decorated = RateLimiter.decorateFunction(limit, function);
        given(limit.acquirePermission()).willReturn(false);
        Try<String> decoratedFunctionResult = Try.success(1).map(decorated);
        assertThat(decoratedFunctionResult.isFailure()).isTrue();
        assertThat(decoratedFunctionResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(function).should(never()).apply(any());
        given(limit.acquirePermission()).willReturn(true);

        Try secondFunctionResult = Try.success(1).map(decorated);

        assertThat(secondFunctionResult.isSuccess()).isTrue();
        then(function).should().apply(1);
    }

    @Test
    public void decorateCompletionStage() {
        Supplier supplier = mock(Supplier.class);
        given(supplier.get()).willReturn("Resource");
        Supplier<CompletionStage<String>> completionStage = () -> supplyAsync(supplier);
        Supplier<CompletionStage<String>> decorated = RateLimiter.decorateCompletionStage(limit, completionStage);
        given(limit.acquirePermission()).willReturn(false);
        AtomicReference<Throwable> error = new AtomicReference<>(null);
        CompletableFuture<String> notPermittedFuture = decorated.get()
            .whenComplete((v, e) -> error.set(e))
            .toCompletableFuture();

        Try<String> errorResult = Try.of(notPermittedFuture::get);

        assertTrue(errorResult.isFailure());
        assertThat(errorResult.getCause()).isInstanceOf(ExecutionException.class);
        assertThat(notPermittedFuture.isCompletedExceptionally()).isTrue();
        assertThat(error.get()).isExactlyInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();

        given(limit.acquirePermission()).willReturn(true);
        AtomicReference<Throwable> shouldBeEmpty = new AtomicReference<>(null);
        CompletableFuture<String> success = decorated.get()
            .whenComplete((v, e) -> error.set(e))
            .toCompletableFuture();

        Try<String> successResult = Try.of(success::get);

        assertThat(successResult.isSuccess()).isTrue();
        assertThat(success.isCompletedExceptionally()).isFalse();
        assertThat(shouldBeEmpty.get()).isNull();
        then(supplier).should().get();
    }

    @Test
    public void waitForPermissionWithOne() {
        given(limit.acquirePermission()).willReturn(true);

        RateLimiter.waitForPermission(limit);

        then(limit).should().acquirePermission();
    }

    @Test(expected = RequestNotPermitted.class)
    public void waitForPermissionWithoutOne() {
        given(limit.acquirePermission()).willReturn(false);

        RateLimiter.waitForPermission(limit);

        then(limit).should().acquirePermission();
    }

    @Test
    public void waitForPermissionWithInterruption() {
        when(limit.acquirePermission())
            .then(invocation -> {
                LockSupport.parkNanos(5_000_000_000L);
                return null;
            });
        AtomicBoolean wasInterrupted = new AtomicBoolean(true);
        Thread thread = new Thread(() -> {
            wasInterrupted.set(false);
            Throwable cause = Try.run(() -> RateLimiter.waitForPermission(limit))
                .getCause();
            Boolean interrupted = Match(cause).of(
                Case($(instanceOf(IllegalStateException.class)), true)
            );
            interrupted = interrupted && Thread.currentThread().isInterrupted();
            wasInterrupted.set(interrupted);
        });
        thread.setDaemon(true);
        thread.start();

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilFalse(wasInterrupted);
        thread.interrupt();
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilTrue(wasInterrupted);
    }

    @Test
    public void construction() {
        RateLimiter rateLimiter = RateLimiter.of("test", () -> config);
        assertThat(rateLimiter).isNotNull();
    }

    @Test
    public void decorateTrySupplier() {
        Supplier<Try<String>> supplier = mock(Supplier.class);
        given(supplier.get()).willReturn(Try.success("Resource"));
        given(limit.acquirePermission()).willReturn(true);

        Try<String> result = RateLimiter.decorateTrySupplier(limit, supplier).get();

        assertThat(result.isSuccess()).isTrue();
        then(supplier).should().get();
    }

    @Test
    public void decorateEitherSupplier() {
        Supplier<Either<RuntimeException, String>> supplier = mock(Supplier.class);
        given(supplier.get()).willReturn(Either.right("Resource"));
        given(limit.acquirePermission()).willReturn(true);

        Either<Exception, String> result = RateLimiter.decorateEitherSupplier(limit, supplier::get).get();

        assertThat(result.isRight()).isTrue();
        then(supplier).should().get();
    }

    @Test
    public void shouldExecuteTrySupplierAndReturnRequestNotPermitted() {
        Supplier<Try<String>> supplier = mock(Supplier.class);
        given(limit.acquirePermission()).willReturn(false);

        Try<String> result = RateLimiter.decorateTrySupplier(limit, supplier).get();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();
    }

    @Test
    public void shouldExecuteEitherSupplierAndReturnRequestNotPermitted() {
        Supplier<Either<RuntimeException, String>> supplier = mock(Supplier.class);
        given(limit.acquirePermission()).willReturn(false);

        Either<Exception, String> result = RateLimiter.decorateEitherSupplier(limit, supplier::get).get();

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();
    }

}