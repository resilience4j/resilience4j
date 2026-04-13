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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * A Bulkhead instance is thread-safe can be used to decorate multiple requests.
 */
public interface ThreadPoolBulkhead extends GenericBulkhead {

    /**
     * Returns a supplier which submits a value-returning task for execution and
     * returns a {@link CompletionStage} representing the pending results of the task.
     *
     * @param bulkhead the bulkhead
     * @param callable the value-returning task to submit
     * @param <T>      the result type of the callable
     * @return a supplier which submits a value-returning task for execution and returns a CompletionStage representing the pending
     * results of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     * @deprecated use {@link GenericBulkhead#decorateCallable(Callable)} instead.
     */
    @Deprecated
    static <T> Supplier<CompletionStage<T>> decorateCallable(ThreadPoolBulkhead bulkhead,
                                                             Callable<T> callable) {
        return GenericBulkhead.decorateCallable(bulkhead, callable);
    }

    /**
     * Returns a supplier which submits a value-returning task for execution
     * and returns a {@link CompletionStage} representing the pending results of the task.
     *
     * @param bulkhead the bulkhead
     * @param supplier the value-returning task to submit
     * @param <T>      the result type of the supplier
     * @return a supplier which submits a value-returning task for execution and returns a CompletionStage representing the pending
     * results of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     * @deprecated use {@link GenericBulkhead#decorateSupplier(Supplier)} instead.
     */
    @Deprecated
    static <T> Supplier<CompletionStage<T>> decorateSupplier(ThreadPoolBulkhead bulkhead,
        Supplier<T> supplier) {
        return GenericBulkhead.decorateSupplier(bulkhead, supplier);
    }

    /**
     * Returns a supplier which submits a task for execution and returns a {@link CompletionStage} representing the state of the task.
     *
     * @param bulkhead the bulkhead
     * @param runnable the to submit
     * @return a supplier which submits a task for execution to the ThreadPoolBulkhead
     * and returns a CompletionStage representing the state of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     * @deprecated use {@link GenericBulkhead#decorateRunnable(Runnable)} instead.
     */
    @Deprecated
    static Supplier<CompletionStage<Void>> decorateRunnable(ThreadPoolBulkhead bulkhead, Runnable runnable) {
        return GenericBulkhead.decorateRunnable(bulkhead, runnable);
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
    static ThreadPoolBulkhead of(String name, ThreadPoolBulkheadConfig config, Map<String, String> tags) {
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

        /**
         * Returns the number of actively executing tasks.
         *
         * @return the number of executing tasks
         */
        int getActiveThreadCount();

        /**
         * Returns the maximum number of available threads.
         * Equivalent to <code>getMaximumThreadPoolSize() - getActiveThreadCount()</code>
         *
         * @return the maximum number of available threads
         */
        int getAvailableThreadCount();
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
