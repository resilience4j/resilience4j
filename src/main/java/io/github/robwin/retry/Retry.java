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
package io.github.robwin.retry;

import javaslang.control.Try;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Retry {

    /**
     * Checks if the call should be retried after an Exception:
     * If yes, waits the configured time
     * If no, throw the last exception
     *
     * @throws Exception the original exception if the maximum retry attempts are exceeded
     */
    void throwOrSleepAfterException() throws Exception;

    /**
     * Checks if the call should be retried after a RuntimeException:
     * If yes, waits the configured time
     * If no, throw the last exception
     */
    void throwOrSleepAfterRuntimeException();

    /**
     * Handles a checked exception
     *
     * @param exception the exception to handle
     * @throws Throwable the exception
     */
    void handleException(Exception exception) throws Throwable;

    /**
     * Handles a runtime exception
     *
     * @param runtimeException the exception to handle
     */
    void handleRuntimeException(RuntimeException runtimeException);

    /**
     * Creates a RetryContext.Builder to configure a custom Retry.
     *
     * @return a RetryContext.Builder
     */
    static RetryContext.Builder custom(){
        return new RetryContext.Builder();
    }

    /**
     * Creates a Retry with default configuration.
     *
     * @return a Retry with default configuration
     */
    static Retry ofDefaults(){
        return Retry.custom().build();
    }


    /**
     * Creates a retryable supplier.
     *
     * @param supplier the original function
     * @param retryContext the retry context
     * @return a retryable function
     */
    @Deprecated
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Try.CheckedSupplier<T> supplier, Retry retryContext){
        return decorateCheckedSupplier(retryContext, supplier);
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retryContext the retry context
     * @param supplier the original function
     *
     * @return a retryable function
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Retry retryContext, Try.CheckedSupplier<T> supplier){
        return () -> {
            do try {
                return supplier.get();
            } catch (Exception exception) {
                retryContext.handleException(exception);
                retryContext.throwOrSleepAfterException();
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param runnable the original runnable
     * @param retryContext the retry context
     *
     * @return a retryable runnable
     */
    @Deprecated
    static Try.CheckedRunnable decorateCheckedRunnable(Try.CheckedRunnable runnable, Retry retryContext){
        return decorateCheckedRunnable(retryContext, runnable);
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
                break;
            } catch (Exception exception) {
                retryContext.handleException(exception);
                retryContext.throwOrSleepAfterException();
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param function the original function
     * @param retryContext the retry context
     * @return a retryable function
     */
    @Deprecated
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Try.CheckedFunction<T, R> function, Retry retryContext){
        return decorateCheckedFunction(retryContext, function);
    }

    /**
     * Creates a retryable function.
     *
     * @param retryContext the retry context
     * @param function the original function
     *
     * @return a retryable function
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Retry retryContext, Try.CheckedFunction<T, R> function){
        return (T t) -> {
            do try {
                return function.apply(t);
            } catch (Exception exception) {
                retryContext.handleException(exception);
                retryContext.throwOrSleepAfterException();
            } while (true);
        };
    }

    /**
     * Creates a retryable supplier.
     *
     * @param supplier the original function
     * @param retryContext the retry context
     * @return a retryable function
     */
    @Deprecated
    static <T> Supplier<T> decorateSupplier(Supplier<T> supplier, Retry retryContext){
        return decorateSupplier(retryContext, supplier);
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retryContext the retry context
     * @param supplier the original function
     *
     * @return a retryable function
     */
    static <T> Supplier<T> decorateSupplier(Retry retryContext, Supplier<T> supplier){
        return () -> {
            do try {
                return supplier.get();
            } catch (RuntimeException runtimeException) {
                retryContext.handleRuntimeException(runtimeException);
                retryContext.throwOrSleepAfterRuntimeException();
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param runnable the original runnable
     * @param retryContext the retry context
     * @return a retryable runnable
     */
    @Deprecated
    static Runnable decorateRunnable(Runnable runnable, Retry retryContext){
        return decorateRunnable(retryContext, runnable);
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
                break;
            } catch (RuntimeException runtimeException) {
                retryContext.handleRuntimeException(runtimeException);
                retryContext.throwOrSleepAfterRuntimeException();
            } while (true);
        };
    }

    /**
     * Creates a retryable function.
     *
     * @param function the original function
     * @param retryContext the retry context
     *
     * @return a retryable function
     */
    @Deprecated
    static <T, R> Function<T, R> decorateFunction(Function<T, R> function, Retry retryContext){
        return decorateFunction(retryContext, function);
    }

    /**
     * Creates a retryable function.
     *
     * @param retryContext the retry context
     * @param function the original function
     *
     * @return a retryable function
     */
    static <T, R> Function<T, R> decorateFunction(Retry retryContext, Function<T, R> function){
        return (T t) -> {
            do try {
                return function.apply(t);
            } catch (RuntimeException runtimeException) {
                retryContext.handleRuntimeException(runtimeException);
                retryContext.throwOrSleepAfterRuntimeException();
            } while (true);
        };
    }

}
