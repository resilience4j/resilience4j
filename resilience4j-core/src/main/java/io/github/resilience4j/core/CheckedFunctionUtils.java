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

import io.github.resilience4j.core.functions.CheckedBiFunction;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class CheckedFunctionUtils {

    private CheckedFunctionUtils() {
    }


    /**
     * Returns a composed supplier that first executes the supplier and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param supplier the supplier which should be recovered from a certain exception
     * @param exceptionHandler the exception handler
     * @return a supplier composed of callable and exceptionHandler
     */
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
                                                 CheckedFunction<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable throwable) {
                return exceptionHandler.apply(throwable);
            }
        };
    }

    /**
     * Returns a composed supplier that first applies the supplier and then applies {@linkplain
     * BiFunction} {@code after} to the result.
     *
     * @param <T>     return type of callable
     * @param <R>     return type of handler
     * @param supplier the supplier
     * @param handler the supplier applied after callable
     * @return a supplier composed of supplier and handler
     */
    public static <T, R> CheckedSupplier<R> andThen(CheckedSupplier<T> supplier,
        CheckedBiFunction<T, Throwable, R> handler) {
        return () -> {
            try {
                return handler.apply(supplier.get(), null);
            } catch (Throwable throwable) {
                return handler.apply(null, throwable);
            }
        };
    }

    /**
     * Returns a composed supplier that first executes the supplier and optionally recovers from a specific result.
     *
     * @param <T>              return type of after
     * @param supplier the supplier
     * @param resultPredicate the result predicate
     * @param resultHandler the result handler
     * @return a supplier composed of supplier and exceptionHandler
     */
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        Predicate<T> resultPredicate, CheckedFunction<T, T> resultHandler) {
        return () -> {
            T result = supplier.get();
            if(resultPredicate.test(result)){
                return resultHandler.apply(result);
            }
            return result;
        };
    }

    /**
     * Returns a composed supplier that first executes the supplier and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param supplier the supplier which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the exception handler
     * @return a supplier composed of supplier and exceptionHandler
     */
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        List<Class<? extends Throwable>> exceptionTypes,
        CheckedFunction<Throwable, T> exceptionHandler) {
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
     * Returns a composed supplier that first executes the supplier and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param supplier the supplier which should be recovered from a certain exception
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the exception handler
     * @return a supplier composed of callable and exceptionHandler
     */
    public static <X extends Throwable, T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        Class<X> exceptionType,
        CheckedFunction<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable throwable) {
                if(exceptionType.isAssignableFrom(throwable.getClass())) {
                    return exceptionHandler.apply(throwable);
                }else{
                    throw throwable;
                }
            }
        };
    }
}
