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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.github.resilience4j.retry.internal.RetryImpl;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

/**
 * A Retry instance is thread-safe can be used to decorate multiple requests.
 * A Retry.
 */
public interface Retry {

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
    Retry.Context context();

    /**
     * Creates a async retry Context.
     *
     * @return the async retry Context
     */
    Retry.AsyncContext asyncContext();

    /**
     * Returns the RetryConfig of this Retry.
     *
     * @return the RetryConfig of this Retry
     */
    RetryConfig getRetryConfig();

    /**
     * Returns an EventPublisher can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param name the ID of the Retry
     * @param retryConfig a custom Retry configuration
     *
     * @return a Retry with a custom Retry configuration.
     */
    static Retry of(String name, RetryConfig retryConfig){
        return new RetryImpl(name, retryConfig);
    }

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param name the ID of the Retry
     * @param retryConfigSupplier a supplier of a custom Retry configuration
     *
     * @return a Retry with a custom Retry configuration.
     */
    static Retry of(String name, Supplier<RetryConfig> retryConfigSupplier){
        return new RetryImpl(name, retryConfigSupplier.get());
    }

    /**
     * Creates a Retry with default configuration.
     *
     * @param name the ID of the Retry
     * @return a Retry with default configuration
     */
    static Retry ofDefaults(String name){
        return new RetryImpl(name, RetryConfig.ofDefaults());
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T> the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier){
        return decorateSupplier(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     *
     * @return the result of the decorated Callable.
     * @param <T> the result type of callable
     * @throws Exception if unable to compute a result
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception{
        return decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable){
        decorateRunnable(this, runnable).run();
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retry the retry context
     * @param supplier the original function
     * @param <T> the type of results supplied by this supplier
     *
     * @return a retryable function
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Retry retry, CheckedFunction0<T> supplier){
        return () -> {
	        @SuppressWarnings("unchecked")
	        Retry.Context<T> context = retry.context();
            do try {
                T result = supplier.apply();
	            final boolean validationOfResult = context.onResult(result);
	            if (!validationOfResult) {
		            context.onSuccess();
		            return result;
	            }
            } catch (Exception exception) {
                context.onError(exception);
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param retry the retry context
     * @param runnable the original runnable
     *
     * @return a retryable runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Retry retry, CheckedRunnable runnable){
        return () -> {
            Retry.Context context = retry.context();
            do try {
                runnable.run();
                context.onSuccess();
                break;
            } catch (Exception exception) {
                context.onError(exception);
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param retry the retry context
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the result type of the function
     *
     * @return a retryable function
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Retry retry, CheckedFunction1<T, R> function){
        return (T t) -> {
	        @SuppressWarnings("unchecked")
	        Retry.Context<R> context = retry.context();
            do try {
	            R result = function.apply(t);
	            final boolean validationOfResult = context.onResult(result);
	            if (!validationOfResult) {
		            context.onSuccess();
		            return result;
	            }
            } catch (Exception exception) {
                context.onError(exception);
            } while (true);
        };
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retry the retry context
     * @param supplier the original function
     * @param <T> the type of results supplied by this supplier
     *
     * @return a retryable function
     */
    static <T> Supplier<T> decorateSupplier(Retry retry, Supplier<T> supplier){
        return () -> {
	        @SuppressWarnings("unchecked")
	        Retry.Context<T> context = retry.context();
            do try {
	            T result = supplier.get();
	            final boolean validationOfResult = context.onResult(result);
	            if (!validationOfResult) {
		            context.onSuccess();
		            return result;
	            }
            } catch (RuntimeException runtimeException) {
                context.onRuntimeError(runtimeException);
            } while (true);
        };
    }

    /**
     * Creates a retryable callable.
     *
     * @param retry the retry context
     * @param supplier the original function
     * @param <T> the type of results supplied by this supplier
     *
     * @return a retryable function
     */
    static <T> Callable<T> decorateCallable(Retry retry, Callable<T> supplier){
        return () -> {
	        @SuppressWarnings("unchecked")
	        Retry.Context<T> context = retry.context();
            do try {
	            T result = supplier.call();
	            final boolean validationOfResult = context.onResult(result);
	            if (!validationOfResult) {
		            context.onSuccess();
		            return result;
	            }
            } catch (RuntimeException runtimeException) {
                context.onRuntimeError(runtimeException);
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param retry the retry context
     * @param runnable the original runnable
     *
     * @return a retryable runnable
     */
    static Runnable decorateRunnable(Retry retry, Runnable runnable){
        return () -> {
            Retry.Context context = retry.context();
            do try {
                runnable.run();
                context.onSuccess();
                break;
            } catch (RuntimeException runtimeException) {
                context.onRuntimeError(runtimeException);
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param retry the retry context
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the result type of the function
     *
     * @return a retryable function
     */
    static <T, R> Function<T, R> decorateFunction(Retry retry, Function<T, R> function){
        return (T t) -> {
	        @SuppressWarnings("unchecked")
	        Retry.Context<R> context = retry.context();
            do try {
	            R result = function.apply(t);
	            final boolean validationOfResult = context.onResult(result);
	            if (!validationOfResult) {
		            context.onSuccess();
		            return result;
	            }
            } catch (RuntimeException runtimeException) {
                context.onRuntimeError(runtimeException);
            } while (true);
        };
    }

    /**
     * Decorates and executes the decorated CompletionStage.

     * @param scheduler execution service to use to schedule retries
     * @param supplier the original CompletionStage
     * @param <T> the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage( ScheduledExecutorService scheduler, Supplier<CompletionStage<T>> supplier){
        return decorateCompletionStage(this, scheduler, supplier).get();
    }

    /**
     * Decorates CompletionStageSupplier with Retry
     *
     * @param retry the retry context
     * @param scheduler execution service to use to schedule retries
     * @param supplier completion stage supplier
     * @param <T> type of completion stage result
     * @return decorated supplier
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(
            Retry retry,
            ScheduledExecutorService scheduler,
            Supplier<CompletionStage<T>> supplier
    ) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();
            @SuppressWarnings("unchecked") final Runnable block = new AsyncRetryBlock<>(scheduler, retry.asyncContext(), supplier, promise);
            block.run();

            return promise;
        };
    }

    /**
     * Get the Metrics of this RateLimiter.
     *
     * @return the Metrics of this RateLimiter
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

	/**
	 * the retry context which will be used during the retry iteration to decide what can be done on error , result, on runtime error
	 *
	 * @param <T> the result type
	 */
	interface Context<T> {

        /**
         *  Records a successful call.
         */
        void onSuccess();

		/**
		 * @param result the returned result from the called logic
		 * @return true if we need to retry again or false if no retry anymore
		 */
		boolean onResult(T result);

        /**
         * Handles a checked exception
         *
         * @param exception the exception to handle
         * @throws Throwable the exception
         */
        void onError(Exception exception) throws Throwable;

        /**
         * Handles a runtime exception
         *
         * @param runtimeException the exception to handle
         */
        void onRuntimeError(RuntimeException runtimeException);
    }

    interface AsyncContext<T> {

        /**
         *  Records a successful call.
         */
        void onSuccess();

        /**
         * Records an failed call.
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
     * An EventPublisher which subscribes to the reactive stream of RetryEvents and
     * can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<RetryEvent>{

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
            final CompletionStage<T> stage;

            try {
                stage = supplier.get();
            } catch (Throwable t) {
                onError(t);
                return;
            }

            stage.whenComplete((result, t) -> {
                if (result != null) {
                    onResult(result);
                } else if (t != null) {
                    onError(t);
                }
            });
        }

        private void onError(Throwable t) {
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
                retryContext.onSuccess();
            } else {
                scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
}
