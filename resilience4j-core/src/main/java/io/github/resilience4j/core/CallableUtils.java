package io.github.resilience4j.core;

import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CallableUtils {

    private CallableUtils() {
    }

    /**
     * Returns a composed function that first applies the Callable and then applies the
     * resultHandler.
     *
     * @param <T>           return type of callable
     * @param <R>           return type of handler
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
     * @param handler the function applied after callable
     * @return a function composed of supplier and handler
     */
    public static <T, R> Callable<R> andThen(Callable<T> callable,
        BiFunction<T, Exception, R> handler) {
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
     * @param resultHandler    the function applied after callable was successful
     * @param exceptionHandler the function applied after callable has failed
     * @return a function composed of supplier and handler
     */
    public static <T, R> Callable<R> andThen(Callable<T> callable, Function<T, R> resultHandler,
        Function<Exception, R> exceptionHandler) {
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
     * @param exceptionHandler the exception handler
     * @return a function composed of callable and exceptionHandler
     */
    public static <T> Callable<T> recover(Callable<T> callable,
        Function<Exception, T> exceptionHandler) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }
}
