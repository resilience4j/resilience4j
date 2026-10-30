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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

/**
 * A Bulkhead implementation based on a semaphore.
 */
public class SemaphoreBulkhead implements Bulkhead {

    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String TAGS_MUST_NOTE_BE_NULL = "Tags must not be null";

    private final String name;
    private final Semaphore semaphore;
    private final BulkheadMetrics metrics;
    private final BulkheadEventProcessor eventProcessor;
    private final ConcurrentLinkedQueue<PendingPermission> pendingPermissions =
        new ConcurrentLinkedQueue<>();
    private final AtomicInteger grantsInProgress = new AtomicInteger();
    private final AtomicInteger queuedCalls = new AtomicInteger();

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, String> tags;
    @SuppressWarnings("squid:S3077")
    // this object is immutable and we replace ref entirely during config change.
    private volatile BulkheadConfig config;

    /**
     * Creates a bulkhead using a configuration supplied
     *
     * @param name           the name of this bulkhead
     * @param bulkheadConfig custom bulkhead configuration
     */
    public SemaphoreBulkhead(String name, @Nullable BulkheadConfig bulkheadConfig) {
        this(name, bulkheadConfig, emptyMap());
    }

    /**
     * Creates a bulkhead using a configuration supplied
     *
     * @param name           the name of this bulkhead
     * @param bulkheadConfig custom bulkhead configuration
     * @param tags           the tags to add to the Bulkhead
     */
    public SemaphoreBulkhead(String name, @Nullable BulkheadConfig bulkheadConfig,
        Map<String, String> tags) {
        this.name = name;
        this.config = requireNonNull(bulkheadConfig, CONFIG_MUST_NOT_BE_NULL);
        this.tags = requireNonNull(tags, TAGS_MUST_NOTE_BE_NULL);
        // init semaphore
        this.semaphore = new Semaphore(config.getMaxConcurrentCalls(), config.isFairCallHandlingEnabled());

        this.metrics = new BulkheadMetrics();
        this.eventProcessor = new BulkheadEventProcessor();
    }

    /**
     * Creates a bulkhead with a default config.
     *
     * @param name the name of this bulkhead
     */
    public SemaphoreBulkhead(String name) {
        this(name, BulkheadConfig.ofDefaults(), emptyMap());
    }

    /**
     * Create a bulkhead using a configuration supplier
     *
     * @param name           the name of this bulkhead
     * @param configSupplier BulkheadConfig supplier
     */
    public SemaphoreBulkhead(String name, Supplier<BulkheadConfig> configSupplier) {
        this(name, configSupplier.get(), emptyMap());
    }

    /**
     * Create a bulkhead using a configuration supplier
     *
     * @param name           the name of this bulkhead
     * @param configSupplier BulkheadConfig supplier
     * @param tags           tags to add to the Bulkhead
     */
    public SemaphoreBulkhead(String name, Supplier<BulkheadConfig> configSupplier,
        Map<String, String> tags) {
        this(name, configSupplier.get(), tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeConfig(final BulkheadConfig newConfig) {
        lock.lock();

        try {
            int delta = newConfig.getMaxConcurrentCalls() - config.getMaxConcurrentCalls();
            if (delta < 0) {
                semaphore.acquireUninterruptibly(-delta);
            } else if (delta > 0) {
                semaphore.release(delta);
            }
            config = newConfig;
        } finally {
            lock.unlock();
        }
        grantPendingPermissions();
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
    public CompletableFuture<Void> acquirePermissionAsync() {
        if (pendingPermissions.isEmpty() && semaphore.tryAcquire()) {
            publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
            return CompletableFuture.completedFuture(null);
        }
        Duration maxWaitDuration = config.getMaxWaitDuration();
        if (maxWaitDuration.isZero()) {
            publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
            return CompletableFuture
                .failedFuture(BulkheadFullException.createBulkheadFullException(this));
        }
        if (!reserveQueueSlot()) {
            publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
            return CompletableFuture
                .failedFuture(BulkheadFullException.createBulkheadFullException(this));
        }
        PendingPermission permission = new PendingPermission();
        ScheduledFuture<?> timeoutTask;
        try {
            timeoutTask = SchedulerFactory.getInstance().getScheduler().schedule(() -> {
                if (permission.tryExpire()) {
                    permission.completeExceptionally(
                        BulkheadFullException.createBulkheadFullException(this));
                }
            }, toNanosSaturated(maxWaitDuration), TimeUnit.NANOSECONDS);
        } catch (RejectedExecutionException e) {
            // The scheduler was shut down concurrently. Fail before queueing the request, otherwise
            // a request nobody holds would be granted a permit later and leak it.
            queuedCalls.decrementAndGet();
            return CompletableFuture.failedFuture(e);
        }
        pendingPermissions.offer(permission);
        permission.whenComplete((result, throwable) -> {
            timeoutTask.cancel(false);
            if (throwable != null) {
                if (pendingPermissions.remove(permission)) {
                    queuedCalls.decrementAndGet();
                }
                // A cancelled request was withdrawn by the caller, the Bulkhead did not reject it
                if (!(throwable instanceof CancellationException)) {
                    publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
                }
            }
        });
        grantPendingPermissions();
        return permission;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releasePermission() {
        semaphore.release();
        grantPendingPermissions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onComplete() {
        semaphore.release();
        publishBulkheadEvent(() -> new BulkheadOnCallFinishedEvent(name));
        grantPendingPermissions();
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
    public Map<String, String> getTags() {
        return tags;
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
        long timeout = config.getMaxWaitDuration().toMillis();

        try {
            return semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Grants queued permission requests in FIFO order as long as permits are available.
     * Permission requests which already expired or were cancelled are skipped and their permit
     * is returned.
     * <p>
     * The {@link BulkheadOnCallPermittedEvent} is published after the request won the handoff
     * but before its future is completed, because completing the future runs its dependent
     * actions on the granting thread and such an action may already finish the call. An action
     * may also release its permission and trigger the next grant, so re-entrant and concurrent
     * calls are folded into the already running grant loop instead of recursing. A failing event
     * consumer never leaves the loop early, otherwise the drain could get stuck; its exception is
     * rethrown once the drain is complete.
     */
    private void grantPendingPermissions() {
        if (grantsInProgress.getAndIncrement() != 0) {
            return;
        }
        RuntimeException consumerFailure = null;
        int missed = 1;
        do {
            while (!pendingPermissions.isEmpty() && semaphore.tryAcquire()) {
                PendingPermission permission = pendingPermissions.poll();
                if (permission == null) {
                    semaphore.release();
                    continue;
                }
                queuedCalls.decrementAndGet();
                if (!permission.tryGrant()) {
                    semaphore.release();
                    continue;
                }
                try {
                    publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
                } catch (RuntimeException e) {
                    if (consumerFailure == null) {
                        consumerFailure = e;
                    } else {
                        consumerFailure.addSuppressed(e);
                    }
                } finally {
                    permission.complete(null);
                }
            }
            missed = grantsInProgress.addAndGet(-missed);
        } while (missed != 0);
        if (consumerFailure != null) {
            throw consumerFailure;
        }
    }

    /**
     * Reserves a slot in the queue of waiting permission requests, unless the queue already holds
     * {@link BulkheadConfig#getMaxQueuedCalls()} requests.
     */
    private boolean reserveQueueSlot() {
        int maxQueuedCalls = config.getMaxQueuedCalls();
        int queued = queuedCalls.get();
        while (queued < maxQueuedCalls) {
            if (queuedCalls.compareAndSet(queued, queued + 1)) {
                return true;
            }
            queued = queuedCalls.get();
        }
        return false;
    }

    private static long toNanosSaturated(Duration duration) {
        try {
            return duration.toNanos();
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * A queued permission request. Leaving the pending state is the handoff between granting,
     * expiring and cancelling: only the winner completes the future. This lets the grant publish
     * its event before the request's dependent actions run, and a lost race can never leak a
     * permit.
     */
    private static final class PendingPermission extends CompletableFuture<Void> {

        private static final int PENDING = 0;
        private static final int GRANTED = 1;
        private static final int EXPIRED = 2;
        private static final int CANCELLED = 3;

        private final AtomicInteger state = new AtomicInteger(PENDING);

        boolean tryGrant() {
            return state.compareAndSet(PENDING, GRANTED);
        }

        boolean tryExpire() {
            return state.compareAndSet(PENDING, EXPIRED);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (state.compareAndSet(PENDING, CANCELLED)) {
                return super.cancel(mayInterruptIfRunning);
            }
            return isCancelled();
        }
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
            registerConsumer(BulkheadOnCallPermittedEvent.class.getName(),
                onCallPermittedEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCallRejected(
            EventConsumer<BulkheadOnCallRejectedEvent> onCallRejectedEventConsumer) {
            registerConsumer(BulkheadOnCallRejectedEvent.class.getName(),
                onCallRejectedEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCallFinished(
            EventConsumer<BulkheadOnCallFinishedEvent> onCallFinishedEventConsumer) {
            registerConsumer(BulkheadOnCallFinishedEvent.class.getName(),
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

        @Override
        public int getQueuedCalls() {
            return queuedCalls.get();
        }
    }
}
