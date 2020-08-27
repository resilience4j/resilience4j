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
package io.github.resilience4j.ratelimiter;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.resilience4j.ratelimiter.RateLimiter.waitForPermission;

public interface VavrRateLimiter {
    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param supplier    the original supplier
     * @param <T>         the type of results supplied supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(RateLimiter rateLimiter,
                                                           CheckedFunction0<T> supplier) {
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
    static <T> CheckedFunction0<T> decorateCheckedSupplier(RateLimiter rateLimiter, int permits,
                                                           CheckedFunction0<T> supplier) {
        return () -> {
            waitForPermission(rateLimiter, permits);
            return supplier.apply();
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
            runnable.run();
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
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(RateLimiter rateLimiter,
                                                                 CheckedFunction1<T, R> function) {
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
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(RateLimiter rateLimiter,
                                                                 int permits, CheckedFunction1<T, R> function) {
        return (T t) -> {
            waitForPermission(rateLimiter, permits);
            return function.apply(t);
        };
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
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(RateLimiter rateLimiter,
                                                                 Function<T, Integer> permitsCalculator, CheckedFunction1<T, R> function) {
        return (T t) -> {
            waitForPermission(rateLimiter, permitsCalculator.apply(t));
            return function.apply(t);
        };
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param supplier    the original supplier
     * @param <T>         the type of results supplied supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> Supplier<Try<T>> decorateTrySupplier(RateLimiter rateLimiter,
                                                    Supplier<Try<T>> supplier) {
        return decorateTrySupplier(rateLimiter, 1, supplier);
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
    static <T> Supplier<Try<T>> decorateTrySupplier(RateLimiter rateLimiter, int permits,
                                                    Supplier<Try<T>> supplier) {
        return () -> {
            try {
                waitForPermission(rateLimiter, permits);
                return supplier.get();
            } catch (RequestNotPermitted requestNotPermitted) {
                return Try.failure(requestNotPermitted);
            }
        };
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param supplier    the original supplier
     * @param <T>         the type of results supplied supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> Supplier<Either<Exception, T>> decorateEitherSupplier(RateLimiter rateLimiter,
                                                                     Supplier<Either<? extends Exception, T>> supplier) {
        return decorateEitherSupplier(rateLimiter, 1, supplier);
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
    static <T> Supplier<Either<Exception, T>> decorateEitherSupplier(RateLimiter rateLimiter,
                                                                     int permits, Supplier<Either<? extends Exception, T>> supplier) {
        return () -> {
            try {
                waitForPermission(rateLimiter, permits);
                return Either.narrow(supplier.get());
            } catch (RequestNotPermitted requestNotPermitted) {
                return Either.left(requestNotPermitted);
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
    static <T> Try<T> executeTrySupplier(RateLimiter rateLimiter,Supplier<Try<T>> supplier) {
        return executeTrySupplier(rateLimiter,1, supplier);
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param permits  number of permits that this call requires
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> Try<T> executeTrySupplier(RateLimiter rateLimiter, int permits, Supplier<Try<T>> supplier) {
        return decorateTrySupplier(rateLimiter, permits, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> Either<Exception, T> executeEitherSupplier(RateLimiter rateLimiter,
        Supplier<Either<? extends Exception, T>> supplier) {
        return executeEitherSupplier(rateLimiter,1, supplier);
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param permits  number of permits that this call requires
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> Either<Exception, T> executeEitherSupplier(RateLimiter rateLimiter, int permits,
                                                           Supplier<Either<? extends Exception, T>> supplier) {
        return decorateEitherSupplier(rateLimiter, permits, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    static <T> T executeCheckedSupplier(RateLimiter rateLimiter, CheckedFunction0<T> checkedSupplier) throws Throwable {
        return executeCheckedSupplier(rateLimiter,1, checkedSupplier);
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
    static <T> T executeCheckedSupplier(RateLimiter rateLimiter, int permits, CheckedFunction0<T> checkedSupplier)
        throws Throwable {
        return decorateCheckedSupplier(rateLimiter, permits, checkedSupplier).apply();
    }
}
