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

import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


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
        CheckedSupplier supplier = mock(CheckedSupplier.class);
        CheckedSupplier decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(false);

        assertThatThrownBy(decorated::get).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();

        given(limit.acquirePermission(1)).willReturn(true);
        decorated.get();
        then(supplier).should().get();
    }

    @Test
    public void decorateCheckedRunnable() throws Throwable {
        CheckedRunnable runnable = mock(CheckedRunnable.class);
        CheckedRunnable decorated = RateLimiter.decorateCheckedRunnable(limit, runnable);
        given(limit.acquirePermission(1)).willReturn(false);

        assertThatThrownBy(decorated::run).isInstanceOf(RequestNotPermitted.class);
        then(runnable).should(never()).run();

        given(limit.acquirePermission(1)).willReturn(true);
        decorated.run();
        then(runnable).should().run();
    }

    @Test
    public void decorateCheckedFunction() throws Throwable {
        CheckedFunction<Integer, String> function = mock(CheckedFunction.class);
        CheckedFunction<Integer, String> decorated = RateLimiter
            .decorateCheckedFunction(limit, function);
        given(limit.acquirePermission(1)).willReturn(false);

        assertThatThrownBy(() -> decorated.apply(1)).isInstanceOf(RequestNotPermitted.class);
        then(function).should(never()).apply(any());

        given(limit.acquirePermission(1)).willReturn(true);
        decorated.apply(1);
        then(function).should().apply(1);
    }

    @Test
    public void decorateSupplier() {
        Supplier supplier = mock(Supplier.class);
        Supplier decorated = RateLimiter.decorateSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(false);

        assertThatThrownBy(decorated::get).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();

        given(limit.acquirePermission(1)).willReturn(true);
        decorated.get();
        then(supplier).should().get();
    }

    @Test
    public void decorateConsumer() {
        Consumer<Integer> consumer = mock(Consumer.class);
        Consumer<Integer> decorated = RateLimiter.decorateConsumer(limit, consumer);
        given(limit.acquirePermission(1)).willReturn(false);

        assertThatThrownBy(() -> decorated.accept(1)).isInstanceOf(RequestNotPermitted.class);
        then(consumer).should(never()).accept(any());

        given(limit.acquirePermission(1)).willReturn(true);
        decorated.accept(1);
        then(consumer).should().accept(1);
    }

    @Test
    public void decorateRunnable() {
        Runnable runnable = mock(Runnable.class);
        Runnable decorated = RateLimiter.decorateRunnable(limit, runnable);
        given(limit.acquirePermission(1)).willReturn(false);

        assertThatThrownBy(decorated::run).isInstanceOf(RequestNotPermitted.class);
        then(runnable).should(never()).run();

        given(limit.acquirePermission(1)).willReturn(true);
        decorated.run();
        then(runnable).should().run();
    }

    @Test
    public void decorateFunction() {
        Function<Integer, String> function = mock(Function.class);
        Function<Integer, String> decorated = RateLimiter.decorateFunction(limit, function);
        given(limit.acquirePermission(1)).willReturn(false);

        assertThatThrownBy(() -> decorated.apply(1)).isInstanceOf(RequestNotPermitted.class);
        then(function).should(never()).apply(any());

        given(limit.acquirePermission(1)).willReturn(true);
        decorated.apply(1);
        then(function).should().apply(1);
    }

    @Test
    public void decorateCompletionStage() throws Exception {
        Supplier supplier = mock(Supplier.class);
        given(supplier.get()).willReturn("Resource");
        Supplier<CompletionStage<String>> completionStage = () -> supplyAsync(supplier);
        Supplier<CompletionStage<String>> decorated = RateLimiter
            .decorateCompletionStage(limit, completionStage);
        given(limit.acquirePermission(1)).willReturn(false);
        AtomicReference<Throwable> error = new AtomicReference<>(null);
        CompletableFuture<String> notPermittedFuture = decorated.get()
            .whenComplete((v, e) -> error.set(e))
            .toCompletableFuture();

        assertThatThrownBy(notPermittedFuture::get).isInstanceOf(ExecutionException.class);
        assertTrue(notPermittedFuture.isCompletedExceptionally());
        assertThat(error.get()).isExactlyInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();

        given(limit.acquirePermission(1)).willReturn(true);
        AtomicReference<Throwable> shouldBeEmpty = new AtomicReference<>(null);
        CompletableFuture<String> success = decorated.get()
            .whenComplete((v, e) -> error.set(e))
            .toCompletableFuture();

        String result = success.get();
        assertThat(result).isEqualTo("Resource");
        assertThat(success.isCompletedExceptionally()).isFalse();
        assertThat(shouldBeEmpty.get()).isNull();
        then(supplier).should().get();
    }

    @Test(expected = RequestNotPermitted.class)
    public void decorateFutureFailure()
            throws InterruptedException, ExecutionException, TimeoutException {

        Supplier<String> supplier = mock(Supplier.class);
        given(supplier.get()).willReturn("Resource");
        Supplier<Future<String>> decoratedFuture =
            RateLimiter.decorateFuture(limit, () -> supplyAsync(supplier));
        given(limit.acquirePermission(1)).willReturn(false);

        decoratedFuture.get().get(2, TimeUnit.SECONDS);
    }

    @Test
    public void decorateFutureSuccess()
            throws ExecutionException, InterruptedException, TimeoutException {
        Supplier<String> supplier = mock(Supplier.class);
        given(supplier.get()).willReturn("Resource");
        Supplier<Future<String>> decoratedFuture =
            RateLimiter.decorateFuture(limit, () -> supplyAsync(supplier));
        given(limit.acquirePermission(1)).willReturn(true);

        String result = decoratedFuture.get().get(2, TimeUnit.SECONDS);

        then(supplier).should().get();
        assertThat(result).isEqualTo("Resource");
    }

    @Test
    public void waitForPermissionWithOne() {
        given(limit.acquirePermission(1)).willReturn(true);

        RateLimiter.waitForPermission(limit);

        then(limit).should().acquirePermission(1);
    }

    @Test(expected = RequestNotPermitted.class)
    public void waitForPermissionWithoutOne() {
        given(limit.acquirePermission(1)).willReturn(false);

        RateLimiter.waitForPermission(limit);

        then(limit).should().acquirePermission(1);
    }

    @Test
    public void waitForPermissionWithInterruption() {
        when(limit.acquirePermission(1))
            .then(invocation -> {
                LockSupport.parkNanos(5_000_000_000L);
                return null;
            });
        AtomicBoolean wasInterrupted = new AtomicBoolean(true);
        Thread thread = new Thread(() -> {
            wasInterrupted.set(false);
            Throwable cause = null;
            try {
                RateLimiter.waitForPermission(limit);
            } catch (Throwable t) {
                cause = t;
            }
            boolean interrupted = (cause instanceof IllegalStateException)
                && Thread.currentThread().isInterrupted();
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
}
