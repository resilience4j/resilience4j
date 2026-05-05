/*
 * Copyright 2026 kanghyun.yang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.core;

import org.slf4j.MDC;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Primitive for one-shot delayed task execution with cheap cancellation.
 * <p>
 * Intended for the "schedule a timer, usually cancel it before it fires" pattern
 * common in resilience primitives (hedge trigger timers, timeout guards, etc.).
 * Implementation varies by {@link ExecutorServiceFactory#getThreadType()}:
 * <ul>
 *   <li><b>Virtual mode</b>: each timer is backed by its own virtual thread that
 *       parks for the delay and runs the task on expiry. Leverages the cheap
 *       creation cost of virtual threads and avoids queue management / worker
 *       dispatch entirely. Cancellation interrupts the timer thread.</li>
 *   <li><b>Platform mode</b>: a process-wide daemon-backed
 *       {@link ScheduledExecutorService} singleton is reused across all calls.
 *       Per-timer platform thread creation would cost ~1ms + ~1MB stack, so a
 *       shared pool is preferred. Cancellation removes the task from the
 *       scheduler queue.</li>
 * </ul>
 * <p>
 * In both modes:
 * <ul>
 *   <li>A race-free state machine ({@code PENDING → FIRED | CANCELLED}) ensures
 *       the task runs at most once and {@code cancel()} returns {@code true}
 *       only if it prevented execution.</li>
 *   <li>MDC and {@link ContextPropagator} values are captured on the caller and
 *       restored on the firing thread, mirroring
 *       {@code ContextAwareScheduledThreadPoolExecutor} semantics.</li>
 * </ul>
 * <p>
 * This helper is a stateless gateway and does not expose {@code shutdown()}.
 * The shared platform scheduler uses daemon threads so JVM shutdown is never
 * blocked by pending timers.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
public final class OneShotDelayedScheduler {

    private static final String VIRTUAL_PREFIX = "OneShotDelayed-v-";
    private static final String PLATFORM_PREFIX = "OneShotDelayed-p-";

    private enum State { PENDING, FIRED, CANCELLED }

    /**
     * Lazy holder for the shared platform-mode scheduler. Initialised on first
     * use so processes running purely in virtual mode never pay for a platform
     * thread pool. Threads are daemon so the pool never blocks JVM exit.
     */
    private static final class PlatformSchedulerHolder {
        static final ScheduledExecutorService INSTANCE = Executors.newScheduledThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors()),
            Thread.ofPlatform().name(PLATFORM_PREFIX, 0).daemon(true).factory());
    }

    private OneShotDelayedScheduler() {
    }

    /**
     * Schedules {@code task} to run after {@code delay}. Returns a
     * {@link Cancellation} handle that can prevent the task from firing when
     * invoked before the delay elapses.
     *
     * @param delay               how long to wait before firing. Zero or negative
     *                            delays fire effectively immediately.
     * @param contextPropagators  propagators whose values are captured on the
     *                            calling thread and restored on the firing
     *                            thread. May be {@code null} or empty.
     * @param task                the task to run if not cancelled first. Must
     *                            not be {@code null}.
     * @return a cancellation handle. {@link Cancellation#cancel()} is
     *         idempotent and thread-safe.
     */
    public static Cancellation schedule(Duration delay,
                                        List<? extends ContextPropagator> contextPropagators,
                                        Runnable task) {
        Objects.requireNonNull(delay, "delay must not be null");
        Objects.requireNonNull(task, "task must not be null");

        List<? extends ContextPropagator> propagators =
            contextPropagators == null ? Collections.emptyList() : contextPropagators;

        Map<String, String> mdcSnapshot = Optional.ofNullable(MDC.getCopyOfContextMap())
            .orElse(Collections.emptyMap());

        Runnable wrapped = ContextPropagator.decorateRunnable(propagators, () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            MDC.clear();
            if (!mdcSnapshot.isEmpty()) {
                MDC.setContextMap(mdcSnapshot);
            }
            try {
                task.run();
            } finally {
                MDC.clear();
                if (previousMdc != null) {
                    MDC.setContextMap(previousMdc);
                }
            }
        });

        long delayNanos = delay.toNanos();
        AtomicReference<State> state = new AtomicReference<>(State.PENDING);

        // Transition PENDING -> FIRED atomically. If cancel() already won the
        // race and set the state to CANCELLED, skip execution. Shared between
        // virtual and platform paths so the observable contract is identical.
        Runnable fireIfPending = () -> {
            if (state.compareAndSet(State.PENDING, State.FIRED)) {
                wrapped.run();
            }
        };

        if (ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL) {
            Thread thread = Thread.ofVirtual()
                .name(VIRTUAL_PREFIX, 0)
                .unstarted(() -> {
                    if (delayNanos > 0) {
                        LockSupport.parkNanos(delayNanos);
                    }
                    fireIfPending.run();
                });
            thread.start();
            return () -> {
                if (state.compareAndSet(State.PENDING, State.CANCELLED)) {
                    thread.interrupt();
                    return true;
                }
                return false;
            };
        }

        // Platform mode: reuse a shared STPE rather than creating a platform
        // thread per timer. Platform thread creation costs ~1ms + ~1MB stack,
        // which would regress the fast-primary path relative to the previous
        // STPE-backed implementation.
        ScheduledFuture<?> scheduled = PlatformSchedulerHolder.INSTANCE.schedule(
            fireIfPending,
            Math.max(0L, delayNanos),
            TimeUnit.NANOSECONDS);
        return () -> {
            if (state.compareAndSet(State.PENDING, State.CANCELLED)) {
                scheduled.cancel(false);
                return true;
            }
            return false;
        };
    }

    /**
     * Handle returned by
     * {@link OneShotDelayedScheduler#schedule(Duration, List, Runnable)} to
     * cancel a pending one-shot task.
     */
    @FunctionalInterface
    public interface Cancellation {

        /**
         * Attempts to cancel the pending task. Returns {@code true} exactly once
         * if this call transitioned the task from pending to cancelled.
         * Subsequent calls return {@code false}. Safe to call concurrently and
         * after the task has already fired.
         *
         * @return {@code true} if this call was the one that cancelled the task.
         */
        boolean cancel();
    }
}
