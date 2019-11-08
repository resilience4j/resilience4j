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
import io.github.resilience4j.bulkhead.internal.FixedThreadPoolBulkhead;
import io.github.resilience4j.core.EventConsumer;
import io.vavr.collection.Map;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * A Bulkhead instance is thread-safe can be used to decorate multiple requests.
 */
public interface ThreadPoolBulkhead {

    /**
     * Returns a callable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param callable the original Callable
     * @param <T>      the result type of callable
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Callable<CompletionStage<T>> decorateCallable(ThreadPoolBulkhead bulkhead,
        Callable<T> callable) {
        return () -> bulkhead.submit(callable);
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<CompletionStage<T>> decorateSupplier(ThreadPoolBulkhead bulkhead,
        Supplier<T> supplier) {
        return () -> bulkhead.submit(supplier::get);
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     * @return a runnable which is decorated by a bulkhead.
     */
    static Runnable decorateRunnable(ThreadPoolBulkhead bulkhead, Runnable runnable) {
        return () -> bulkhead.submit(runnable);
    }

    /**
     * Create a Bulkhead with a default configuration.
     *
     * @param name the name of the bulkhead
     * @return a Bulkhead instance
     */
    static ThreadPoolBulkhead ofDefaults(String name) {
        return new FixedThreadPoolBulkhead(name);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name   the name of the bulkhead
     * @param config a custom BulkheadConfig configuration
     * @return a Bulkhead instance
     */
    static ThreadPoolBulkhead of(String name, ThreadPoolBulkheadConfig config) {
        return new FixedThreadPoolBulkhead(name, config);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name   the name of the bulkhead
     * @param config a custom BulkheadConfig configuration
     * @return a Bulkhead instance
     */
    static ThreadPoolBulkhead of(String name, ThreadPoolBulkheadConfig config,
        io.vavr.collection.Map<String, String> tags) {
        return new FixedThreadPoolBulkhead(name, config, tags);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name                   the name of the bulkhead
     * @param bulkheadConfigSupplier custom configuration supplier
     * @return a Bulkhead instance
     */
    static ThreadPoolBulkhead of(String name,
        Supplier<ThreadPoolBulkheadConfig> bulkheadConfigSupplier) {
        return new FixedThreadPoolBulkhead(name, bulkheadConfigSupplier);
    }

    /**
     * Submits a value-returning task for execution and returns a Future representing the pending
     * results of the task.
     *
     * @param task the task to submit
     * @param <T>  the type of the task's result
     * @return a CompletableFuture representing listenable future completion of the task
     * @throws BulkheadFullException if the no permits
     */
    <T> CompletionStage<T> submit(Callable<T> task);

    /**
     * Submits a task for execution.
     *
     * @param task the task to submit
     * @throws BulkheadFullException if the no permits
     */
    void submit(Runnable task);

    /**
     * Returns the name of this bulkhead.
     *
     * @return the name of this bulkhead
     */
    String getName();

    /**
     * Returns the ThreadPoolBulkheadConfig of this Bulkhead.
     *
     * @return bulkhead config
     */
    ThreadPoolBulkheadConfig getBulkheadConfig();

    /**
     * Get the Metrics of this Bulkhead.
     *
     * @return the Metrics of this Bulkhead
     */
    Metrics getMetrics();

    /**
     * Returns an unmodifiable map with tags assigned to this Retry.
     *
     * @return the tags assigned to this Retry in an unmodifiable map
     */
    Map<String, String> getTags();

    /**
     * Returns an EventPublisher which subscribes to the reactive stream of BulkheadEvent and can be
     * used to register event consumers.
     *
     * @return an EventPublisher
     */
    ThreadPoolBulkheadEventPublisher getEventPublisher();

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws BulkheadFullException if the no permits
     */
    default <T> CompletionStage<T> executeSupplier(Supplier<T> supplier) {
        return decorateSupplier(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     * @param <T>      the result type of callable
     * @return the result of the decorated Callable.
     * @throws Exception if unable to compute a result
     */
    default <T> CompletionStage<T> executeCallable(Callable<T> callable) throws Exception {
        return decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable) {
        decorateRunnable(this, runnable).run();
    }

    interface Metrics {

        /**
         * Returns the core number of threads.
         *
         * @return the core number of threads
         */
        int getCoreThreadPoolSize();

        /**
         * Returns the current number of threads in the pool.
         *
         * @return the current number of threads
         */
        int getThreadPoolSize();

        /**
         * Returns the maximum allowed number of threads.
         *
         * @return the maximum allowed number of threads
         */
        int getMaximumThreadPoolSize();

        /**
         * Returns the number of tasks in the queue.
         *
         * @return the number of tasks in the queue
         */
        int getQueueDepth();

        /**
         * Returns the remaining queue capacity.
         *
         * @return the remaining queue capacity
         */
        int getRemainingQueueCapacity();

        /**
         * Returns the queue capacity.
         *
         * @return the queue capacity
         */
        int getQueueCapacity();
    }

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface ThreadPoolBulkheadEventPublisher extends
        io.github.resilience4j.core.EventPublisher<BulkheadEvent> {

        ThreadPoolBulkheadEventPublisher onCallRejected(
            EventConsumer<BulkheadOnCallRejectedEvent> eventConsumer);

        ThreadPoolBulkheadEventPublisher onCallPermitted(
            EventConsumer<BulkheadOnCallPermittedEvent> eventConsumer);

        ThreadPoolBulkheadEventPublisher onCallFinished(
            EventConsumer<BulkheadOnCallFinishedEvent> eventConsumer);
    }
}
