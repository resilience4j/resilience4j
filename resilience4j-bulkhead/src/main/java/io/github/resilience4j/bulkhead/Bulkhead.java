/*
 *
 *  Copyright 2017: Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.bulkhead.utils.BulkheadUtils;
import io.github.resilience4j.core.EventConsumer;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  A Bulkhead instance is thread-safe can be used to decorate multiple requests.
 *
 * A {@link Bulkhead} represent an entity limiting the amount of parallel operations. It does not assume nor does it mandate usage
 * of any particular concurrency and/or io model. These details are left for the client to manage. This bulkhead, depending on the
 * underlying concurrency/io model can be used to shed load, and, where it makes sense, limit resource use (i.e. limit amount of
 * threads/actors involved in a particular flow, etc).
 *
 * In order to execute an operation protected by this bulkhead, a permission must be obtained by calling {@link Bulkhead#isCallPermitted()}
 * If the bulkhead is full, no additional operations will be permitted to execute until space is available.
 *
 * Once the operation is complete, regardless of the result, client needs to call {@link Bulkhead#onComplete()} in order to maintain
 * integrity of internal bulkhead state.
 *
 */
public interface Bulkhead {

    /**
     * Dynamic bulkhead configuration change.
     * NOTE! New `maxWaitTime` duration won't affect threads that are currently waiting for permission.
     * @param newConfig new BulkheadConfig
     */
    void changeConfig(BulkheadConfig newConfig);

    /**
     * Attempts to acquire a permit, which allows an call to be executed.
     *
     * @return boolean whether a call should be executed
     */
    boolean isCallPermitted();

    /**
     * Records a completed call.
     */
    void onComplete();

    /**
     * Returns the name of this bulkhead.
     *
     * @return the name of this bulkhead
     */
    String getName();

    /**
     * Returns the BulkheadConfig of this Bulkhead.
     *
     * @return bulkhead config
     */
    BulkheadConfig getBulkheadConfig();

    /**
     * Get the Metrics of this Bulkhead.
     *
     * @return the Metrics of this Bulkhead
     */
    Metrics getMetrics();

    /**
     * Returns an EventPublisher which subscribes to the reactive stream of BulkheadEvent and
     * can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

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
     * Decorates and executes the decorated CompletionStage.
     *
     * @param supplier the original CompletionStage
     * @param <T> the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier){
        return decorateCompletionStage(this, supplier).get();
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the Bulkhead
     * @param supplier the original supplier
     * @param <T> the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Bulkhead bulkhead, CheckedFunction0<T> supplier){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return supplier.apply();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T> the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(Bulkhead bulkhead, Supplier<CompletionStage<T>> supplier) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();

            if (!bulkhead.isCallPermitted()) {
                promise.completeExceptionally(new BulkheadFullException(String.format("Bulkhead '%s' is open", bulkhead.getName())));
            }
            else {
                try {
                    supplier.get()
                            .whenComplete(
                                (result, throwable) -> {
                                    bulkhead.onComplete();
                                    if (throwable != null) {
                                        promise.completeExceptionally(throwable);
                                    }
                                    else {
                                        promise.complete(result);
                                    }
                                }
                            );
                }
                catch (Throwable throwable) {
                    bulkhead.onComplete();
                    promise.completeExceptionally(throwable);
                }
            }

            return promise;
        };
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     *
     * @return a runnable which is decorated by a Bulkhead.
     */
    static CheckedRunnable decorateCheckedRunnable(Bulkhead bulkhead, CheckedRunnable runnable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                runnable.run();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a callable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param callable the original Callable
     * @param <T> the result type of callable
     *
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Callable<T> decorateCallable(Bulkhead bulkhead, Callable<T> callable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return callable.call();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T> the type of results supplied by this supplier
     *
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<T> decorateSupplier(Bulkhead bulkhead, Supplier<T> supplier){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return supplier.get();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a bulkhead.

     * @param bulkhead the bulkhead
     * @param consumer the original consumer
     * @param <T> the type of the input to the consumer
     *
     * @return a consumer which is decorated by a Bulkhead.
     */
    static <T> Consumer<T> decorateConsumer(Bulkhead bulkhead, Consumer<T> consumer){
        return (t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                consumer.accept(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a bulkhead.

     * @param bulkhead the bulkhead
     * @param consumer the original consumer
     * @param <T> the type of the input to the consumer
     *
     * @return a consumer which is decorated by a Bulkhead.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(Bulkhead bulkhead, CheckedConsumer<T> consumer){
        return (t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                consumer.accept(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     *
     * @return a runnable which is decorated by a bulkhead.
     */
    static Runnable decorateRunnable(Bulkhead bulkhead, Runnable runnable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                runnable.run();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a function which is decorated by a bulkhead.

     * @param bulkhead the bulkhead
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return a function which is decorated by a bulkhead.
     */
    static <T, R> Function<T, R> decorateFunction(Bulkhead bulkhead, Function<T, R> function){
        return (T t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                return function.apply(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a function which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return a function which is decorated by a bulkhead.
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Bulkhead bulkhead, CheckedFunction1<T, R> function){
        return (T t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                return function.apply(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Create a Bulkhead with a default configuration.
     *
     * @param name the name of the bulkhead
     * @return a Bulkhead instance
     */
    static Bulkhead ofDefaults(String name) {
        return new SemaphoreBulkhead(name);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name the name of the bulkhead
     * @param config a custom BulkheadConfig configuration
     * @return a Bulkhead instance
     */
    static Bulkhead of(String name, BulkheadConfig config) {
        return new SemaphoreBulkhead(name, config);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name the name of the bulkhead
     * @param bulkheadConfigSupplier custom configuration supplier
     * @return a Bulkhead instance
     */
    static Bulkhead of(String name, Supplier<BulkheadConfig> bulkheadConfigSupplier) {
        return new SemaphoreBulkhead(name, bulkheadConfigSupplier);
    }

    interface Metrics {


        /**
         * Returns the number of parallel executions this bulkhead can support at this point in time.
         *
         * @return remaining bulkhead depth
         */
        int getAvailableConcurrentCalls();

        /**
         * Returns the configured max amount of concurrent calls
         * allowed for this bulkhead, basically it's a top inclusive bound for
         * the value returned from {@link #getAvailableConcurrentCalls()}.
         *
         * @return max allowed concurrent calls
         */
        int getMaxAllowedConcurrentCalls();
    }

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<BulkheadEvent> {

        EventPublisher onCallRejected(EventConsumer<BulkheadOnCallRejectedEvent> eventConsumer);

        EventPublisher onCallPermitted(EventConsumer<BulkheadOnCallPermittedEvent> eventConsumer);

        EventPublisher onCallFinished(EventConsumer<BulkheadOnCallFinishedEvent> eventConsumer);
    }
}
