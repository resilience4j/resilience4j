package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadType;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton factory for a {@link ScheduledExecutorService} used by the circuit-breaker
 * module. <p>
 *
 * The chosen executor type (platform vs virtual threads) depends on the current value
 * of {@code resilience4j.thread.type}.  Because tests or applications may change this
 * property at runtime, the factory detects configuration changes and transparently
 * recreates the scheduler so that callers always receive an executor that matches the
 * *current* setting. <p>
 *
 * A {@link #reset()} method is provided mainly for tests to force recreation.
 */
public final class SchedulerFactory {

    private static final class Holder {
        private static final SchedulerFactory INSTANCE = new SchedulerFactory();
    }

    /**
     * Returns the singleton factory instance.
     */
    public static SchedulerFactory getInstance() {
        return Holder.INSTANCE;
    }

    /* ──────────────────────────────
     *  Instance state
     * ────────────────────────────── */

    /** cached scheduler (may be {@code null} until first access) */
    private ScheduledExecutorService scheduler;
    /** remembers whether the cached scheduler was created for virtual threads */
    private boolean virtual;
    /** ReentrantLock to protect the scheduler state - virtual thread compatible */
    private final ReentrantLock lock = new ReentrantLock();

    private SchedulerFactory() { }

    /**
     * Returns a {@link ScheduledExecutorService} matching the current Resilience4j
     * thread-type configuration.  If the configuration changed since the last call,
     * the previous scheduler is shut down and a new one is created.
     */
    public ScheduledExecutorService getScheduler() {
        ScheduledExecutorService old = null;
        ScheduledExecutorService result;

        lock.lock();
        try {
            boolean desiredVirtual = ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL;

            if (scheduler == null
                || desiredVirtual != virtual
                || scheduler.isShutdown()
                || scheduler.isTerminated()) {

                ScheduledExecutorService fresh =
                    ExecutorServiceFactory.newSingleThreadScheduledExecutor(
                        "CircuitBreakerAutoTransitionThread");

                old = scheduler;
                scheduler = fresh;
                virtual = desiredVirtual;
            }
            result = scheduler;
        } finally {
            lock.unlock();
        }

        if (old != null) {
            old.shutdownNow();
        }
        return result;
    }

    /**
     * For test-code: shut down and forget the current scheduler so that the next
     * {@link #getScheduler()} call creates a fresh executor according to the then
     * active configuration.
     */
    public void reset() {
        ScheduledExecutorService old;

        lock.lock();
        try {
            old = scheduler;
            scheduler = null;
        } finally {
            lock.unlock();
        }

        if (old != null) {
            old.shutdownNow();
        }
    }
}
