package io.github.resilience4j.hedge;

import io.github.resilience4j.hedge.internal.ExecutorServiceHedge;
import io.github.resilience4j.hedge.internal.HedgeDurationSupplier;
import io.github.resilience4j.hedge.internal.HedgeImpl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public interface GenericHedge {

    /**
     * Creates a Hedge with a HedgeConfig configuration.
     *
     * @param hedgeConfig the HedgeConfig
     * @return The Hedge
     */
    static GenericHedge of(SimpleHedgeConfig hedgeConfig, ScheduledExecutorService executorService) {
        return of(Hedge.DEFAULT_NAME, hedgeConfig, executorService);
    }

    /**
     * Creates a Hedge with a HedgeConfig configuration.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig the HedgeConfig
     * @return The Hedge
     */
    static GenericHedge of(String name, SimpleHedgeConfig hedgeConfig, ScheduledExecutorService executorService) {
        return new ExecutorServiceHedge<>(name, emptyMap(), hedgeConfig, executorService);
    }

    /**
     * Creates a Hedge with a custom Hedge configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @param tags        tags added to the Hedge
     * @return a Hedge with a custom Hedge configuration.
     */
    static GenericHedge of(String name, SimpleHedgeConfig hedgeConfig,
                    Map<String, String> tags, ScheduledExecutorService executorService) {
        return new ExecutorServiceHedge<>(name, tags, hedgeConfig, executorService);
    }

    /**
     * Creates a Hedge decorator with a timeout Duration.
     *
     * @param hedgeDuration the timeout Duration
     * @return The Hedge
     */
    static GenericHedge of(Duration hedgeDuration, ScheduledExecutorService executorService) {
        SimpleHedgeConfig hedgeConfig = new SimpleHedgeConfig.Builder()
                .preconfiguredDuration(hedgeDuration)
                .build();
        return of(Hedge.DEFAULT_NAME, hedgeConfig, executorService);
    }

    /**
     * Creates a CompletableFuture that is enhanced by a Hedge.
     *
     * @param callable        the callable to submit
     * @param executorService the executor service to which you are submitting the callable and hedges.
     * @param <T>             the type of results supplied by the supplier
     * @return a CompletableFuture which is resolved by the Hedge
     */
    <T> CompletableFuture<T> submit(Callable<T> callable, ExecutorService executorService);

    String getName();

    /**
     * Returns an unmodifiable map with tags assigned to this Hedge.
     *
     * @return the tags assigned to this Hedge in an unmodifiable map
     */
    Map<String, String> getTags();

    /**
     * Creates a CompletionStage supplier which is enhanced by a Hedge
     *
     * @param <T>      the type of the returned CompletionStage's result
     * @param <F>      the CompletionStage type supplied
     * @param supplier the original CompletionStage supplier
     * @return a CompletionStage supplier which is decorated by a Hedge
     */
    <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
            Supplier<F> supplier);

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    Hedge.EventPublisher getEventPublisher();

    /**
     * Returns HedgeDurationSupplier which are used to determine whether to hedge.
     *
     * @return current Hedge's Duration Supplier
     */
    HedgeDurationSupplier getDurationSupplier();

    /**
     * Returns the current duration that the next hedged call would be expected to wait before hedging.
     *
     * @return current Hedge's Duration
     */
    Duration getDuration();

    /**
     * Records a successful call.
     * <p>
     *
     * @param duration the duration the primary took to succeed
     *                 This method must be invoked when a call was successful.
     */
    void onPrimarySuccess(Duration duration);

    /**
     * Records a successful hedged call.
     * <p>
     *
     * @param duration the duration the hedge took to succeed
     *                 This method must be invoked when a hedged call was successful.
     */
    void onSecondarySuccess(Duration duration);

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param duration  the duration the primary took to fail
     * @param throwable The throwable which must be recorded
     */

    //make configurable to include errors in calculations (same with hedged)
    void onPrimaryFailure(Duration duration, Throwable throwable);

    /**
     * Records a failed hedged call. This method must be invoked when a hedged call failed.
     *
     * @param duration  the duration the hedge took to fail
     * @param throwable The throwable which must be recorded
     */
    void onSecondaryFailure(Duration duration, Throwable throwable);

    /**
     * Get the HedgeConfig of this Hedge decorator.
     *
     * @return the HedgeConfig of this Hedge decorator
     */
    SimpleHedgeConfig getHedgeConfig();
}
