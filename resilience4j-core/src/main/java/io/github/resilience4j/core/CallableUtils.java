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
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class CallableUtils {

    private CallableUtils() {
    }

    /**
     * Returns a composed function that first applies the Callable and then applies the
     * resultHandler.
     *
     * @param <T>           return type of callable
     * @param <R>           return type of handler
     * @param callable the callable
     * @param resultHandler the function applied after callable
     * @return a function composed of supplier and resultHandler
     */
    public static <T, R> Callable<R> andThen(Callable<T> callable, Function<T, R> resultHandler) {
        return () -> resultHandler.apply(callable.call());
    }

    /**
     * Returns a composed function that first applies the Callable and then applies {@linkplain
     * BiFunction} {@code after} to the result.
     *
     * @param <T>     return type of callable
     * @param <R>     return type of handler
     * @param callable the callable
     * @param handler the function applied after callable
     * @return a function composed of supplier and handler
     */
    public static <T, R> Callable<R> andThen(Callable<T> callable,
        BiFunction<T, Throwable, R> handler) {
        return () -> {
            try {
                T result = callable.call();
                return handler.apply(result, null);
            } catch (Exception exception) {
                return handler.apply(null, exception);
            }
        };
    }

    /**
     * Returns a composed function that first applies the Callable and then applies either the
     * resultHandler or exceptionHandler.
     *
     * @param <T>              return type of callable
     * @param <R>              return type of resultHandler and exceptionHandler
     * @param callable the callable
     * @param resultHandler    the function applied after callable was successful
     * @param exceptionHandler the function applied after callable has failed
     * @return a function composed of supplier and handler
     */
    public static <T, R> Callable<R> andThen(Callable<T> callable, Function<T, R> resultHandler,
        Function<Throwable, R> exceptionHandler) {
        return () -> {
            try {
                T result = callable.call();
                return resultHandler.apply(result);
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    /**
     * Returns a composed function that first executes the Callable and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param callable the callable which should be recovered from a certain exception
     * @param exceptionHandler the exception handler
     * @return a function composed of callable and exceptionHandler
     */
    public static <T> Callable<T> recover(Callable<T> callable,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    /**
     * Returns a composed Callable that first executes the Callable and optionally recovers from a specific result.
     *
     * @param <T>              return type of after
     * @param callable the callable
     * @param resultPredicate the result predicate
     * @param resultHandler the result handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> Callable<T> recover(Callable<T> callable,
        Predicate<T> resultPredicate, UnaryOperator<T> resultHandler) {
        return () -> {
            T result = callable.call();
            if(resultPredicate.test(result)){
                return resultHandler.apply(result);
            }
            return result;
        };
    }

    /**
     * Returns a composed function that first executes the Callable and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param callable the callable which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> Callable<T> recover(Callable<T> callable,
        List<Class<? extends Throwable>> exceptionTypes,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return callable.call();
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
     * Returns a composed function that first executes the Callable and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param callable the callable which should be recovered from a certain exception
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of callable and exceptionHandler
     */
    public static <X extends Throwable, T> Callable<T> recover(Callable<T> callable,
        Class<X> exceptionType,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return callable.call();
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
