/*
 *
 *  Copyright 2020: Robert Winkler
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
package io.github.resilience4j.core;

import java.util.List;
import java.util.function.*;

public class SupplierUtils {

    private SupplierUtils() {
    }

    /**
     * Returns a composed Supplier that first applies the Supplier and then applies the
     * resultHandler.
     *
     * @param <T>           return type of callable
     * @param <R>           return type of handler
     * @param supplier the supplier
     * @param resultHandler the function applied after supplier
     * @return a function composed of supplier and resultHandler
     */
    public static <T, R> Supplier<R> andThen(Supplier<T> supplier, Function<T, R> resultHandler) {
        return () -> resultHandler.apply(supplier.get());
    }

    /**
     * Returns a composed Supplier that first applies the Supplier and then applies {@linkplain
     * BiFunction} {@code after} to the result.
     *
     * @param <T>     return type of after
     * @param supplier the supplier
     * @param handler the function applied after supplier
     * @return a function composed of supplier and handler
     */
    public static <T, R> Supplier<R> andThen(Supplier<T> supplier,
        BiFunction<T, Throwable, R> handler) {
        return () -> {
            try {
                T result = supplier.get();
                return handler.apply(result, null);
            } catch (Exception exception) {
                return handler.apply(null, exception);
            }
        };
    }

    /**
     * Returns a composed Supplier that first executes the Supplier and optionally recovers from a specific result.
     *
     * @param <T>              return type of after
     * @param supplier the supplier
     * @param resultPredicate the result predicate
     * @param resultHandler the result handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> Supplier<T> recover(Supplier<T> supplier,
        Predicate<T> resultPredicate, UnaryOperator<T> resultHandler) {
        return () -> {
            T result = supplier.get();
            if(resultPredicate.test(result)){
                return resultHandler.apply(result);
            }
            return result;
        };
    }

    /**
     * Returns a composed function that first executes the Supplier and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param supplier the supplier
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> Supplier<T> recover(Supplier<T> supplier,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    /**
     * Returns a composed function that first executes the Supplier and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param supplier the supplier which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> Supplier<T> recover(Supplier<T> supplier,
        List<Class<? extends Throwable>> exceptionTypes,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception exception) {
                if(exceptionTypes.stream().anyMatch(exceptionType -> exceptionType.isAssignableFrom(exception.getClass()))){
                    return exceptionHandler.apply(exception);
                }else{
                    throw exception;
                }
            }
        };
    }

    /**
     * Returns a composed function that first executes the Supplier and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param supplier the supplier which should be recovered from a certain exception
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <X extends Throwable, T> Supplier<T> recover(Supplier<T> supplier,
        Class<X> exceptionType,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return supplier.get();
            } catch (RuntimeException exception) {
                if(exceptionType.isAssignableFrom(exception.getClass())) {
                    return exceptionHandler.apply(exception);
                }else{
                    throw exception;
                }
            }
        };
    }

    /**
     * Returns a composed function that first applies the Supplier and then applies either the
     * resultHandler or exceptionHandler.
     *
     * @param <T>              return type of after
     * @param supplier the supplier which should be recovered from a certain exception
     * @param resultHandler    the function applied after Supplier was successful
     * @param exceptionHandler the function applied after Supplier has failed
     * @return a function composed of supplier and handler
     */
    public static <T, R> Supplier<R> andThen(Supplier<T> supplier, Function<T, R> resultHandler,
        Function<Throwable, R> exceptionHandler) {
        return () -> {
            try {
                T result = supplier.get();
                return resultHandler.apply(result);
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }
}
