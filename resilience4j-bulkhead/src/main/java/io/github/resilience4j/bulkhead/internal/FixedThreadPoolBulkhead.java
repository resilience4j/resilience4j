/*
 *
 *  Copyright 2019 Robert Winkler, Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.internal;


import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.core.ContextPropagator;
import io.github.resilience4j.core.lang.NonNull;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * A Bulkhead implementation based on a fixed ThreadPoolExecutor. which is based into the thread
 * pool execution handling : 1- submit service call through bulk head thread pool 2- if there is
 * free thread from the thread pool or the queue is not yet full , it will be permitted 3- otherwise
 * the thread pool will throw RejectedExecutionException which mean is not permitted
 */
public class FixedThreadPoolBulkhead extends AbstractExecutorServiceBulkhead<ThreadPoolExecutor> implements ThreadPoolBulkhead {

    private final FixedThreadPoolBulkhead.BulkheadMetrics metrics;
    private final ThreadPoolBulkheadConfig config;

    /**
     * Creates a bulkhead using a configuration supplied
     *
     * @param name           the name of this bulkhead
     * @param bulkheadConfig custom bulkhead configuration
     */
    public FixedThreadPoolBulkhead(String name, ThreadPoolBulkheadConfig bulkheadConfig) {
        this(name, bulkheadConfig, emptyMap());
    }

    /**
     * Creates a bulkhead using a configuration supplied
     *
     * @param name           the name of this bulkhead
     * @param bulkheadConfig custom bulkhead configuration
     * @param tags           tags to add to the Bulkhead
     */
    public FixedThreadPoolBulkhead(String name, @NonNull ThreadPoolBulkheadConfig bulkheadConfig,
        Map<String, String> tags) {
        super(new ThreadPoolExecutor(bulkheadConfig.getCoreThreadPoolSize(),
                bulkheadConfig.getMaxThreadPoolSize(),
                bulkheadConfig.getKeepAliveDuration().toMillis(), TimeUnit.MILLISECONDS,
                bulkheadConfig.getQueueCapacity() == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(bulkheadConfig.getQueueCapacity()),
                new BulkheadNamingThreadFactory(name),
                bulkheadConfig.getRejectedExecutionHandler()),
            name,
            tags);
        this.config = bulkheadConfig;
        // init thread pool executor
        // adding prover jvm executor shutdown
        this.metrics = new FixedThreadPoolBulkhead.BulkheadMetrics();
    }

    /**
     * Creates a bulkhead with a default config.
     *
     * @param name the name of this bulkhead
     */
    public FixedThreadPoolBulkhead(String name) {
        this(name, ThreadPoolBulkheadConfig.ofDefaults(), emptyMap());
    }

    /**
     * Creates a bulkhead with a default config.
     *
     * @param name the name of this bulkhead
     */
    public FixedThreadPoolBulkhead(String name, Map<String, String> tags) {
        this(name, ThreadPoolBulkheadConfig.ofDefaults(), tags);
    }

    /**
     * Create a bulkhead using a configuration supplier
     *
     * @param name           the name of this bulkhead
     * @param configSupplier BulkheadConfig supplier
     */
    public FixedThreadPoolBulkhead(String name, Supplier<ThreadPoolBulkheadConfig> configSupplier) {
        this(name, configSupplier.get(), emptyMap());
    }

    /**
     * Create a bulkhead using a configuration supplier
     *
     * @param name           the name of this bulkhead
     * @param configSupplier BulkheadConfig supplier
     */
    public FixedThreadPoolBulkhead(String name, Supplier<ThreadPoolBulkheadConfig> configSupplier,
        Map<String, String> tags) {
        this(name, configSupplier.get(), tags);
    }

    @Override
    protected <T> @Nonnull Supplier<T> decorate(Supplier<T> supplier) {
        return ContextPropagator.decorateSupplier(config.getContextPropagator(), supplier);
    }

    @Override
    protected boolean isWritableStackTraceEnabled() {
        return config.isWritableStackTraceEnabled();
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        return ContextPropagator.decorateRunnable(config.getContextPropagator(), runnable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadPoolBulkheadConfig getBulkheadConfig() {
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return String.format("FixedThreadPoolBulkhead '%s'", this.name);
    }

    /**
     * the thread pool bulk head metrics
     */
    private final class BulkheadMetrics implements Metrics {

        private BulkheadMetrics() {
        }

        @Override
        public int getCoreThreadPoolSize() {
            return executorService.getCorePoolSize();
        }

        @Override
        public int getThreadPoolSize() {
            return executorService.getPoolSize();
        }

        @Override
        public int getMaximumThreadPoolSize() {
            return executorService.getMaximumPoolSize();
        }

        @Override
        public int getQueueDepth() {
            return executorService.getQueue().size();
        }

        @Override
        public int getRemainingQueueCapacity() {
            return executorService.getQueue().remainingCapacity();
        }

        @Override
        public int getQueueCapacity() {
            return config.getQueueCapacity();
        }

        @Override
        public int getActiveThreadCount() {
            return executorService.getActiveCount();
        }

        @Override
        public int getAvailableThreadCount() {
            return getMaximumThreadPoolSize() - getActiveThreadCount();
        }
    }
}
