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
package io.github.resilience4j.retry;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.event.*;
import io.github.resilience4j.retry.internal.RetryImpl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Retry instance is thread-safe can be used to decorate multiple requests. A Retry.
 */
public interface Retry {

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param name        the ID of the Retry
     * @param retryConfig a custom Retry configuration
     * @return a Retry with a custom Retry configuration.
     */
    static Retry of(String name, RetryConfig retryConfig) {
        return of(name, retryConfig, Collections.emptyMap());
    }

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param name        the ID of the Retry
     * @param retryConfig a custom Retry configuration
     * @param tags        tags to assign to the Retry
     * @return a Retry with a custom Retry configuration.
     */
    static Retry of(String name, RetryConfig retryConfig, Map<String, String> tags) {
        return new RetryImpl(name, retryConfig, tags);
    }

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param name                the ID of the Retry
     * @param retryConfigSupplier a supplier of a custom Retry configuration
     * @return a Retry with a custom Retry configuration.
     */
    static Retry of(String name, Supplier<RetryConfig> retryConfigSupplier) {
        return of(name, retryConfigSupplier.get(), Collections.emptyMap());
    }

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param name                the ID of the Retry
     * @param retryConfigSupplier a supplier of a custom Retry configuration
     * @param tags                tags to assign to the Retry
     * @return a Retry with a custom Retry configuration.
     */
    static Retry of(String name, Supplier<RetryConfig> retryConfigSupplier,
                    Map<String, String> tags) {
        return new RetryImpl(name, retryConfigSupplier.get(), tags);
    }

    /**
     * Creates a Retry with default configuration.
     *
     * @param name the ID of the Retry
     * @return a Retry with default configuration
     */
    static Retry ofDefaults(String name) {
        return of(name, RetryConfig.ofDefaults(), Collections.emptyMap());
    }

    /**
     * Decorates CompletionStageSupplier with Retry
     *
     * @param retry     the retry context
     * @param scheduler execution service to use to schedule retries
     * @param supplier  completion stage supplier
     * @param <T>       type of completion stage result
     * @return decorated supplier
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(
        Retry retry,
        ScheduledExecutorService scheduler,
        Supplier<CompletionStage<T>> supplier
    ) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();
            final Runnable block = new AsyncRetryBlock<>(scheduler, retry.asyncContext(), supplier,
                promise);
            block.run();

            return promise;
        };
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retry    the retry context
     * @param supplier the original function
     * @param <T>      the type of results supplied by this supplier
     * @return a retryable function
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(Retry retry,
                                                          CheckedSupplier<T> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                try {
                    T result = supplier.get();
                    final boolean validationOfResult = context.onResult(result);
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } catch (Exception exception) {
                    context.onError(exception);
                }
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param retry    the retry context
     * @param runnable the original runnable
     * @return a retryable runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Retry retry, CheckedRunnable runnable) {
        return () -> {
            Retry.Context context = retry.context();
            do {
                try {
                    runnable.run();
                    context.onComplete();
                    break;
                } catch (Exception exception) {
                    context.onError(exception);
                }
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param retry    the retry context
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the result type of the function
     * @return a retryable function
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(Retry retry,
                                                                CheckedFunction<T, R> function) {
        return (T t) -> {
            Retry.Context<R> context = retry.context();
            do {
                try {
                    R result = function.apply(t);
                    final boolean validationOfResult = context.onResult(result);
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } catch (Exception exception) {
                    context.onError(exception);
                }
            } while (true);
        };
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retry    the retry context
     * @param supplier the original function
     * @param <T>      the type of results supplied by this supplier
     * @return a retryable function
     */
    static <T> Supplier<T> decorateSupplier(Retry retry, Supplier<T> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                try {
                    T result = supplier.get();
                    final boolean validationOfResult = context.onResult(result);
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } catch (RuntimeException runtimeException) {
                    context.onRuntimeError(runtimeException);
                }
            } while (true);
        };
    }

    /**
     * Creates a retryable callable.
     *
     * @param retry    the retry context
     * @param supplier the original function
     * @param <T>      the type of results supplied by this supplier
     * @return a retryable function
     */
    static <T> Callable<T> decorateCallable(Retry retry, Callable<T> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                try {
                    T result = supplier.call();
                    final boolean validationOfResult = context.onResult(result);
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } catch (Exception exception) {
                    context.onError(exception);
                }
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param retry    the retry context
     * @param runnable the original runnable
     * @return a retryable runnable
     */
    static Runnable decorateRunnable(Retry retry, Runnable runnable) {
        return () -> {
            Retry.Context context = retry.context();
            do {
                try {
                    runnable.run();
                    context.onComplete();
                    break;
                } catch (RuntimeException runtimeException) {
                    context.onRuntimeError(runtimeException);
                }
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param retry    the retry context
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the result type of the function
     * @return a retryable function
     */
    static <T, R> Function<T, R> decorateFunction(Retry retry, Function<T, R> function) {
        return (T t) -> {
            Retry.Context<R> context = retry.context();
            do {
                try {
                    R result = function.apply(t);
                    final boolean validationOfResult = context.onResult(result);
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } catch (RuntimeException runtimeException) {
                    context.onRuntimeError(runtimeException);
                }
            } while (true);
        };
    }

    /**
     * Returns the ID of this Retry.
     *
     * @return the ID of this Retry
     */
    String getName();

    /**
     * Creates a retry Context.
     *
     * @return the retry Context
     */
    <T> Retry.Context<T> context();

    /**
     * Creates a async retry Context.
     *
     * @return the async retry Context
     */
    <T> Retry.AsyncContext<T> asyncContext();

    /**
     * Returns the RetryConfig of this Retry.
     *
     * @return the RetryConfig of this Retry
     */
    RetryConfig getRetryConfig();

    /**
     * Returns an unmodifiable map with tags assigned to this Retry.
     *
     * @return the tags assigned to this Retry in an unmodifiable map
     */
    Map<String, String> getTags();

    /**
     * Returns an EventPublisher can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    default <T> T executeCheckedSupplier(CheckedSupplier<T> checkedSupplier) throws Throwable {
        return decorateCheckedSupplier(this, checkedSupplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier) {
        return decorateSupplier(this, supplier).get();
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
        return decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable) {
        decorateRunnable(this, runnable).run();
    }

    /**
     * Decorates and executes the decorated CompletionStage.
     *
     * @param scheduler execution service to use to schedule retries
     * @param supplier  the original CompletionStage
     * @param <T>       the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage(ScheduledExecutorService scheduler,
                                                          Supplier<CompletionStage<T>> supplier) {
        return decorateCompletionStage(this, scheduler, supplier).get();
    }

    /**
     * Get the Metrics of this Retry instance.
     *
     * @return the Metrics of this Retry instance
     */
    Metrics getMetrics();

    interface Metrics {

        /**
         * Returns the number of successful calls without a retry attempt.
         *
         * @return the number of successful calls without a retry attempt
         */
        long getNumberOfSuccessfulCallsWithoutRetryAttempt();

        /**
         * Returns the number of failed calls without a retry attempt.
         *
         * @return the number of failed calls without a retry attempt
         */
        long getNumberOfFailedCallsWithoutRetryAttempt();

        /**
         * Returns the number of successful calls after a retry attempt.
         *
         * @return the number of successful calls after a retry attempt
         */
        long getNumberOfSuccessfulCallsWithRetryAttempt();

        /**
         * Returns the number of failed calls after all retry attempts.
         *
         * @return the number of failed calls after all retry attempts
         */
        long getNumberOfFailedCallsWithRetryAttempt();
    }

    interface AsyncContext<T> {

        /**
         * Records a successful call.
         *
         * @deprecated since 1.2.0
         */
        @Deprecated
        void onSuccess();

        /**
         * Records a successful call or retryable call with the needed generated retry events. When
         * there is a successful retry before reaching the max retries limit, it will generate
         * {@link RetryOnSuccessEvent}. When the retry reach the max retries limit, it will generate
         * {@link RetryOnErrorEvent} with last exception or {@link MaxRetriesExceeded} if no other
         * exception is thrown.
         */
        void onComplete();

        /**
         * Records an failed call.
         *
         * @param throwable the exception to handle
         * @return delay in milliseconds until the next try
         */
        long onError(Throwable throwable);

        /**
         * check the result call.
         *
         * @param result the  result to validate
         * @return delay in milliseconds until the next try if the result match the predicate
         */
        long onResult(T result);
    }

    /**
     * the retry context which will be used during the retry iteration to decide what can be done on
     * error , result, on runtime error
     *
     * @param <T> the result type
     */
    interface Context<T> {

        /**
         * Records a successful call.
         *
         * @deprecated since 1.2.0
         */
        @Deprecated
        void onSuccess();


        /**
         * Records a successful call or retryable call with the needed generated retry events. When
         * there is a successful retry before reaching the max retries limit, it will generate a
         * {@link RetryOnSuccessEvent}. When the retry reaches the max retries limit, it will generate a
         * {@link RetryOnErrorEvent} with last exception or {@link MaxRetriesExceeded} if no other
         * exceptions is thrown.
         */
        void onComplete();

        /**
         * @param result the returned result from the called logic
         * @return true if we need to retry again or false if no retry anymore
         */
        boolean onResult(T result);

        /**
         * Handles a checked exception
         *
         * @param exception the exception to handle
         * @throws Exception when retry count has exceeded
         */
        void onError(Exception exception) throws Exception;

        /**
         * Handles a runtime exception
         *
         * @param runtimeException the exception to handle
         * @throws RuntimeException when retry count has exceeded
         */
        void onRuntimeError(RuntimeException runtimeException);
    }

    /**
     * An EventPublisher which subscribes to the reactive stream of RetryEvents and can be used to
     * register event consumers.
     * <p>
     * To understand when the handlers are called, see the documentation of the respective events.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<RetryEvent> {

        EventPublisher onRetry(EventConsumer<RetryOnRetryEvent> eventConsumer);

        EventPublisher onSuccess(EventConsumer<RetryOnSuccessEvent> eventConsumer);

        EventPublisher onError(EventConsumer<RetryOnErrorEvent> eventConsumer);

        EventPublisher onIgnoredError(EventConsumer<RetryOnIgnoredErrorEvent> eventConsumer);

    }

    class AsyncRetryBlock<T> implements Runnable {

        private final ScheduledExecutorService scheduler;
        private final Retry.AsyncContext<T> retryContext;
        private final Supplier<CompletionStage<T>> supplier;
        private final CompletableFuture<T> promise;

        AsyncRetryBlock(
            ScheduledExecutorService scheduler,
            Retry.AsyncContext<T> retryContext,
            Supplier<CompletionStage<T>> supplier,
            CompletableFuture<T> promise
        ) {
            this.scheduler = scheduler;
            this.retryContext = retryContext;
            this.supplier = supplier;
            this.promise = promise;
        }

        @Override
        public void run() {
            final CompletionStage<T> stage = supplier.get();

            stage.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    if (throwable instanceof Exception) {
                        onError((Exception) throwable);
                    } else {
                        promise.completeExceptionally(throwable);
                    }
                } else {
                    onResult(result);
                }
            });
        }

        private void onError(Exception t) {
            final long delay = retryContext.onError(t);

            if (delay < 1) {
                promise.completeExceptionally(t);
            } else {
                scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        }

        private void onResult(T result) {
            final long delay = retryContext.onResult(result);

            if (delay < 1) {
                promise.complete(result);
                retryContext.onComplete();
            } else {
                scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
}
