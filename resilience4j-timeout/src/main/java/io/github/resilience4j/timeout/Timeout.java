package io.github.resilience4j.timeout;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.timeout.internal.TimeoutContext;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;

/**
 * A Timeout decorator stops execution at a configurable rate.
 */
public interface Timeout {

    /**
     * Creates a Timeout decorator with a default TimeoutConfig configuration.
     *
     * @return The {@link Timeout}
     */
    static Timeout ofDefaults() {
        return new TimeoutContext(TimeoutConfig.ofDefaults());
    }

    /**
     * Creates a Timeout decorator with a TimeoutConfig configuration.
     *
     * @param timeoutConfig the TimeoutConfig
     * @return The {@link Timeout}
     */
    static Timeout of(TimeoutConfig timeoutConfig) {
        return new TimeoutContext(TimeoutConfig.ofDefaults());
    }

    /**
     * Creates a Timeout decorator with a timeout Duration.
     *
     * @param timeoutDuration the timeout Duration
     * @return The {@link Timeout}
     */
    static Timeout of(Duration timeoutDuration) {
        TimeoutConfig timeoutConfig = TimeoutConfig.custom()
                .timeoutDuration(timeoutDuration)
                .build();

        return new TimeoutContext(timeoutConfig);
    }

    /**
     * Creates a supplier which is restricted by a Timeout.
     *
     * @param timeout     the Timeout
     * @param supplier    the original supplier
     * @param <T> the type of results supplied supplier
     * @return a supplier which is restricted by a Timeout.
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Timeout timeout, CheckedFunction0<T> supplier) {
        return () -> waitForCheckedFunction0(timeout, supplier);
    }

    /**
     * Creates a runnable which is restricted by a Timeout.
     *
     * @param timeout     the Timeout
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a Timeout.
     */
    static CheckedRunnable decorateCheckedRunnable(Timeout timeout, CheckedRunnable runnable) {
        return () -> waitForCheckedRunnable(timeout, runnable);
    }

    /**
     * Creates a function which is restricted by a Timeout.
     *
     * @param timeout the Timeout
     * @param function    the original function
     * @param <T> the type of function argument
     * @param <R> the type of function results
     * @return a function which is restricted by a Timeout.
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Timeout timeout, CheckedFunction1<T, R> function) {
        return (T t) -> waitForCheckedFunction1(timeout, function, t);
    }

    /**
     * Creates a supplier which is restricted by a Timeout.
     *
     * @param timeout     the Timeout
     * @param supplier    the original supplier
     * @param <T> the type of results supplied supplier
     * @return a supplier which is restricted by a Timeout.
     */
    static <T> Supplier<T> decorateSupplier(Timeout timeout, Supplier<T> supplier) {
        return () -> waitForSupplier(timeout, supplier);
    }

    /**
     * Creates a callable which is restricted by a Timeout.
     *
     * @param timeout     the Timeout
     * @param callable    the original callable
     * @param <T> the type of results supplied by the callable
     * @return a callable which is restricted by a Timeout.
     */
    static <T> Callable<T> decorateCallable(Timeout timeout, Callable<T> callable) {
        return () -> waitForCallable(timeout, callable);
    }

    /**
     * Creates a consumer which is restricted by a Timeout.
     *
     * @param timeout     the Timeout
     * @param consumer    the original consumer
     * @param <T> the type of the input to the consumer
     * @return a consumer which is restricted by a Timeout.
     */
    static <T> Consumer<T> decorateConsumer(Timeout timeout, Consumer<T> consumer) {
        return (T t) -> waitForConsumer(timeout, consumer, t);
    }

    /**
     * Creates a runnable which is restricted by a Timeout.
     *
     * @param timeout     the Timeout
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a Timeout.
     */
    static Runnable decorateRunnable(Timeout timeout, Runnable runnable) {
        return () -> waitForRunnable(timeout, runnable);
    }

    /**
     * Creates a function which is restricted by a Timeout.
     *
     * @param timeout     the Timeout
     * @param function    the original function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return a function which is restricted by a Timeout.
     */
    static <T, R> Function<T, R> decorateFunction(Timeout timeout, Function<T, R> function) {
        return (T t) -> waitForFunction(timeout, function, t);
    }

    /**
     * Get the TimeoutConfig of this Timeout decorator.
     *
     * @return the TimeoutConfig of this Timeout decorator
     */
    TimeoutConfig getTimeoutConfig();

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T> the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier){
        return decorateSupplier(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     *
     * @return the result of the decorated Callable.
     * @param <T> the result type of callable
     * @throws Exception if unable to compute a result
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception{
        return decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable){
        decorateRunnable(this, runnable).run();
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param supplier    the original supplier
     * @param <T> the type of results supplied supplier
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T> T waitForCheckedFunction0(final Timeout timeout, final CheckedFunction0<T> supplier) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<T> task = executorService.submit(() -> Try.of(supplier)
                .getOrElseThrow((Function<? super Throwable, ExecutionException>) ExecutionException::new));

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param runnable    the original runnable
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static Void waitForCheckedRunnable(final Timeout timeout, final CheckedRunnable runnable) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<Void> task = executorService.submit(() -> Try.run(runnable)
                .getOrElseThrow((Function<? super Throwable, ExecutionException>) ExecutionException::new));

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param function    the original function
     * @param t           the consumed value
     * @param <T> the type of results supplied function
     * @param <R> the type of the result of the function
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T,R> R waitForCheckedFunction1(final Timeout timeout, final CheckedFunction1<T,R> function, final T t) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<R> task = executorService.submit(() -> Try.of(() -> function.apply(t))
        .getOrElseThrow((Function<? super Throwable, ExecutionException>) ExecutionException::new));

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param supplier    the original supplier
     * @param <T> the type of results supplied supplier
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T> T waitForSupplier(final Timeout timeout, final Supplier<T> supplier) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<T> task = executorService.submit(supplier::get);

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param callable    the original callable
     * @param <T> the type of results supplied callable
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T> T waitForCallable(final Timeout timeout, final Callable<T> callable) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<T> task = executorService.submit(callable);

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param consumer    the original consumer
     * @param t           the consumed value
     * @param <T> the type of results supplied consumer
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T> Void waitForConsumer(final Timeout timeout, final Consumer<T> consumer, final T t) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<Void> task = executorService.submit(() -> {
            consumer.accept(t);
            return null;
        });

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param runnable    the original runnable
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static Void waitForRunnable(final Timeout timeout, final Runnable runnable) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<Void> task = executorService.submit(() -> {
            runnable.run();
            return null;
        });

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param function    the original function
     * @param t           the consumed value
     * @param <T> the type of results supplied function
     * @param <R> the type of the result of the function
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T,R> R waitForFunction(final Timeout timeout, final Function<T,R> function, final T t) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<R> task = executorService.submit(() -> function.apply(t));

        return Try.of(() -> task.get(timeoutDuration.getNano(), TimeUnit.NANOSECONDS))
                .getOrElseThrow(throwable -> new TimeoutException(throwable.getCause()));
    }
}
