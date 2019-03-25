/*
 *
 *  Copyright 2019 Robert Winkler
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


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;

/**
 * A Bulkhead implementation based on a fixed ThreadPoolExecutor.
 */
public class FixedThreadPoolBulkhead implements ThreadPoolBulkhead {

    private final String name;
    private final ThreadPoolExecutor executorService;
    private volatile ThreadPoolBulkheadConfig config;
    private final FixedThreadPoolBulkhead.BulkheadMetrics metrics;
    private final FixedThreadPoolBulkhead.BulkheadEventProcessor eventProcessor;

    /**
     * Creates a bulkhead using a configuration supplied
     *
     * @param name           the name of this bulkhead
     * @param bulkheadConfig custom bulkhead configuration
     */
    public FixedThreadPoolBulkhead(String name, ThreadPoolBulkheadConfig bulkheadConfig) {
        this.name = name;
        this.config = bulkheadConfig != null ? bulkheadConfig
                : ThreadPoolBulkheadConfig.ofDefaults();
        // init thread pool executor
        this.executorService = new ThreadPoolExecutor(config.getCoreThreadPoolSize(), config.getMaxThreadPoolSize(),
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(config.getQueueCapacity()));

        this.metrics = new FixedThreadPoolBulkhead.BulkheadMetrics();
        this.eventProcessor = new FixedThreadPoolBulkhead.BulkheadEventProcessor();
    }

    /**
     * Creates a bulkhead with a default config.
     *
     * @param name the name of this bulkhead
     */
    public FixedThreadPoolBulkhead(String name) {
        this(name, ThreadPoolBulkheadConfig.ofDefaults());
    }

    /**
     * Create a bulkhead using a configuration supplier
     *
     * @param name           the name of this bulkhead
     * @param configSupplier BulkheadConfig supplier
     */
    public FixedThreadPoolBulkhead(String name, Supplier<ThreadPoolBulkheadConfig> configSupplier) {
        this(name, configSupplier.get());
    }

    @Override
    public boolean isCallPermitted() {
        boolean callPermitted = executorService.getQueue().remainingCapacity() > 0;

        publishBulkheadEvent(
                () -> callPermitted ? new BulkheadOnCallPermittedEvent(name)
                        : new BulkheadOnCallRejectedEvent(name)
        );

        return callPermitted;
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        return executorService.submit(callable);
    }

    @Override
    public void submit(Runnable runnable) {
        executorService.submit(runnable);
    }

    @Override
    public void onComplete() {
        publishBulkheadEvent(() -> new BulkheadOnCallFinishedEvent(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    private void publishBulkheadEvent(Supplier<BulkheadEvent> eventSupplier) {
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(eventSupplier.get());
        }
    }

    @Override
    public String toString() {
        return String.format("FixedThreadPoolBulkhead '%s'", this.name);
    }

    private class BulkheadEventProcessor extends EventProcessor<BulkheadEvent> implements EventPublisher, EventConsumer<BulkheadEvent> {

        @Override
        public EventPublisher onCallPermitted(EventConsumer<BulkheadOnCallPermittedEvent> onCallPermittedEventConsumer) {
            registerConsumer(BulkheadOnCallPermittedEvent.class, onCallPermittedEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCallRejected(EventConsumer<BulkheadOnCallRejectedEvent> onCallRejectedEventConsumer) {
            registerConsumer(BulkheadOnCallRejectedEvent.class, onCallRejectedEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCallFinished(EventConsumer<BulkheadOnCallFinishedEvent> onCallFinishedEventConsumer) {
            registerConsumer(BulkheadOnCallFinishedEvent.class, onCallFinishedEventConsumer);
            return this;
        }

        @Override
        public void consumeEvent(BulkheadEvent event) {
            super.processEvent(event);
        }
    }

    private final class BulkheadMetrics implements Metrics {
        private BulkheadMetrics() {
        }

        @Override
        public int getAvailableConcurrentCalls() {
            return executorService.getPoolSize() - executorService.getActiveCount();
        }

        @Override
        public int getMaxAllowedConcurrentCalls() {
            return executorService.getPoolSize();
        }
    }

}
