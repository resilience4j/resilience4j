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

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
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
        when(limit.getRateLimiterConfig())
            .thenReturn(config);
    }

    @Test
    public void decorateCheckedSupplier() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        CheckedFunction0 decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedSupplierResult = Try.of(decorated);
        then(decoratedSupplierResult.isFailure()).isTrue();
        then(decoratedSupplierResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(supplier, never()).apply();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondSupplierResult = Try.of(decorated);
        then(secondSupplierResult.isSuccess()).isTrue();
        verify(supplier, times(1)).apply();
    }

    @Test
    public void decorateCheckedRunnable() throws Throwable {
        CheckedRunnable runnable = mock(CheckedRunnable.class);
        CheckedRunnable decorated = RateLimiter.decorateCheckedRunnable(limit, runnable);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedRunnableResult = Try.run(decorated);
        then(decoratedRunnableResult.isFailure()).isTrue();
        then(decoratedRunnableResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(runnable, never()).run();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondRunnableResult = Try.run(decorated);
        then(secondRunnableResult.isSuccess()).isTrue();
        verify(runnable, times(1)).run();
    }

    @Test
    public void decorateCheckedFunction() throws Throwable {
        CheckedFunction1<Integer, String> function = mock(CheckedFunction1.class);
        CheckedFunction1<Integer, String> decorated = RateLimiter.decorateCheckedFunction(limit, function);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try<String> decoratedFunctionResult = Try.success(1).mapTry(decorated);
        then(decoratedFunctionResult.isFailure()).isTrue();
        then(decoratedFunctionResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(function, never()).apply(any());

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondFunctionResult = Try.success(1).mapTry(decorated);
        then(secondFunctionResult.isSuccess()).isTrue();
        verify(function, times(1)).apply(1);
    }

    @Test
    public void decorateSupplier() throws Exception {
        Supplier supplier = mock(Supplier.class);
        Supplier decorated = RateLimiter.decorateSupplier(limit, supplier);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedSupplierResult = Try.success(decorated).map(Supplier::get);
        then(decoratedSupplierResult.isFailure()).isTrue();
        then(decoratedSupplierResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(supplier, never()).get();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondSupplierResult = Try.success(decorated).map(Supplier::get);
        then(secondSupplierResult.isSuccess()).isTrue();
        verify(supplier, times(1)).get();
    }

    @Test
    public void decorateConsumer() throws Exception {
        Consumer<Integer> consumer = mock(Consumer.class);
        Consumer<Integer> decorated = RateLimiter.decorateConsumer(limit, consumer);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try<Integer> decoratedConsumerResult = Try.success(1).andThen(decorated);
        then(decoratedConsumerResult.isFailure()).isTrue();
        then(decoratedConsumerResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(consumer, never()).accept(any());

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondConsumerResult = Try.success(1).andThen(decorated);
        then(secondConsumerResult.isSuccess()).isTrue();
        verify(consumer, times(1)).accept(1);
    }

    @Test
    public void decorateRunnable() throws Exception {
        Runnable runnable = mock(Runnable.class);
        Runnable decorated = RateLimiter.decorateRunnable(limit, runnable);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try decoratedRunnableResult = Try.success(decorated).andThen(Runnable::run);
        then(decoratedRunnableResult.isFailure()).isTrue();
        then(decoratedRunnableResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(runnable, never()).run();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondRunnableResult = Try.success(decorated).andThen(Runnable::run);
        then(secondRunnableResult.isSuccess()).isTrue();
        verify(runnable, times(1)).run();
    }

    @Test
    public void decorateFunction() throws Exception {
        Function<Integer, String> function = mock(Function.class);
        Function<Integer, String> decorated = RateLimiter.decorateFunction(limit, function);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        Try<String> decoratedFunctionResult = Try.success(1).map(decorated);
        then(decoratedFunctionResult.isFailure()).isTrue();
        then(decoratedFunctionResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        verify(function, never()).apply(any());

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        Try secondFunctionResult = Try.success(1).map(decorated);
        then(secondFunctionResult.isSuccess()).isTrue();
        verify(function, times(1)).apply(1);
    }

    @Test
    public void decorateCompletionStage() throws Exception {
        Supplier supplier = mock(Supplier.class);
        BDDMockito.given(supplier.get()).willReturn("Resource");
        Supplier<CompletionStage<String>> completionStage = () -> supplyAsync(supplier);

        Supplier<CompletionStage<String>> decorated = RateLimiter.decorateCompletionStage(limit, completionStage);

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);

        AtomicReference<Throwable> error = new AtomicReference<>(null);
        CompletableFuture<String> notPermittedFuture = decorated.get()
            .whenComplete((v, e) -> error.set(e))
            .toCompletableFuture();
        Try<String> errorResult = Try.of(notPermittedFuture::get);
        assertTrue(errorResult.isFailure());
        then(errorResult.getCause()).isInstanceOf(ExecutionException.class);
        then(notPermittedFuture.isCompletedExceptionally()).isTrue();
        then(error.get()).isExactlyInstanceOf(RequestNotPermitted.class);
        verify(supplier, never()).get();

        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);

        AtomicReference<Throwable> shouldBeEmpty = new AtomicReference<>(null);
        CompletableFuture<String> success = decorated.get()
            .whenComplete((v, e) -> error.set(e))
            .toCompletableFuture();
        Try<String> successResult = Try.of(success::get);
        then(successResult.isSuccess()).isTrue();
        then(success.isCompletedExceptionally()).isFalse();
        then(shouldBeEmpty.get()).isNull();
        verify(supplier).get();
    }

    @Test
    public void waitForPermissionWithOne() throws Exception {
        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(true);
        RateLimiter.waitForPermission(limit);
        verify(limit, times(1))
            .getPermission(config.getTimeoutDuration());
    }

    @Test(expected = RequestNotPermitted.class)
    public void waitForPermissionWithoutOne() throws Exception {
        when(limit.getPermission(config.getTimeoutDuration()))
            .thenReturn(false);
        RateLimiter.waitForPermission(limit);
        verify(limit, times(1))
            .getPermission(config.getTimeoutDuration());
    }

    @Test
    public void waitForPermissionWithInterruption() throws Exception {
        when(limit.getPermission(config.getTimeoutDuration()))
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
    public void construction() throws Exception {
        RateLimiter rateLimiter = RateLimiter.of("test", () -> config);
        then(rateLimiter).isNotNull();
    }
}