/*
 *
 *  Copyright 2026 Oleksandr Shevchenko
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

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadType;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides a lazily created, shared {@link ScheduledExecutorService} which is used by
 * {@link SemaphoreBulkhead} to expire queued permission requests once their max wait duration
 * has elapsed. It is modeled after the CircuitBreaker SchedulerFactory: the scheduler honors
 * the configured Resilience4j thread type and is recreated when the configuration changes.
 *
 * <p>A {@link #reset()} method is provided mainly for tests to force recreation.
 */
final class SchedulerFactory {

    private static final class Holder {
        private static final SchedulerFactory INSTANCE = new SchedulerFactory();
    }

    /**
     * Returns the singleton factory instance.
     */
    static SchedulerFactory getInstance() {
        return Holder.INSTANCE;
    }

    /** cached scheduler (may be {@code null} until first access) */
    private ScheduledExecutorService scheduler;
    /** remembers whether the cached scheduler was created for virtual threads */
    private boolean virtual;
    /** ReentrantLock to protect the scheduler state - virtual thread compatible */
    private final ReentrantLock lock = new ReentrantLock();

    private SchedulerFactory() {
    }

    /**
     * Returns a {@link ScheduledExecutorService} matching the current Resilience4j thread-type
     * configuration. If the configuration changed since the last call, the previous scheduler
     * is shut down gracefully, so that already scheduled permission timeouts still fire, and a
     * new one is created.
     */
    ScheduledExecutorService getScheduler() {
        ScheduledExecutorService old = null;
        ScheduledExecutorService result;

        lock.lock();
        try {
            boolean desiredVirtual = ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL;

            if (scheduler == null
                || desiredVirtual != virtual
                || scheduler.isShutdown()
                || scheduler.isTerminated()) {

                ThreadType desiredType = desiredVirtual ? ThreadType.VIRTUAL : ThreadType.PLATFORM;
                ScheduledExecutorService fresh =
                    ExecutorServiceFactory.newSingleThreadScheduledExecutor(
                        "BulkheadPermissionTimeoutThread", desiredType);

                old = scheduler;
                scheduler = fresh;
                virtual = desiredVirtual;
            }
            result = scheduler;
        } finally {
            lock.unlock();
        }

        if (old != null) {
            old.shutdown();
        }
        return result;
    }

    /**
     * For test-code: shut down and forget the current scheduler so that the next
     * {@link #getScheduler()} call creates a fresh executor according to the then active
     * configuration. Already scheduled permission timeouts still fire on the old scheduler.
     */
    void reset() {
        ScheduledExecutorService old;

        lock.lock();
        try {
            old = scheduler;
            scheduler = null;
        } finally {
            lock.unlock();
        }

        if (old != null) {
            old.shutdown();
        }
    }
}
