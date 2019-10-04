package io.github.resilience4j.timelimiter;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;
import io.github.resilience4j.timelimiter.internal.TimeLimiterImpl;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * A TimeLimiter decorator stops execution after a configurable duration.
 */
public interface TimeLimiter {

    String DEFAULT_NAME = "UNDEFINED";

    /**
     * Creates a TimeLimiter decorator with a default TimeLimiterConfig configuration.
     *
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter ofDefaults() {
        return new TimeLimiterImpl(DEFAULT_NAME, TimeLimiterConfig.ofDefaults());
    }

    /**
     * Creates a TimeLimiter decorator with a TimeLimiterConfig configuration.
     *
     * @param timeLimiterConfig the TimeLimiterConfig
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter of(TimeLimiterConfig timeLimiterConfig) {
        return of(DEFAULT_NAME, timeLimiterConfig);
    }

    /**
     * Creates a TimeLimiter decorator with a TimeLimiterConfig configuration.
     *
     * @param name the name of the TimeLimiter
     * @param timeLimiterConfig the TimeLimiterConfig
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter of(String name, TimeLimiterConfig timeLimiterConfig) {
        return new TimeLimiterImpl(name, timeLimiterConfig);
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
        return new TimeLimiterImpl(DEFAULT_NAME, timeLimiterConfig);
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
        return timeLimiter.decorateFutureSupplier(futureSupplier);
    }

    String getName();

    /**
     * Get the TimeLimiterConfig of this TimeLimiter decorator.
     *
     * @return the TimeLimiterConfig of this TimeLimiter decorator
     */
    TimeLimiterConfig getTimeLimiterConfig();

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

    /**
     * Creates a Callback that is restricted by a TimeLimiter.
     *
     * @param futureSupplier the original future supplier
     * @param <T> the type of results supplied by the supplier
     * @param <F> the future type supplied
     * @return a future supplier which is restricted by a {@link TimeLimiter}.
     */
    <T, F extends Future<T>> Callable<T> decorateFutureSupplier(Supplier<F> futureSupplier);

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Records a successful call.
     *
     * This method must be invoked when a call was successful.
     */
    void onSuccess();

    /**
     * Records a failed call.
     * This method must be invoked when a call failed.
     * @param throwable The throwable which must be recorded
     */
    void onError(Throwable throwable);

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<TimeLimiterEvent> {

        EventPublisher onSuccess(EventConsumer<TimeLimiterOnSuccessEvent> eventConsumer);

        EventPublisher onError(EventConsumer<TimeLimiterOnErrorEvent> eventConsumer);

        EventPublisher onTimeout(EventConsumer<TimeLimiterOnTimeoutEvent> eventConsumer);

    }
}
