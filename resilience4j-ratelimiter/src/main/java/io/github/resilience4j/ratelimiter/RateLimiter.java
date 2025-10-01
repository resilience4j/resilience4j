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

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.exception.AcquirePermissionCancelledException;
import io.github.resilience4j.core.functions.*;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * A RateLimiter instance is thread-safe can be used to decorate multiple requests.
 * <p>
 * A RateLimiter distributes permits at a configurable rate. {@link #acquirePermission()} blocks if
 * necessary until a permit is available, and then takes it. Once acquired, permits need not be
 * released.
 */
public interface RateLimiter {

    /**
     * Creates a RateLimiter with a custom RateLimiter configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, RateLimiterConfig rateLimiterConfig) {
        return of(name, rateLimiterConfig, emptyMap());
    }

    /**
     * Creates a RateLimiter with a custom RateLimiter configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @param nanoTimeSupplier  specify an other source than System.nanoTimeSupplier() useful for testing
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, RateLimiterConfig rateLimiterConfig, Supplier<Long> nanoTimeSupplier) {
        return of(name, rateLimiterConfig, emptyMap(), nanoTimeSupplier);
    }

    /**
     * Creates a RateLimiter with a custom RateLimiter configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @param tags              tags to assign to the RateLimiter
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, RateLimiterConfig rateLimiterConfig,
        Map<String, String> tags) {
        return new AtomicRateLimiter(name, rateLimiterConfig, tags);
    }

    /**
     * Creates a RateLimiter with a custom RateLimiter configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @param tags              tags to assign to the RateLimiter
     * @param nanoTimeSupplier  specify an other source than System.nanoTimeSupplier() useful for testing
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, RateLimiterConfig rateLimiterConfig,
                          Map<String, String> tags, Supplier<Long> nanoTimeSupplier) {
        return new AtomicRateLimiter(name, rateLimiterConfig, tags, nanoTimeSupplier);
    }

    /**
     * Creates a RateLimiter with a custom RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiter
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
        return of(name, rateLimiterConfigSupplier.get(), emptyMap());
    }

    /**
     * Creates a RateLimiter with a custom RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiter
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @param tags                      tags to assign to the RateLimiter
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier,
        Map<String, String> tags) {
        return new AtomicRateLimiter(name, rateLimiterConfigSupplier.get(), tags);
    }

    /**
     * Creates a RateLimiter with a default RateLimiterConfig configuration.
     *
     * @param name the name of the RateLimiter
     * @return The {@link RateLimiter}
     */
    static RateLimiter ofDefaults(String name) {
        return new AtomicRateLimiter(name, RateLimiterConfig.ofDefaults());
    }

    /**
     * Returns a supplier which is decorated by a rateLimiter.
     *
     * @param rateLimiter the rateLimiter
     * @param supplier    the original supplier
     * @param <T>         the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a RateLimiter.
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(RateLimiter rateLimiter,
        Supplier<CompletionStage<T>> supplier) {
        return decorateCompletionStage(rateLimiter, 1, supplier);
    }

    /**
     * Returns a supplier which is decorated by a rateLimiter.
     *
     * @param rateLimiter the rateLimiter
     * @param permits     number of permits that this call requires
     * @param supplier    the original supplier
     * @param <T>         the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a RateLimiter.
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(RateLimiter rateLimiter,
        int permits, Supplier<CompletionStage<T>> supplier) {
        return () -> {
            final CompletableFuture<T> promise = new CompletableFuture<>();
            try {
                waitForPermission(rateLimiter, permits);
                supplier.get()
                    .whenComplete(
                        (result, throwable) -> {
                            if (throwable != null) {
                                rateLimiter.onError(throwable);
                                promise.completeExceptionally(throwable);
                            } else {
                                rateLimiter.onResult(result);
                                promise.complete(result);
                            }
                        }
                    );
            } catch (RequestNotPermitted requestNotPermitted) {
                promise.completeExceptionally(requestNotPermitted);
            } catch (Exception exception) {
                rateLimiter.onError(exception);
                promise.completeExceptionally(exception);
            }
            return promise;
        };
    }

    /**
     * Returns a Supplier which is decorated by a RateLimiter.
     * @param rateLimiter the rate limiter
     * @param supplier    the original supplier
     * @param <T>         the type of the returned Future's result
     * @param <F>         the return type of the original Supplier (extends Future&lt;T&gt;)
     * @return a supplier which is decorated by a rate limiter.
     */
    static <T, F extends Future<T>> Supplier<F> decorateFuture(
        RateLimiter rateLimiter,
        Supplier<? extends F> supplier
    ) {
        return decorateFuture(rateLimiter, 1, supplier);
    }

    /**
     * Returns a Supplier which is decorated by a RateLimiter.
     * @param rateLimiter the rate limiter
     * @param permits     the number of permits that this call requires
     * @param supplier    the original supplier
     * @param <T>         the type of the returned Future's result
     * @param <F>         the return type of the original Supplier (extends Future&lt;T&gt;)
     * @return a supplier which is decorated by a rate limiter.
     */
    static <T, F extends Future<T>> Supplier<F> decorateFuture(RateLimiter rateLimiter, int permits,
        Supplier<? extends F> supplier) {
        return () -> decorateSupplier(rateLimiter, permits, supplier)
            .get();
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param supplier    the original supplier
     * @param <T>         the type of results supplied supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(RateLimiter rateLimiter,
                                                          CheckedSupplier<T> supplier) {
        return decorateCheckedSupplier(rateLimiter, 1, supplier);
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param supplier    the original supplier
     * @param <T>         the type of results supplied supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(RateLimiter rateLimiter, int permits,
                                                          CheckedSupplier<T> supplier) {
        return () -> {
            waitForPermission(rateLimiter, permits);
            try {
                T result = supplier.get();
                rateLimiter.onResult(result);
                return result;
            } catch (Exception exception) {
                rateLimiter.onError(exception);
                throw exception;
            }
        };
    }

    /**
     * Creates a runnable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a RateLimiter.
     */
    static CheckedRunnable decorateCheckedRunnable(RateLimiter rateLimiter,
                                                   CheckedRunnable runnable) {

        return decorateCheckedRunnable(rateLimiter, 1, runnable);
    }

    /**
     * Creates a runnable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a RateLimiter.
     */
    static CheckedRunnable decorateCheckedRunnable(RateLimiter rateLimiter, int permits,
                                                   CheckedRunnable runnable) {

        return () -> {
            waitForPermission(rateLimiter, permits);
            try {
                runnable.run();
                rateLimiter.onSuccess();
            } catch (Exception exception) {
                rateLimiter.onError(exception);
                throw exception;
            }
        };
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param function    the original function
     * @param <T>         the type of function argument
     * @param <R>         the type of function results
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(RateLimiter rateLimiter,
                                                                CheckedFunction<T, R> function) {
        return decorateCheckedFunction(rateLimiter, 1, function);
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param function    the original function
     * @param <T>         the type of function argument
     * @param <R>         the type of function results
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(RateLimiter rateLimiter,
                                                                 int permits, CheckedFunction<T, R> function) {
        return (T t) -> decorateCheckedSupplier(rateLimiter, permits, () -> function.apply(t))
            .get();
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter       the RateLimiter
     * @param permitsCalculator calculates the number of permits required by this call based on the
     *                          functions argument
     * @param function          the original function
     * @param <T>               the type of function argument
     * @param <R>               the type of function results
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(RateLimiter rateLimiter,
                                                                 Function<T, Integer> permitsCalculator, CheckedFunction<T, R> function) {
        return (T t) -> decorateCheckedFunction(rateLimiter, permitsCalculator.apply(t), function)
            .apply(t);
    }

    /**
     * Creates a consumer which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param consumer    the original consumer
     * @param <T>         the type of consumer argument
     * @return a consumer which is restricted by a RateLimiter.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(RateLimiter rateLimiter,
                                                           CheckedConsumer<T> consumer) {
        return decorateCheckedConsumer(rateLimiter, 1, consumer);
    }

    /**
     * Creates a consumer which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param consumer    the original consumer
     * @param <T>         the type of consumer argument
     * @return a consumer which is restricted by a RateLimiter.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(RateLimiter rateLimiter,
                                                                int permits, CheckedConsumer<T> consumer) {
        return (T t) -> decorateCheckedRunnable(rateLimiter, permits, () -> consumer.accept(t))
                .run();
    }

    /**
     * Creates a consumer which is restricted by a RateLimiter.
     *
     * @param rateLimiter       the RateLimiter
     * @param permitsCalculator calculates the number of permits required by this call based on the
     *                          functions argument
     * @param consumer          the original consumer
     * @param <T>               the type of consumer argument
     * @return a consumer which is restricted by a RateLimiter.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(RateLimiter rateLimiter,
                                                                Function<T, Integer> permitsCalculator, CheckedConsumer<T> consumer) {
        return (T t) -> decorateCheckedConsumer(rateLimiter, permitsCalculator.apply(t), consumer)
                .accept(t);
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param supplier    the original supplier
     * @param <T>         the type of results supplied supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> Supplier<T> decorateSupplier(RateLimiter rateLimiter, Supplier<T> supplier) {
        return decorateSupplier(rateLimiter, 1, supplier);
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param supplier    the original supplier
     * @param <T>         the type of results supplied supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> Supplier<T> decorateSupplier(RateLimiter rateLimiter, int permits, Supplier<T> supplier) {
        return decorateCheckedSupplier(rateLimiter, permits, supplier::get)
            .unchecked();
    }

    /**
     * Creates a callable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param callable    the original callable
     * @param <T>         the type of results supplied by callable
     * @return a callable which is restricted by a RateLimiter.
     */
    static <T> Callable<T> decorateCallable(RateLimiter rateLimiter, Callable<T> callable) {
        return decorateCallable(rateLimiter, 1, callable);
    }

    /**
     * Creates a callable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param callable    the original callable
     * @param <T>         the type of results supplied by callable
     * @return a callable which is restricted by a RateLimiter.
     */
    static <T> Callable<T> decorateCallable(RateLimiter rateLimiter, int permits, Callable<T> callable) {
        return () -> decorateCheckedSupplier(rateLimiter, permits, callable::call)
            .unchecked()
            .get();
    }

    /**
     * Creates a consumer which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param consumer    the original consumer
     * @param <T>         the type of the input to the consumer
     * @return a consumer which is restricted by a RateLimiter.
     */
    static <T> Consumer<T> decorateConsumer(RateLimiter rateLimiter, Consumer<T> consumer) {
        return decorateConsumer(rateLimiter, 1, consumer);
    }

    /**
     * Creates a consumer which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param consumer    the original consumer
     * @param <T>         the type of the input to the consumer
     * @return a consumer which is restricted by a RateLimiter.
     */
    static <T> Consumer<T> decorateConsumer(RateLimiter rateLimiter, int permits,
        Consumer<T> consumer) {
        return (T t) -> {
            waitForPermission(rateLimiter, permits);
            try {
                consumer.accept(t);
                rateLimiter.onSuccess();
            } catch (Exception exception) {
                rateLimiter.onError(exception);
                throw exception;
            }
        };
    }

    /**
     * Creates a consumer which is restricted by a RateLimiter.
     *
     * @param rateLimiter       the RateLimiter
     * @param permitsCalculator calculates the number of permits required by this call based on the
     *                          functions argument
     * @param consumer          the original consumer
     * @param <T>               the type of the input to the consumer
     * @return a consumer which is restricted by a RateLimiter.
     */
    static <T> Consumer<T> decorateConsumer(RateLimiter rateLimiter,
        Function<T, Integer> permitsCalculator, Consumer<T> consumer) {
        return (T t) -> decorateConsumer(rateLimiter, permitsCalculator.apply(t), consumer).accept(t);
    }

    /**
     * Creates a runnable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a RateLimiter.
     */
    static Runnable decorateRunnable(RateLimiter rateLimiter, Runnable runnable) {
        return decorateRunnable(rateLimiter, 1, runnable);
    }

    /**
     * Creates a runnable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a RateLimiter.
     */
    static Runnable decorateRunnable(RateLimiter rateLimiter, int permits, Runnable runnable) {
        return decorateCheckedRunnable(rateLimiter, permits, runnable::run)
            .unchecked();
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param function    the original function
     * @param <T>         the type of the input to the function
     * @param <R>         the type of the result of the function
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> Function<T, R> decorateFunction(RateLimiter rateLimiter,
        Function<T, R> function) {
        return decorateFunction(rateLimiter, 1, function);
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param permits     number of permits that this call requires
     * @param function    the original function
     * @param <T>         the type of the input to the function
     * @param <R>         the type of the result of the function
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> Function<T, R> decorateFunction(RateLimiter rateLimiter, int permits, Function<T, R> function) {
        return decorateCheckedFunction(rateLimiter, permits, function::apply)
            .unchecked();
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter       the RateLimiter
     * @param function          the original function
     * @param permitsCalculator calculates the number of permits required by this call based on the
     *                          functions argument
     * @param <T>               the type of the input to the function
     * @param <R>               the type of the result of the function
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> Function<T, R> decorateFunction(RateLimiter rateLimiter,
        Function<T, Integer> permitsCalculator, Function<T, R> function) {
        return (T t) -> decorateFunction(rateLimiter, permitsCalculator.apply(t), function).apply(t);
    }

    /**
     * Will wait for permission within default timeout duration.
     *
     * @param rateLimiter the RateLimiter to get permission from
     * @throws RequestNotPermitted                 if waiting time elapsed before a permit was
     *                                             acquired.
     * @throws AcquirePermissionCancelledException if thread was interrupted during permission wait
     */
    static void waitForPermission(final RateLimiter rateLimiter) {
        waitForPermission(rateLimiter, 1);
    }

    /**
     * Will wait for required number of permits within default timeout duration.
     *
     * @param rateLimiter the RateLimiter to get permission from
     * @param permits     number of permits we have to acquire
     * @throws RequestNotPermitted                 if waiting time elapsed before a permit was
     *                                             acquired.
     * @throws AcquirePermissionCancelledException if thread was interrupted during permission wait
     */
    static void waitForPermission(final RateLimiter rateLimiter, int permits) {
        boolean permission = rateLimiter.acquirePermission(permits);
        if (Thread.currentThread().isInterrupted()) {
            throw new AcquirePermissionCancelledException();
        }
        if (!permission) {
            throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        }
    }

    /**
     * Will drain permits remaining in cycle if calls result meet the criteria defined in
     * {@link RateLimiterConfig#getDrainPermissionsOnResult()}.
     *
     * @param callsResult result of a methods call that was rate limiter by this rate limiter
     * @deprecated because of changing visibility to private in Java9+
     */
    @Deprecated
    default void drainIfNeeded(Either<? extends Throwable, ?> callsResult) {
        Predicate<Either<? extends Throwable, ?>> checker = getRateLimiterConfig()
            .getDrainPermissionsOnResult();
        if (checker != null && checker.test(callsResult)) {
            drainPermissions();
        }
    }

    /**
     * Decorates and executes the decorated CompletionStage.
     *
     * @param supplier the original CompletionStage
     * @param <T>      the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier) {
        return executeCompletionStage(1, supplier);
    }

    /**
     * Decorates and executes the decorated CompletionStage.
     *
     * @param permits  number of permits that this call requires
     * @param supplier the original CompletionStage
     * @param <T>      the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage(int permits, Supplier<CompletionStage<T>> supplier) {
        return decorateCompletionStage(this, permits, supplier).get();
    }

    /**
     * Dynamic rate limiter configuration change. This method allows to change timeout duration of
     * current limiter. NOTE! New timeout duration won't affect threads that are currently waiting
     * for permission.
     *
     * @param timeoutDuration new timeout duration
     */
    void changeTimeoutDuration(Duration timeoutDuration);

    /**
     * Dynamic rate limiter configuration change. This method allows to change count of permissions
     * available during refresh period. NOTE! New limit won't affect current period permissions and
     * will apply only from next one.
     *
     * @param limitForPeriod new permissions limit
     */
    void changeLimitForPeriod(int limitForPeriod);

    /**
     * Acquires a permission from this rate limiter, blocking until one is available, or the thread
     * is interrupted. Maximum wait time is {@link RateLimiterConfig#getTimeoutDuration()}
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException}, but its
     * interrupt status will be set.
     *
     * @return {@code true} if a permit was acquired and {@code false} if waiting timeoutDuration
     * elapsed before a permit was acquired
     */
    default boolean acquirePermission() {
        return acquirePermission(1);
    }

    /**
     * Acquires the given number of permits from this rate limiter, blocking until one is available,
     * or the thread is interrupted. Maximum wait time is {@link RateLimiterConfig#getTimeoutDuration()}
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException}, but its
     * interrupt status will be set.
     *
     * @param permits number of permits - use for systems where 1 call != 1 permit
     * @return {@code true} if a permit was acquired and {@code false} if waiting timeoutDuration
     * elapsed before a permit was acquired
     */
    boolean acquirePermission(int permits);

    /**
     * Reserves a permission from this rate limiter and returns nanoseconds you should wait for it.
     * If returned long is negative, it means that you failed to reserve permission, possibly your
     * {@link RateLimiterConfig#getTimeoutDuration()} is less then time to wait for permission.
     *
     * @return {@code long} amount of nanoseconds you should wait for reserved permissions. if
     * negative, it means you failed to reserve.
     */
    default long reservePermission() {
        return reservePermission(1);
    }

    /**
     * Reserves the given number permits from this rate limiter and returns nanoseconds you should
     * wait for it. If returned long is negative, it means that you failed to reserve permission,
     * possibly your  {@link RateLimiterConfig#getTimeoutDuration()} is less then time to wait for
     * permission.
     *
     * @param permits number of permits - use for systems where 1 call != 1 permit
     * @return {@code long} amount of nanoseconds you should wait for reserved permissions. if
     * negative, it means you failed to reserve.
     */
    long reservePermission(int permits);

    /**
     * Drains all the permits left in the current period.
     */
    void drainPermissions();

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param throwable The throwable which must be recorded
     */
    default void onError(Throwable throwable) {
        drainIfNeeded(Either.left(throwable));
    }

    /**
     * Records a successful call. This method must be invoked when a call was
     * successful.
     */
    default void onSuccess() {
        drainIfNeeded(Either.right(null));
    }

    /**
     * This method must be invoked when a call returned a result
     * and the result predicate should decide if the call was successful or not.
     *
     * @param result The result of the protected function
     */
    default void onResult(Object result) {
        drainIfNeeded(Either.right(result));
    }

    /**
     * Get the name of this RateLimiter
     *
     * @return the name of this RateLimiter
     */
    String getName();

    /**
     * Get the RateLimiterConfig of this RateLimiter.
     *
     * @return the RateLimiterConfig of this RateLimiter
     */
    RateLimiterConfig getRateLimiterConfig();

    /**
     * Returns an unmodifiable map with tags assigned to this RateLimiter.
     *
     * @return the tags assigned to this Retry in an unmodifiable map
     */
    Map<String, String> getTags();

    /**
     * Get the Metrics of this RateLimiter.
     *
     * @return the Metrics of this RateLimiter
     */
    Metrics getMetrics();

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier) {
        return executeSupplier(1, supplier);
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param permits  number of permits that this call requires
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(int permits, Supplier<T> supplier) {
        return decorateSupplier(this, permits, supplier).get();
    }



    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     * @param <T>      the result type of callable
     * @return the result of the decorated Callable.
     * @throws Exception if unable to compute a result
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception {
        return executeCallable(1, callable);
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param permits  number of permits that this call requires
     * @param callable the original Callable
     * @param <T>      the result type of callable
     * @return the result of the decorated Callable.
     * @throws Exception if unable to compute a result
     */
    default <T> T executeCallable(int permits, Callable<T> callable) throws Exception {
        return decorateCallable(this, permits, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable) {
        executeRunnable(1, runnable);
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param permits  number of permits that this call requires
     * @param runnable the original Runnable
     */
    default void executeRunnable(int permits, Runnable runnable) {
        decorateRunnable(this, permits, runnable).run();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    default <T> T executeCheckedSupplier(CheckedSupplier<T> checkedSupplier) throws Throwable {
        return executeCheckedSupplier(1, checkedSupplier);
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param permits         number of permits that this call requires
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    default <T> T executeCheckedSupplier(int permits, CheckedSupplier<T> checkedSupplier)
        throws Throwable {
        return decorateCheckedSupplier(this, permits, checkedSupplier).get();
    }


    interface Metrics {

        /**
         * Returns an estimate of the number of threads waiting for permission in this JVM process.
         * <p>This method is typically used for debugging and testing purposes.
         *
         * @return estimate of the number of threads waiting for permission.
         */
        int getNumberOfWaitingThreads();

        /**
         * Estimates count of available permissions. Can be negative if some permissions where
         * reserved.
         * <p>This method is typically used for debugging and testing purposes.
         *
         * @return estimated count of permissions
         */
        int getAvailablePermissions();
    }

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<RateLimiterEvent> {

        EventPublisher onSuccess(EventConsumer<RateLimiterOnSuccessEvent> eventConsumer);

        EventPublisher onFailure(EventConsumer<RateLimiterOnFailureEvent> eventConsumer);

    }
}
