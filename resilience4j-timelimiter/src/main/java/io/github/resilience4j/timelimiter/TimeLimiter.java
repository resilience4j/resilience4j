package io.github.resilience4j.timelimiter;

import io.github.resilience4j.timelimiter.internal.TimeLimiterImpl;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * A TimeLimiter decorator stops execution after a configurable duration.
 */
public interface TimeLimiter {

    /**
     * Creates a TimeLimiter decorator with a default TimeLimiterConfig configuration.
     *
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter ofDefaults() {
        return new TimeLimiterImpl(TimeLimiterConfig.ofDefaults());
    }

    /**
     * Creates a TimeLimiter decorator with a TimeLimiterConfig configuration.
     *
     * @param timeLimiterConfig the TimeLimiterConfig
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter of(TimeLimiterConfig timeLimiterConfig) {
        return new TimeLimiterImpl(timeLimiterConfig);
    }

    /**
     * Creates a TimeLimiter decorator with a timeout Duration.
     *
     * @param timeoutDuration the timeout Duration
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter of(Duration timeoutDuration) {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(timeoutDuration)
                .build();

        return new TimeLimiterImpl(timeLimiterConfig);
    }

    /**
     * Creates a Callable that is restricted by a TimeLimiter.
     *
     * @param timeLimiter   the TimeLimiter
     * @param future        the original future
     * @param <T> the type of results supplied by the future
     * @param <F> the future type supplied
     * @return a future which is restricted by a {@link TimeLimiter}.
     */
    static <T, F extends Future<T>> Callable<T> decorateFuture(final TimeLimiter timeLimiter, final F future) {
        return () -> {
            try {
                return future.get(timeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                if (timeLimiter.getTimeLimiterConfig().shouldCancelRunningFuture()) {
                    future.cancel(true);
                }
                throw e;
            }
        };
    }

    /**
     * Creates a Callback that is restricted by a TimeLimiter.
     *
     * @param timeLimiter        the TimeLimiter
     * @param futureSupplier     the original future supplier
     * @param <T> the type of results supplied by the supplier
     * @param <F> the future type supplied
     * @return a future supplier which is restricted by a {@link TimeLimiter}.
     */
    static <T, F extends Future<T>> Callable<T> decorateFutureSupplier(TimeLimiter timeLimiter, Supplier<F> futureSupplier) {
        return () -> {
            Future<T> future = futureSupplier.get();
            try {
                return future.get(timeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                if(timeLimiter.getTimeLimiterConfig().shouldCancelRunningFuture()){
                    future.cancel(true);
                }
                throw e;
            }
        };
    }

    /**
     * Get the TimeLimiterConfig of this TimeLimiter decorator.
     *
     * @return the TimeLimiterConfig of this TimeLimiter decorator
     */
    TimeLimiterConfig getTimeLimiterConfig();

    /**
     * Decorates and executes the Future.
     *
     * @param future the original Future
     * @param <T> the result type of the future
     * @param <F> the type of Future
     * @return the result of the decorated Future.
     * @throws Exception if unable to compute a result
     */
    default <T, F extends Future<T>> T executeFuture(F future) throws Exception {
        return decorateFuture(this, future).call();
    }

    /**
     * Decorates and executes the Future Supplier.
     *
     * @param futureSupplier the original future supplier
     * @param <T> the result type of the future
     * @param <F> the type of Future
     * @return the result of the Future.
     * @throws Exception if unable to compute a result
     */
    default <T, F extends Future<T>> T executeFutureSupplier(Supplier<F> futureSupplier) throws Exception {
        return decorateFutureSupplier(this, futureSupplier).call();
    }
}
