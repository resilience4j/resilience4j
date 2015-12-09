package io.github.robwin.retry;

import javaslang.control.Try;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Retry {

    /**
     * Checks if the call should be retried
     *
     * @return the maximum number of attempts
     * @throws Exception the original exception if the maximum retry attempts are exceeded
     */
    boolean isRetryAllowedAfterException() throws Exception;

    /**
     * Checks if the call should be retried
     *
     * @return the maximum number of attempts
     */
    boolean isRetryAllowedAfterRuntimeException();

    /**
     * Handles a checked exception
     *
     * @param exception the exception to handle
     * @throws Throwable the exception
     */
    void handleException(Exception exception) throws Throwable;

    /**
     * Handles a runtime exception
     *
     * @param runtimeException the exception to handle
     */
    void handleRuntimeException(RuntimeException runtimeException);

    /**
     * Creates a RetryContext.Builder to configure a custom Retry.
     *
     * @return a RetryContext.Builder
     */
    static RetryContext.Builder custom(){
        return new RetryContext.Builder();
    }

    /**
     * Creates a Retry with default configuration.
     *
     * @return a Retry with default configuration
     */
    static Retry ofDefaults(){
        return Retry.custom().build();
    }

    static <T> Try.CheckedSupplier<T> retryableCheckedSupplier(Try.CheckedSupplier<T> supplier, Retry retryContext){
        return () -> {
            do try {
                return supplier.get();
            } catch (Exception exception) {
                retryContext.handleException(exception);
            } while (retryContext.isRetryAllowedAfterException());
            // Should never reach this code
            return null;
        };
    }

    static Try.CheckedRunnable retryableCheckedRunnable(Try.CheckedRunnable runnable, Retry retryContext){
        return () -> {
            do try {
                runnable.run();
                break;
            } catch (Exception exception) {
                retryContext.handleException(exception);
            } while (retryContext.isRetryAllowedAfterException());
        };
    }

    static <T, R> Try.CheckedFunction<T, R> retryableCheckedFunction(Try.CheckedFunction<T, R> function, Retry retryContext){
        return (T t) -> {
            do try {
                return function.apply(t);
            } catch (Exception exception) {
                retryContext.handleException(exception);
            } while (retryContext.isRetryAllowedAfterException());
            // Should never reach this code
            return null;
        };
    }

    static <T> Supplier<T> retryableSupplier(Supplier<T> supplier, Retry retryContext){
        return () -> {
            do try {
                return supplier.get();
            } catch (RuntimeException runtimeException) {
                retryContext.handleRuntimeException(runtimeException);
            } while (retryContext.isRetryAllowedAfterRuntimeException());
            // Should never reach this code
            return null;
        };
    }

    static Runnable retryableRunnable(Runnable runnable, Retry retryContext){
        return () -> {
            do try {
                runnable.run();
                break;
            } catch (RuntimeException runtimeException) {
                retryContext.handleRuntimeException(runtimeException);
            } while (retryContext.isRetryAllowedAfterRuntimeException());
        };
    }

    static <T, R> Function<T, R> retryableFunction(Function<T, R> function, Retry retryContext){
        return (T t) -> {
            do try {
                return function.apply(t);
            } catch (RuntimeException runtimeException) {
                retryContext.handleRuntimeException(runtimeException);
            } while (retryContext.isRetryAllowedAfterRuntimeException());
            // Should never reach this code
            return null;
        };
    }

}
