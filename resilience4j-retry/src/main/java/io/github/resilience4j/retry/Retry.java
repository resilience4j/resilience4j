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
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.github.resilience4j.retry.internal.RetryImpl;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

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
            Retry.Context context = retry.context();
            do try {
                T result = supplier.apply();
                context.onSuccess();
                return result;
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
            Retry.Context context = retry.context();
            do try {
                R result = function.apply(t);
                context.onSuccess();
                return result;
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
            Retry.Context context = retry.context();
            do try {
                T result = supplier.get();
                context.onSuccess();
                return result;
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
            Retry.Context context = retry.context();
            do try {
                T result = supplier.call();
                context.onSuccess();
                return result;
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
            Retry.Context context = retry.context();
            do try {
                R result = function.apply(t);
                context.onSuccess();
                return result;
            } catch (RuntimeException runtimeException) {
                context.onRuntimeError(runtimeException);
            } while (true);
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

    interface Context {

        /**
         *  Records a successful call.
         */
        void onSuccess();

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

    /**
     * An EventPublisher which subscribes to the reactive stream of RetryEvents and
     * can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<RetryEvent>{

        EventPublisher onSuccess(EventConsumer<RetryOnSuccessEvent> eventConsumer);

        EventPublisher onError(EventConsumer<RetryOnErrorEvent> eventConsumer);

        EventPublisher onIgnoredError(EventConsumer<RetryOnIgnoredErrorEvent> eventConsumer);

    }
}
