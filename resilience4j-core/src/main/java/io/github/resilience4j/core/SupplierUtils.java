package io.github.resilience4j.core;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class SupplierUtils {

    private SupplierUtils() {
    }

    /**
     * Returns a composed function that first applies the Supplier and then applies the
     * resultHandler.
     *
     * @param <T>           return type of callable
     * @param <R>           return type of handler
     * @param resultHandler the function applied after supplier
     * @return a function composed of supplier and resultHandler
     */
    public static <T, R> Supplier<R> andThen(Supplier<T> supplier, Function<T, R> resultHandler) {
        return () -> resultHandler.apply(supplier.get());
    }

    /**
     * Returns a composed function that first applies the Supplier and then applies {@linkplain
     * BiFunction} {@code after} to the result.
     *
     * @param <T>     return type of after
     * @param handler the function applied after supplier
     * @return a function composed of supplier and handler
     */
    public static <T, R> Supplier<R> andThen(Supplier<T> supplier,
        BiFunction<T, Exception, R> handler) {
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
     * Returns a composed function that first executes the Supplier and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> Supplier<T> recover(Supplier<T> supplier,
        Function<Exception, T> exceptionHandler) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    /**
     * Returns a composed function that first applies the Supplier and then applies either the
     * resultHandler or exceptionHandler.
     *
     * @param <T>              return type of after
     * @param resultHandler    the function applied after Supplier was successful
     * @param exceptionHandler the function applied after Supplier has failed
     * @return a function composed of supplier and handler
     */
    public static <T, R> Supplier<R> andThen(Supplier<T> supplier, Function<T, R> resultHandler,
        Function<Exception, R> exceptionHandler) {
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
