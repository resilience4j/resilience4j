package io.github.resilience4j.retry;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.function.Supplier;

public interface VavrRetry {

    /**
     * Creates a retryable supplier.
     *
     * @param retry    the retry context
     * @param supplier the original function
     * @param <T>      the type of results supplied by this supplier
     * @return a retryable function
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Retry retry, CheckedFunction0<T> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do try {
                T result = supplier.apply();
                final boolean validationOfResult = context.onResult(result);
                if (!validationOfResult) {
                    context.onSuccess();
                    return result;
                }
            } catch (Exception exception) {
                context.onError(exception);
            } while (true);
        };
    }

    /**
     * Creates a retryable runnable.
     *
     * @param retry    the retry context
     * @param runnable the original runnable
     * @return a retryable runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Retry retry, CheckedRunnable runnable) {
        return () -> {
            Retry.Context context = retry.context();
            do try {
                runnable.run();
                context.onSuccess();
                break;
            } catch (Exception exception) {
                context.onError(exception);
            } while (true);
        };
    }




    /**
     * Creates a retryable function.
     *
     * @param retry    the retry context
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the result type of the function
     * @return a retryable function
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Retry retry, CheckedFunction1<T, R> function) {
        return (T t) -> {
            Retry.Context<R> context = retry.context();
            do try {
                R result = function.apply(t);
                final boolean validationOfResult = context.onResult(result);
                if (!validationOfResult) {
                    context.onSuccess();
                    return result;
                }
            } catch (Exception exception) {
                context.onError(exception);
            } while (true);
        };
    }



    /**
     * Creates a retryable supplier.
     *
     * @param retry    the retry context
     * @param supplier the original function
     * @param <T>      the type of results supplied by this supplier
     * @return a retryable function
     */
    static <E extends Exception, T> Supplier<Either<E, T>> decorateEitherSupplier(Retry retry, Supplier<Either<E, T>> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                Either<E, T> result = supplier.get();
                if(result.isRight()){
                    final boolean validationOfResult = context.onResult(result.get());
                    if (!validationOfResult) {
                        context.onSuccess();
                        return result;
                    }
                }else{
                    E exception = result.getLeft();
                    try {
                        context.onError(result.getLeft());
                    } catch (Exception e) {
                        return Either.left(exception);
                    }
                }
            }  while (true);
        };
    }

    /**
     * Creates a retryable supplier.
     *
     * @param retry    the retry context
     * @param supplier the original function
     * @param <T>      the type of results supplied by this supplier
     * @return a retryable function
     */
    static <T> Supplier<Try<T>> decorateTrySupplier(Retry retry, Supplier<Try<T>> supplier) {
        return () -> {
            Retry.Context<T> context = retry.context();
            do {
                Try<T> result = supplier.get();
                if(result.isSuccess()){
                    final boolean validationOfResult = context.onResult(result.get());
                    if (!validationOfResult) {
                        context.onSuccess();
                        return result;
                    }
                }else{
                    Throwable cause = result.getCause();
                    if(cause instanceof Exception){
                        try {
                            context.onError((Exception)result.getCause());
                        } catch (Exception e) {
                            return result;
                        }
                    }
                    else{
                        return result;
                    }
                }
            } while (true);
        };
    }

}
