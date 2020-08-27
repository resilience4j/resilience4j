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
package io.github.resilience4j.bulkhead;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.function.Supplier;

public interface VavrBulkhead {
    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the Bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Bulkhead bulkhead,
                                                           CheckedFunction0<T> supplier) {
        return () -> {
            bulkhead.acquirePermission();
            try {
                return supplier.apply();
            } finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     * @return a runnable which is decorated by a Bulkhead.
     */
    static CheckedRunnable decorateCheckedRunnable(Bulkhead bulkhead, CheckedRunnable runnable) {
        return () -> {
            bulkhead.acquirePermission();
            try {
                runnable.run();
            } finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<Try<T>> decorateTrySupplier(Bulkhead bulkhead, Supplier<Try<T>> supplier) {
        return () -> {
            if (bulkhead.tryAcquirePermission()) {
                try {
                    return supplier.get();
                } finally {
                    bulkhead.onComplete();
                }
            } else {
                return Try.failure(BulkheadFullException.createBulkheadFullException(bulkhead));
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<Either<Exception, T>> decorateEitherSupplier(Bulkhead bulkhead,
                                                                     Supplier<Either<? extends Exception, T>> supplier) {
        return () -> {
            if (bulkhead.tryAcquirePermission()) {
                try {
                    Either<? extends Exception, T> result = supplier.get();
                    return Either.narrow(result);
                } finally {
                    bulkhead.onComplete();
                }
            } else {
                return Either.left(BulkheadFullException.createBulkheadFullException(bulkhead));
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a consumer which is decorated by a Bulkhead.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(Bulkhead bulkhead,
                                                          CheckedConsumer<T> consumer) {
        return t -> {
            bulkhead.acquirePermission();
            try {
                consumer.accept(t);
            } finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a function which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the type of the result of the function
     * @return a function which is decorated by a bulkhead.
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Bulkhead bulkhead,
                                                                 CheckedFunction1<T, R> function) {
        return (T t) -> {
            bulkhead.acquirePermission();
            try {
                return function.apply(t);
            } finally {
                bulkhead.onComplete();
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
    static <T> Try<T> executeTrySupplier(Bulkhead bulkhead, Supplier<Try<T>> supplier) {
        return decorateTrySupplier(bulkhead, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> Either<Exception, T> executeEitherSupplier(Bulkhead bulkhead,
        Supplier<Either<? extends Exception, T>> supplier) {
        return decorateEitherSupplier(bulkhead, supplier).get();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    static <T> T executeCheckedSupplier(Bulkhead bulkhead, CheckedFunction0<T> checkedSupplier) throws Throwable {
        return decorateCheckedSupplier(bulkhead, checkedSupplier).apply();
    }
}
