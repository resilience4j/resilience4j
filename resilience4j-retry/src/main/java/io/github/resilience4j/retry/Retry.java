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

import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.internal.RetryContext;
import io.reactivex.Flowable;
import javaslang.control.Try;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Retry {

    /**
     * Returns the ID of this Retry.
     *
     * @return the ID of this Retry
     */
    String getId();

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

    /**
     * Returns a reactive stream of RetryEvents.
     *
     * @return a reactive stream of RetryEvents
     */
    Flowable<RetryEvent> getEventStream();

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param id the ID of the Retry
     * @param retryConfig a custom Retry configuration
     *
     * @return a Retry with a custom Retry configuration.
     */
    static RetryContext of(String id, RetryConfig retryConfig){
        return new RetryContext(id, retryConfig);
    }

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param id the ID of the Retry
     * @param retryConfigSupplier a supplier of a custom Retry configuration
     *
     * @return a Retry with a custom Retry configuration.
     */
    static RetryContext of(String id, Supplier<RetryConfig> retryConfigSupplier){
        return new RetryContext(id, retryConfigSupplier.get());
    }

    /**
     * Creates a Retry with default configuration.
     *
     * @param id the ID of the Retry
     * @return a Retry with default configuration
     */
    static Retry ofDefaults(String id){
        return new RetryContext(id, RetryConfig.ofDefaults());
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retryContext the retry context
     * @param supplier the original function
     * @param <T> the type of results supplied by this supplier
     *
     * @return a retryable function
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Retry retryContext, Try.CheckedSupplier<T> supplier){
        return () -> {
            do try {
                T result = supplier.get();
                retryContext.onSuccess();
                return result;
            } catch (Exception exception) {
                retryContext.onError(exception);
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param retryContext the retry context
     * @param runnable the original runnable
     *
     * @return a retryable runnable
     */
    static Try.CheckedRunnable decorateCheckedRunnable(Retry retryContext, Try.CheckedRunnable runnable){
        return () -> {
            do try {
                runnable.run();
                retryContext.onSuccess();
                break;
            } catch (Exception exception) {
                retryContext.onError(exception);
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param retryContext the retry context
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the result type of the function
     *
     * @return a retryable function
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Retry retryContext, Try.CheckedFunction<T, R> function){
        return (T t) -> {
            do try {
                R result = function.apply(t);
                retryContext.onSuccess();
                return result;
            } catch (Exception exception) {
                retryContext.onError(exception);
            } while (true);
        };
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retryContext the retry context
     * @param supplier the original function
     * @param <T> the type of results supplied by this supplier
     *
     * @return a retryable function
     */
    static <T> Supplier<T> decorateSupplier(Retry retryContext, Supplier<T> supplier){
        return () -> {
            do try {
                T result = supplier.get();
                retryContext.onSuccess();
                return result;
            } catch (RuntimeException runtimeException) {
                retryContext.onRuntimeError(runtimeException);
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param retryContext the retry context
     * @param runnable the original runnable
     *
     * @return a retryable runnable
     */
    static Runnable decorateRunnable(Retry retryContext, Runnable runnable){
        return () -> {
            do try {
                runnable.run();
                retryContext.onSuccess();
                break;
            } catch (RuntimeException runtimeException) {
                retryContext.onRuntimeError(runtimeException);
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param retryContext the retry context
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the result type of the function
     *
     * @return a retryable function
     */
    static <T, R> Function<T, R> decorateFunction(Retry retryContext, Function<T, R> function){
        return (T t) -> {
            do try {
                R result = function.apply(t);
                retryContext.onSuccess();
                return result;
            } catch (RuntimeException runtimeException) {
                retryContext.onRuntimeError(runtimeException);
            } while (true);
        };
    }

}
