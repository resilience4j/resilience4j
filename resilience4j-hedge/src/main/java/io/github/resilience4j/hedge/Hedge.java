/*
 *
 *  Copyright 2021: Matthew Sandoz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.hedge;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.hedge.event.*;
import io.github.resilience4j.hedge.internal.HedgeImpl;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * A Hedge executes extra requests if the main execution does not return in time. If used without a scheduled thread
 * pool executor, it will consume a thread which will sit idle for the associated timeout. With a scheduled thread pool
 * executor, it will queue hedging executions in the executor's priority queue and process them when there are threads
 * available to execute them.<p>
 *
 * If not using a scheduled thread pool executor, it is recommended to double your normal executor pool to allow for the
 * hedge timing requests. Set the size limit on your scheduled thread pool executor to limit the number of hedges
 * actively running at the same time. Adjust your rejected execution handler to the desired behavior, such as
 * ThreadPoolExecutor.AbortPolicy.<p>
 *
 * Hedge metrics do not include metrics on calls rejected by the executor. These situations are expected to result in
 * completion of the primary call.<p>
 *
 * Good candidates for Hedged calls include side-effect-free calls, calls which may have a long response time tail. Do
 * not hedge non-idempotent inserts or other similar calls.
 */
public interface Hedge {

    String DEFAULT_NAME = "UNDEFINED";

    /**
     * Creates a Hedge decorator with a default HedgeConfig configuration.
     *
     * @return The Hedge
     */
    static Hedge ofDefaults() {
        return new HedgeImpl(DEFAULT_NAME, HedgeConfig.ofDefaults());
    }

    /**
     * Creates a Hedge decorator with a default HedgeConfig configuration.
     *
     * @param name the name of the Hedge
     * @return The Hedge
     */
    static Hedge ofDefaults(String name) {
        return new HedgeImpl(name, HedgeConfig.ofDefaults());
    }

    /**
     * Creates a Hedge decorator with a HedgeConfig configuration.
     *
     * @param HedgeConfig the HedgeConfig
     * @return The Hedge
     */
    static Hedge of(HedgeConfig HedgeConfig) {
        return of(DEFAULT_NAME, HedgeConfig);
    }

    /**
     * Creates a Hedge decorator with a HedgeConfig configuration.
     *
     * @param name        the name of the Hedge
     * @param HedgeConfig the HedgeConfig
     * @return The Hedge
     */
    static Hedge of(String name, HedgeConfig HedgeConfig) {
        return new HedgeImpl(name, HedgeConfig);
    }

    /**
     * Creates a Hedge with a custom Hedge configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name        the name of the Hedge
     * @param HedgeConfig a custom Hedge configuration
     * @param tags        tags added to the Hedge
     * @return a Hedge with a custom Hedge configuration.
     */
    static Hedge of(String name, HedgeConfig HedgeConfig,
                    io.vavr.collection.Map<String, String> tags) {
        return new HedgeImpl(name, HedgeConfig, tags);
    }

    /**
     * Creates a Hedge decorator with a timeout Duration.
     *
     * @param hedgeDuration the timeout Duration
     * @return The Hedge
     */
    static Hedge of(Duration hedgeDuration) {
        HedgeConfig hedgeConfig = new HedgeConfig.Builder()
            .preconfiguredMetrics(hedgeDuration)
            .build();
        return new HedgeImpl(DEFAULT_NAME, hedgeConfig);
    }

    /**
     * Creates a Future that is resolved by a Hedge.
     *
     * @param callable        the callable to submit
     * @param executorService the executor service to which you are submitting the callable and hedges.
     * @param <T>             the type of results supplied by the supplier
     * @return a future supplier which is resolved by a Hedge
     */
    <T> Future<T> submit(Callable<T> callable, ExecutorService executorService);

    /**
     * Creates a Callback that is resolved by a Hedge.
     *
     * @param callable    the Hedge
     * @param primaryExecutor the original future supplier
     * @param hedgedExecutor the original future supplier
     * @param <T>            the type of results supplied by the supplier
     * @return a future supplier which is resolved by a Hedge
     */
    <T> Future<T> submit(Callable<T> callable, ExecutorService primaryExecutor, ScheduledExecutorService hedgedExecutor);

    String getName();

    /**
     * Returns an unmodifiable map with tags assigned to this Hedge.
     *
     * @return the tags assigned to this Hedge in an unmodifiable map
     */
    io.vavr.collection.Map<String, String> getTags();

    /**
     * Get the HedgeConfig of this Hedge decorator.
     *
     * @return the HedgeConfig of this Hedge decorator
     */
    HedgeConfig getHedgeConfig();

    /**
     * Creates a CompletionStage supplier which is guarded by a Hedge
     *
     * @param <T>            the type of the returned CompletionStage's result
     * @param <F>            the CompletionStage type supplied
     * @param supplier       the original CompletionStage supplier
     * @param hedgedExecutor the ScheduledExecutorService used to attempt the hedged requests
     * @return a CompletionStage supplier which is decorated by a Hedge
     */
    <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
        Supplier<F> supplier, ScheduledExecutorService hedgedExecutor);

    /**
     * Creates a CompletionStage supplier which is guarded by a Hedge
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
    EventPublisher getEventPublisher();

    /**
     * Returns HedgeMetrics which are used to determine whether to hedge
     *
     * @return current Hedge's metrics
     */
    HedgeMetrics getMetrics();

    /**
     * Records a successful call.
     * <p>
     * @param duration the duration the primary took to succeed
     * This method must be invoked when a call was successful.
     */
    void onPrimarySuccess(Duration duration);

    /**
     * Records a successful hedged call.
     * <p>
     * @param duration the duration the hedge took to succeed
     * This method must be invoked when a hedged call was successful.
     */
    void onHedgedSuccess(Duration duration);

    /**
     * Records a failed call. This method must be invoked when a call failed.
     * @param duration  the duration the primary took to fail
     * @param throwable The throwable which must be recorded
     */

    //make configurable to include errors in metrics (same with hedged)
    void onPrimaryFailure(Duration duration, Throwable throwable);

    /**
     * Records a failed hedged call. This method must be invoked when a hedged call failed.
     *
     * @param duration the duration the hedge took to fail
     * @param throwable The throwable which must be recorded
     */
    void onHedgedFailure(Duration duration, Throwable throwable);

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<HedgeEvent> {

        EventPublisher onPrimarySuccess(EventConsumer<PrimarySuccessEvent> eventConsumer);

        EventPublisher onPrimaryFailure(EventConsumer<PrimaryFailureEvent> eventConsumer);

        EventPublisher onHedgeSuccess(EventConsumer<HedgeSuccessEvent> eventConsumer);

        EventPublisher onHedgeFailure(EventConsumer<HedgeFailureEvent> eventConsumer);
    }
}
