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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.bulkhead.internal.FixedThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.utils.BulkheadUtils;
import io.github.resilience4j.core.EventConsumer;

/**
 *  A Bulkhead instance is thread-safe can be used to decorate multiple requests.
 *
 */
public interface ThreadPoolBulkhead {

    /**
     * Attempts to acquire a permit, which allows an call to be executed.
     *
     * @return boolean whether a call should be executed
     */
    boolean isCallPermitted();

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return a Future representing pending completion of the task
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Submits a task for execution.
     *
     * @param task the task to submit
     */
    void submit(Runnable task);

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
    default <T> Future<T> executeSupplier(Supplier<T> supplier){
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
    default <T> Future<T> executeCallable(Callable<T> callable) throws Exception{
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
     * Returns a callable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param callable the original Callable
     * @param <T> the result type of callable
     *
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Callable<Future<T>> decorateCallable(ThreadPoolBulkhead bulkhead, Callable<T> callable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return bulkhead.submit(callable);
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
    static <T> Supplier<Future<T>> decorateSupplier(ThreadPoolBulkhead bulkhead, Supplier<T> supplier){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return bulkhead.submit(supplier::get);
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
    static Runnable decorateRunnable(ThreadPoolBulkhead bulkhead, Runnable runnable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                bulkhead.submit(runnable);
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
    static ThreadPoolBulkhead ofDefaults(String name) {
        return new FixedThreadPoolBulkhead(name);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name the name of the bulkhead
     * @param config a custom BulkheadConfig configuration
     * @return a Bulkhead instance
     */
    static ThreadPoolBulkhead of(String name, ThreadPoolBulkheadConfig config) {
        return new FixedThreadPoolBulkhead(name, config);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name the name of the bulkhead
     * @param bulkheadConfigSupplier custom configuration supplier
     * @return a Bulkhead instance
     */
    static ThreadPoolBulkhead of(String name, Supplier<ThreadPoolBulkheadConfig> bulkheadConfigSupplier) {
        return new FixedThreadPoolBulkhead(name, bulkheadConfigSupplier);
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
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<BulkheadEvent> {

        EventPublisher onCallRejected(EventConsumer<BulkheadOnCallRejectedEvent> eventConsumer);

        EventPublisher onCallPermitted(EventConsumer<BulkheadOnCallPermittedEvent> eventConsumer);

        EventPublisher onCallFinished(EventConsumer<BulkheadOnCallFinishedEvent> eventConsumer);
    }
}
