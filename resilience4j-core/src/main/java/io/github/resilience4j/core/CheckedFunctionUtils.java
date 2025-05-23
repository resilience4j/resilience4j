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
import java.util.function.UnaryOperator;

public class CheckedFunctionUtils {

    private CheckedFunctionUtils() {
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, CheckedFunction)}
     */
    @Deprecated
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
                                                 CheckedFunction<Throwable, T> exceptionHandler) {
        return CheckedSupplierUtils.recover(supplier, exceptionHandler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#andThen(CheckedSupplier, CheckedBiFunction)}
     */
    @Deprecated
    public static <T, R> CheckedSupplier<R> andThen(CheckedSupplier<T> supplier,
        CheckedBiFunction<T, Throwable, R> handler) {
        return CheckedSupplierUtils.andThen(supplier, handler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, Predicate, CheckedFunction)}
     */
    @Deprecated
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        Predicate<T> resultPredicate, CheckedFunction<T, T> resultHandler) {
        return CheckedSupplierUtils.recover(supplier, resultPredicate, resultHandler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, List, CheckedFunction)}
     */
    @Deprecated
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        List<Class<? extends Throwable>> exceptionTypes,
        CheckedFunction<Throwable, T> exceptionHandler) {
        return CheckedSupplierUtils.recover(supplier, exceptionTypes, exceptionHandler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, Class, CheckedFunction)}
     */
    public static <X extends Throwable, T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        Class<X> exceptionType,
        CheckedFunction<Throwable, T> exceptionHandler) {
        return CheckedSupplierUtils.recover(supplier, exceptionType, exceptionHandler);
    }

    /**
     * Returns a composed function that first applies the CheckedFunction and then applies the
     * resultHandler.
     *
     * @param <T>           function parameter type
     * @param <U>           return type of function
     * @param <R>           return type of handler
     * @param function the function
     * @param resultHandler the function applied after function
     * @return a function composed of supplier and resultHandler
     */
    public static <T, U, R> CheckedFunction<T, R> andThen(CheckedFunction<T, U> function, CheckedFunction<U, R> resultHandler) {
        return t -> resultHandler.apply(function.apply(t));
    }

    /**
     * Returns a composed function that first applies the CheckedFunction and then applies {@linkplain
     * BiFunction} {@code after} to the result.
     *
     * @param <T>           function parameter type
     * @param <U>           return type of function
     * @param <R>           return type of handler
     * @param function the function
     * @param handler the function applied after function
     * @return a function composed of supplier and handler
     */
    public static <T, U, R> CheckedFunction<T, R> andThen(CheckedFunction<T, U> function,
                                                   BiFunction<U, Throwable, R> handler) {
        return t -> {
            try {
                U result = function.apply(t);
                return handler.apply(result, null);
            } catch (Exception exception) {
                return handler.apply(null, exception);
            }
        };
    }

    /**
     * Returns a composed function that first applies the CheckedFunction and then applies either the
     * resultHandler or exceptionHandler.
     *
     * @param <T>           function parameter type
     * @param <U>           return type of function
     * @param <R>           return type of handler
     * @param function the function
     * @param resultHandler    the function applied after function was successful
     * @param exceptionHandler the function applied after function has failed
     * @return a function composed of supplier and handler
     */
    public static <T, U, R> CheckedFunction<T, R> andThen(CheckedFunction<T, U> function, CheckedFunction<U, R> resultHandler,
                                                          CheckedFunction<Throwable, R> exceptionHandler) {
        return t -> {
            try {
                U result = function.apply(t);
                return resultHandler.apply(result);
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    /**
     * Returns a composed function that first executes the CheckedFunction and optionally recovers from an
     * exception.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param function the function which should be recovered from a certain exception
     * @param exceptionHandler the exception handler
     * @return a function composed of function and exceptionHandler
     */
    public static <T, R> CheckedFunction<T, R> recover(CheckedFunction<T, R> function,
                                                CheckedFunction<Throwable, R> exceptionHandler) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    /**
     * Returns a composed CheckedFunction that first executes the CheckedFunction and optionally recovers from a specific result.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param function the function which should be recovered from a certain exception
     * @param resultPredicate the result predicate
     * @param resultHandler the result handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T, R> CheckedFunction<T, R> recover(CheckedFunction<T, R> function,
                                                Predicate<R> resultPredicate, UnaryOperator<R> resultHandler) {
        return t -> {
            R result = function.apply(t);
            if(resultPredicate.test(result)){
                return resultHandler.apply(result);
            }
            return result;
        };
    }

    /**
     * Returns a composed function that first executes the CheckedFunction and optionally recovers from an
     * exception.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param function the function which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T, R> CheckedFunction<T, R> recover(CheckedFunction<T, R> function,
                                                List<Class<? extends Throwable>> exceptionTypes,
                                                       CheckedFunction<Throwable, R> exceptionHandler) {
        return t -> {
            try {
                return function.apply(t);
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
     * Returns a composed function that first executes the CheckedFunction and optionally recovers from an
     * exception.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of function and exceptionHandler
     */
    public static <X extends Throwable, T, R> CheckedFunction<T, R> recover(CheckedFunction<T, R> function,
                                                                     Class<X> exceptionType,
                                                                     CheckedFunction<Throwable, R> exceptionHandler) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception exception) {
                if(exceptionType.isAssignableFrom(exception.getClass())) {
                    return exceptionHandler.apply(exception);
                }else{
                    throw exception;
                }
            }
        };
    }
}
