/*
 *
 *  Copyright 2020: KrnSaurabh
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

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.function.Supplier;

public interface VavrRetry {
    /**
     * Creates a retryable supplier.
     *
     * @param retry    the retry context
     * @param supplier the original function
     * @param <T>      the type of results supplied by this supplier
     * @return a retryable function
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Retry retry,
                                                           CheckedFunction0<T> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                try {
                    T result = supplier.apply();
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
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Retry retry,
                                                                 CheckedFunction1<T, R> function) {
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
    static <E extends Exception, T> Supplier<Either<E, T>> decorateEitherSupplier(Retry retry,
                                                                                  Supplier<Either<E, T>> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                Either<E, T> result = supplier.get();
                if (result.isRight()) {
                    final boolean validationOfResult = context.onResult(result.get());
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } else {
                    E exception = result.getLeft();
                    try {
                        context.onError(result.getLeft());
                    } catch (Exception e) {
                        return Either.left(exception);
                    }
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
    static <T> Supplier<Try<T>> decorateTrySupplier(Retry retry, Supplier<Try<T>> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                Try<T> result = supplier.get();
                if (result.isSuccess()) {
                    final boolean validationOfResult = context.onResult(result.get());
                    if (!validationOfResult) {
                        context.onComplete();
                        return result;
                    }
                } else {
                    Throwable cause = result.getCause();
                    if (cause instanceof Exception) {
                        try {
                            context.onError((Exception) result.getCause());
                        } catch (Exception e) {
                            return result;
                        }
                    } else {
                        return result;
                    }
                }
            } while (true);
        };
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    static  <T> T executeCheckedSupplier(Retry retry, CheckedFunction0<T> checkedSupplier) throws Throwable {
        return decorateCheckedSupplier(retry, checkedSupplier).apply();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static  <E extends Exception, T> Either<E, T> executeEitherSupplier(Retry retry, Supplier<Either<E, T>> supplier) {
        return decorateEitherSupplier(retry, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static  <T> Try<T> executeTrySupplier(Retry retry, Supplier<Try<T>> supplier) {
        return decorateTrySupplier(retry, supplier).get();
    }
}
