package io.github.resilience4j.timeout;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import io.github.resilience4j.timeout.internal.FutureTimeoutProxy;
import io.github.resilience4j.timeout.internal.TimeoutContext;

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
        return new TimeoutContext(timeoutConfig);
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
     * Creates a future which is restricted by a Timeout.
     *
     * @param timeout   the {@link Timeout}
     * @param future    the original future
     * @param <T> the type of results supplied supplier
     * @param <F> the future type supplied
     * @return a future which is restricted by a {@link Timeout}.
     */
    static <T, F extends Future<T>> F decorateFuture(Timeout timeout, F future) {
        return waitForFuture(timeout, future);
    }

    /**
     * Creates a future which is restricted by a Timeout.
     *
     * @param timeout        the {@link Timeout}
     * @param futureSupplier the original future supplier
     * @param <T> the type of results supplied supplier
     * @param <F> the future type supplied
     * @return a future supplier which is restricted by a {@link Timeout}.
     */
    static <T, F extends Future<T>> Supplier<F> decorateFutureSupplier(Timeout timeout, Supplier<F> futureSupplier) {
        return waitForFutureSupplier(timeout, futureSupplier);
    }

    /**
     * Get the TimeoutConfig of this Timeout decorator.
     *
     * @return the TimeoutConfig of this Timeout decorator
     */
    TimeoutConfig getTimeoutConfig();

    /**
     * Decorates and executes the decorated Future.
     *
     * @param future the original Future
     *
     * @return the result of the decorated Future.
     * @param <T> the result type of the future
     * @param <F> the type of Future
     * @throws Exception if unable to compute a result
     */
    default <T, F extends Future<T>> T executeFuture(F future) throws Exception{
        return decorateFuture(this, future).get();
    }

    /**
     * Decorates and executes the decorated future supplier.
     *
     * @param futureSupplier the original future Supplier
     *
     * @return the result of the decorated Supplier.
     * @param <T> the result type of the future
     * @param <F> the type of Future
     * @throws Exception if unable to compute a result
     */
    default <T, F extends Future<T>> F executeFutureSupplier(Supplier<F> futureSupplier) throws Exception{
        return decorateFutureSupplier(this, futureSupplier).get();
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout     the Timeout
     * @param future      the original future
     * @param <T> the type of results from the future
     * @param <F> the type of Future
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T, F extends Future<T>> F waitForFuture(final Timeout timeout, final F future) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        return (F) FutureTimeoutProxy.getProxy(future, timeoutDuration);
    }

    /**
     * Will wait for completion within default timeout duration.
     *
     * @param timeout           the Timeout
     * @param futureSupplier    the original future supplier
     * @param <T> the type of result from the future
     * @param <F> the type of Future
     * @throws TimeoutException if waiting time elapsed before executed completion.
     */
    static <T, F extends Future<T>> Supplier<F> waitForFutureSupplier(final Timeout timeout, final Supplier<F> futureSupplier) throws TimeoutException {
        TimeoutConfig timeoutConfig = timeout.getTimeoutConfig();
        Duration timeoutDuration = timeoutConfig.getTimeoutDuration();

        return () -> FutureTimeoutProxy.getProxy(futureSupplier.get(), timeoutDuration);
    }

}
