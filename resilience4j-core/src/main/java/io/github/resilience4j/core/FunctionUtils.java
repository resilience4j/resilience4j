package io.github.resilience4j.core;


import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class FunctionUtils {

    private FunctionUtils() {
    }

    /**
     * Returns a composed function that first applies the Function and then applies the
     * resultHandler.
     *
     * @param <T>           function parameter type
     * @param <U>           return type of function
     * @param <R>           return type of handler
     * @param function the function
     * @param resultHandler the function applied after function
     * @return a function composed of supplier and resultHandler
     */
    public static <T, U, R> Function<T, R> andThen(Function<T, U> function, Function<U, R> resultHandler) {
        return t -> resultHandler.apply(function.apply(t));
    }

    /**
     * Returns a composed function that first applies the Function and then applies {@linkplain
     * BiFunction} {@code after} to the result.
     *
     * @param <T>           function parameter type
     * @param <U>           return type of function
     * @param <R>           return type of handler
     * @param function the function
     * @param handler the function applied after function
     * @return a function composed of supplier and handler
     */
    public static <T, U, R> Function<T, R> andThen(Function<T, U> function,
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
     * Returns a composed function that first applies the Function and then applies either the
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
    public static <T, U, R> Function<T, R> andThen(Function<T, U> function, Function<U, R> resultHandler,
        Function<Throwable, R> exceptionHandler) {
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
     * Returns a composed function that first executes the Function and optionally recovers from an
     * exception.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param function the function which should be recovered from a certain exception
     * @param exceptionHandler the exception handler
     * @return a function composed of function and exceptionHandler
     */
    public static <T, R> Function<T, R> recover(Function<T, R> function,
        Function<Throwable, R> exceptionHandler) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    /**
     * Returns a composed Function that first executes the Function and optionally recovers from a specific result.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param function the function which should be recovered from a certain exception
     * @param resultPredicate the result predicate
     * @param resultHandler the result handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T, R> Function<T, R> recover(Function<T, R> function,
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
     * Returns a composed function that first executes the Function and optionally recovers from an
     * exception.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param function the function which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T, R> Function<T, R> recover(Function<T, R> function,
        List<Class<? extends Throwable>> exceptionTypes,
        Function<Throwable, R> exceptionHandler) {
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
     * Returns a composed function that first executes the Function and optionally recovers from an
     * exception.
     *
     * @param <T>           function parameter type
     * @param <R>           return type of function
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of function and exceptionHandler
     */
    public static <X extends Throwable, T, R> Function<T, R> recover(Function<T, R> function,
        Class<X> exceptionType,
        Function<Throwable, R> exceptionHandler) {
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
