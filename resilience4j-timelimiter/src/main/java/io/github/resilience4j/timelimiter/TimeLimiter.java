package io.github.resilience4j.timelimiter;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;
import io.github.resilience4j.timelimiter.internal.TimeLimiterImpl;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.*;
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
     * Creates a TimeLimiter decorator with a default TimeLimiterConfig configuration.
     *
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter ofDefaults(String name) {
        return new TimeLimiterImpl(name, TimeLimiterConfig.ofDefaults());
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
     * @param name              the name of the TimeLimiter
     * @param timeLimiterConfig the TimeLimiterConfig
     * @return The {@link TimeLimiter}
     */
    static TimeLimiter of(String name, TimeLimiterConfig timeLimiterConfig) {
        return new TimeLimiterImpl(name, timeLimiterConfig);
    }

    /**
     * Creates a TimeLimiter with a custom TimeLimiter configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                 the name of the TimeLimiter
     * @param timeLimiterConfig    a custom TimeLimiter configuration
     * @param tags                 tags added to the Retry
     * @return a TimeLimiter with a custom TimeLimiter configuration.
     */
    static TimeLimiter of(String name, TimeLimiterConfig timeLimiterConfig, Map<String, String> tags) {
        return new TimeLimiterImpl(name, timeLimiterConfig, tags);
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
     * @param timeLimiter    the TimeLimiter
     * @param futureSupplier the original future supplier
     * @param <T>            the type of results supplied by the supplier
     * @param <F>            the future type supplied
     * @return a future supplier which is restricted by a {@link TimeLimiter}.
     */
    static <T, F extends Future<T>> Callable<T> decorateFutureSupplier(TimeLimiter timeLimiter,
        Supplier<F> futureSupplier) {
        return timeLimiter.decorateFutureSupplier(futureSupplier);
    }

    /**
     * Decorate a CompletionStage supplier which is decorated by a TimeLimiter
     *
     * @param timeLimiter the TimeLimiter
     * @param scheduler   execution service to use to schedule timeout
     * @param supplier    the original CompletionStage supplier
     * @param <T>         the type of the returned CompletionStage's result
     * @param <F>         the CompletionStage type supplied
     * @return a CompletionStage supplier which is decorated by a TimeLimiter
     */
    static <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
        TimeLimiter timeLimiter, ScheduledExecutorService scheduler, Supplier<F> supplier) {
        return timeLimiter.decorateCompletionStage(scheduler, supplier);
    }

    String getName();

    /**
     * Returns an unmodifiable map with tags assigned to this TimeLimiter.
     *
     * @return the tags assigned to this TimeLimiter in an unmodifiable map
     */
    Map<String, String> getTags();

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
     * @param <T>            the result type of the future
     * @param <F>            the type of Future
     * @return the result of the Future.
     * @throws Exception if unable to compute a result
     */
    default <T, F extends Future<T>> T executeFutureSupplier(Supplier<F> futureSupplier)
        throws Exception {
        return decorateFutureSupplier(this, futureSupplier).call();
    }

    /**
     * Decorates and executes the CompletionStage Supplier
     *
     * @param scheduler execution service to use to schedule timeout
     * @param supplier  the original CompletionStage supplier
     * @param <T>       the type of the returned CompletionStage's result
     * @param <F>       the CompletionStage type supplied
     * @return the decorated CompletionStage
     */
    default <T, F extends CompletionStage<T>> CompletionStage<T> executeCompletionStage(
        ScheduledExecutorService scheduler, Supplier<F> supplier) {
        return decorateCompletionStage(this, scheduler, supplier).get();
    }

    /**
     * Creates a Callback that is restricted by a TimeLimiter.
     *
     * @param futureSupplier the original future supplier
     * @param <T>            the type of results supplied by the supplier
     * @param <F>            the future type supplied
     * @return a future supplier which is restricted by a {@link TimeLimiter}.
     */
    <T, F extends Future<T>> Callable<T> decorateFutureSupplier(Supplier<F> futureSupplier);

    /**
     * Decorate a CompletionStage supplier which is decorated by a TimeLimiter
     *
     * @param scheduler execution service to use to schedule timeout
     * @param supplier  the original CompletionStage supplier
     * @param <T>       the type of the returned CompletionStage's result
     * @param <F>       the CompletionStage type supplied
     * @return a CompletionStage supplier which is decorated by a TimeLimiter
     */
    <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
        ScheduledExecutorService scheduler, Supplier<F> supplier);

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Records a successful call.
     * <p>
     * This method must be invoked when a call was successful.
     */
    void onSuccess();

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
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

    static TimeoutException createdTimeoutExceptionWithName(String name, @Nullable Throwable t) {
        final TimeoutException timeoutException = new TimeoutException(String.format("TimeLimiter '%s' recorded a timeout exception.", name));
        if(t != null){
            timeoutException.setStackTrace(t.getStackTrace());
        }
        return timeoutException;
    }
}
