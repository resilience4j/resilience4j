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
package io.github.resilience4j.circuitbreaker;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.function.Supplier;

public interface VavrCircuitBreaker {
    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier       the original supplier
     * @param <T>            the type of results supplied by this supplier
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(CircuitBreaker circuitBreaker,
                                                           CheckedFunction0<T> supplier) {
        return () -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                T returnValue = supplier.apply();
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
                return returnValue;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param runnable       the original runnable
     * @return a runnable which is decorated by a CircuitBreaker.
     */
    static CheckedRunnable decorateCheckedRunnable(CircuitBreaker circuitBreaker,
                                                   CheckedRunnable runnable) {
        return () -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                runnable.run();
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier       the original supplier
     * @param <T>            the type of results supplied by this supplier
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Supplier<Either<Exception, T>> decorateEitherSupplier(CircuitBreaker circuitBreaker,
                                                                     Supplier<Either<? extends Exception, T>> supplier) {
        return () -> {
            if (circuitBreaker.tryAcquirePermission()) {
                final long start = circuitBreaker.getCurrentTimestamp();
                Either<? extends Exception, T> result = supplier.get();
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                if (result.isRight()) {
                    circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
                } else {
                    Exception exception = result.getLeft();
                    circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                }
                return Either.narrow(result);
            } else {
                return Either.left(
                    CallNotPermittedException.createCallNotPermittedException(circuitBreaker));
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier       the original function
     * @param <T>            the type of results supplied by this supplier
     * @return a retryable function
     */
    static <T> Supplier<Try<T>> decorateTrySupplier(CircuitBreaker circuitBreaker,
                                                    Supplier<Try<T>> supplier) {
        return () -> {
            if (circuitBreaker.tryAcquirePermission()) {
                final long start = circuitBreaker.getCurrentTimestamp();
                Try<T> result = supplier.get();
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                if (result.isSuccess()) {
                    circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
                    return result;
                } else {
                    circuitBreaker
                        .onError(duration, circuitBreaker.getTimestampUnit(), result.getCause());
                    return result;
                }
            } else {
                return Try.failure(
                    CallNotPermittedException.createCallNotPermittedException(circuitBreaker));
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param consumer       the original consumer
     * @param <T>            the type of the input to the consumer
     * @return a consumer which is decorated by a CircuitBreaker.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(CircuitBreaker circuitBreaker,
                                                          CheckedConsumer<T> consumer) {
        return (t) -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                consumer.accept(t);
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a function which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param function       the original function
     * @param <T>            the type of the input to the function
     * @param <R>            the type of the result of the function
     * @return a function which is decorated by a CircuitBreaker.
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(CircuitBreaker circuitBreaker,
                                                                 CheckedFunction1<T, R> function) {
        return (T t) -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                R returnValue = function.apply(t);
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
                return returnValue;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> Either<Exception, T> executeEitherSupplier(CircuitBreaker circuitBreaker,
        Supplier<Either<? extends Exception, T>> supplier) {
        return decorateEitherSupplier(circuitBreaker, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> Try<T> executeTrySupplier(CircuitBreaker circuitBreaker, Supplier<Try<T>> supplier) {
        return decorateTrySupplier(circuitBreaker, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    static <T> T executeCheckedSupplier(CircuitBreaker circuitBreaker, CheckedFunction0<T> checkedSupplier) throws Throwable {
        return decorateCheckedSupplier(circuitBreaker, checkedSupplier).apply();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original runnable
     */
    static void executeCheckedRunnable(CircuitBreaker circuitBreaker, CheckedRunnable runnable) throws Throwable {
        decorateCheckedRunnable(circuitBreaker, runnable).run();
    }
}
