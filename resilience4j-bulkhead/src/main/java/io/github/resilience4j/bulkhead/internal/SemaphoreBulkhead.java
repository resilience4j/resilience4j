/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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


import static java.util.Objects.requireNonNull;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.exception.AcquirePermissionCancelledException;
import io.github.resilience4j.core.lang.Nullable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A Bulkhead implementation based on a semaphore.
 */
public class SemaphoreBulkhead implements Bulkhead {

    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";

    private final String name;
    private final Semaphore semaphore;
    private final BulkheadMetrics metrics;
    private final BulkheadEventProcessor eventProcessor;

    private final Object configChangesLock = new Object();
    @SuppressWarnings("squid:S3077")
    // this object is immutable and we replace ref entirely during config change.
    private volatile BulkheadConfig config;

    /**
     * Creates a bulkhead using a configuration supplied
     *
     * @param name the name of this bulkhead
     * @param bulkheadConfig custom bulkhead configuration
     */
    public SemaphoreBulkhead(String name, @Nullable BulkheadConfig bulkheadConfig) {
        this.name = name;
        this.config = requireNonNull(bulkheadConfig, CONFIG_MUST_NOT_BE_NULL);
        // init semaphore
        this.semaphore = new Semaphore(this.config.getMaxConcurrentCalls(), true);

        this.metrics = new BulkheadMetrics();
        this.eventProcessor = new BulkheadEventProcessor();
    }

    /**
     * Creates a bulkhead with a default config.
     *
     * @param name the name of this bulkhead
     */
    public SemaphoreBulkhead(String name) {
        this(name, BulkheadConfig.ofDefaults());
    }

    /**
     * Create a bulkhead using a configuration supplier
     *
     * @param name the name of this bulkhead
     * @param configSupplier BulkheadConfig supplier
     */
    public SemaphoreBulkhead(String name, Supplier<BulkheadConfig> configSupplier) {
        this(name, configSupplier.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeConfig(final BulkheadConfig newConfig) {
        synchronized (configChangesLock) {
            int delta = newConfig.getMaxConcurrentCalls() - config.getMaxConcurrentCalls();
            if (delta < 0) {
                semaphore.acquireUninterruptibly(-delta);
            } else if (delta > 0) {
                semaphore.release(delta);
            }
            config = newConfig;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryAcquirePermission() {
        boolean callPermitted = tryEnterBulkhead();

        publishBulkheadEvent(
                () -> callPermitted ? new BulkheadOnCallPermittedEvent(name)
                        : new BulkheadOnCallRejectedEvent(name)
        );

        return callPermitted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acquirePermission() {
        boolean permitted = tryAcquirePermission();
        if (permitted) {
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new AcquirePermissionCancelledException();
        }
        throw BulkheadFullException.createBulkheadFullException(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releasePermission() {
        semaphore.release();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onComplete() {
        semaphore.release();
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
    public BulkheadConfig getBulkheadConfig() {
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

    @Override
    public String toString() {
        return String.format("Bulkhead '%s'", this.name);
    }

    /**
     * @return true if caller was able to wait for permission without {@link Thread#interrupt}
     */
    boolean tryEnterBulkhead() {

        boolean callPermitted;
        long timeout = config.getMaxWaitDuration().toMillis();

        if (timeout == 0) {
            callPermitted = semaphore.tryAcquire();
        } else {
            try {
                callPermitted = semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                callPermitted = false;
            }
        }
        return callPermitted;
    }

    private void publishBulkheadEvent(Supplier<BulkheadEvent> eventSupplier) {
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(eventSupplier.get());
        }
    }

    private class BulkheadEventProcessor extends EventProcessor<BulkheadEvent> implements
            EventPublisher, EventConsumer<BulkheadEvent> {

        @Override
        public EventPublisher onCallPermitted(
                EventConsumer<BulkheadOnCallPermittedEvent> onCallPermittedEventConsumer) {
            registerConsumer(BulkheadOnCallPermittedEvent.class.getSimpleName(),
                    onCallPermittedEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCallRejected(
                EventConsumer<BulkheadOnCallRejectedEvent> onCallRejectedEventConsumer) {
            registerConsumer(BulkheadOnCallRejectedEvent.class.getSimpleName(),
                    onCallRejectedEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCallFinished(
                EventConsumer<BulkheadOnCallFinishedEvent> onCallFinishedEventConsumer) {
            registerConsumer(BulkheadOnCallFinishedEvent.class.getSimpleName(),
                    onCallFinishedEventConsumer);
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
            return semaphore.availablePermits();
        }

        @Override
        public int getMaxAllowedConcurrentCalls() {
            return config.getMaxConcurrentCalls();
        }
    }
}
