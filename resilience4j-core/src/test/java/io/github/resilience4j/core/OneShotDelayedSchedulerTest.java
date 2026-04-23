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

import io.github.resilience4j.core.OneShotDelayedScheduler.Cancellation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the contract of {@link OneShotDelayedScheduler}:
 * delay honoring, cancel semantics, MDC + ContextPropagator propagation.
 * Parameterized over platform/virtual thread modes.
 */
@ExtendWith(ThreadModeExtension.class)
class OneShotDelayedSchedulerTest {

    @BeforeEach
    void cleanMdc() {
        MDC.clear();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @TestTemplate
    void taskFiresAfterDelay() throws InterruptedException {
        CountDownLatch fired = new CountDownLatch(1);
        OneShotDelayedScheduler.schedule(
            Duration.ofMillis(50),
            Collections.emptyList(),
            fired::countDown);

        assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void parkUntilDeadlineIgnoresEarlyUnpark() throws InterruptedException {
        Object pendingState = new Object();
        AtomicReference<Object> state = new AtomicReference<>(pendingState);
        AtomicLong elapsedNanos = new AtomicLong();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);

        Thread thread = Thread.ofVirtual().unstarted(() -> {
            long startNanos = System.nanoTime();
            started.countDown();
            OneShotDelayedScheduler.parkUntilDeadline(
                startNanos + TimeUnit.MILLISECONDS.toNanos(150),
                state,
                pendingState);
            elapsedNanos.set(System.nanoTime() - startNanos);
            finished.countDown();
        });

        thread.start();
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        LockSupport.unpark(thread);

        assertThat(finished.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(elapsedNanos.get()).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(100));
    }

    @Test
    void platformSchedulerRemovesCancelledTasksFromQueue() {
        ScheduledThreadPoolExecutor scheduler = OneShotDelayedScheduler.newPlatformScheduler();
        try {
            ScheduledFuture<?> scheduled = scheduler.schedule(() -> {}, 1, TimeUnit.HOURS);

            assertThat(scheduler.getQueue()).hasSize(1);
            assertThat(scheduled.cancel(false)).isTrue();
            assertThat(scheduler.getQueue()).isEmpty();
        } finally {
            scheduler.shutdownNow();
        }
    }

    @TestTemplate
    void cancelBeforeFirePreventsExecution() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        Cancellation cancellation = OneShotDelayedScheduler.schedule(
            Duration.ofMillis(500),
            Collections.emptyList(),
            () -> ran.set(true));

        // Cancel well before fire time.
        assertThat(cancellation.cancel()).isTrue();

        // Wait long enough that the task would have fired, then assert it didn't.
        Thread.sleep(700);
        assertThat(ran.get()).isFalse();
    }

    @TestTemplate
    void cancelIsIdempotent() {
        Cancellation cancellation = OneShotDelayedScheduler.schedule(
            Duration.ofMillis(500),
            Collections.emptyList(),
            () -> {});

        assertThat(cancellation.cancel()).isTrue();
        assertThat(cancellation.cancel()).isFalse();
        assertThat(cancellation.cancel()).isFalse();
    }

    @TestTemplate
    void cancelAfterFireReturnsFalse() throws InterruptedException {
        CountDownLatch fired = new CountDownLatch(1);
        Cancellation cancellation = OneShotDelayedScheduler.schedule(
            Duration.ZERO,
            Collections.emptyList(),
            fired::countDown);

        assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
        // Small settle window so the timer thread is past the cancel check.
        Thread.sleep(50);

        assertThat(cancellation.cancel()).isFalse();
    }

    @TestTemplate
    void mdcIsPropagatedToFiringThread() throws InterruptedException {
        MDC.put("request-id", "REQ-123");
        AtomicReference<String> seen = new AtomicReference<>();
        CountDownLatch fired = new CountDownLatch(1);

        OneShotDelayedScheduler.schedule(
            Duration.ofMillis(20),
            Collections.emptyList(),
            () -> {
                seen.set(MDC.get("request-id"));
                fired.countDown();
            });

        assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(seen.get()).isEqualTo("REQ-123");
    }

    @TestTemplate
    void mdcOnCallingThreadIsRestoredAfterTaskRuns() throws InterruptedException {
        // The calling thread's MDC must not be mutated by the timer thread's
        // MDC restoration side-effects (the two threads should be independent).
        MDC.put("caller", "CALLER-A");

        CountDownLatch fired = new CountDownLatch(1);
        OneShotDelayedScheduler.schedule(
            Duration.ofMillis(20),
            Collections.emptyList(),
            fired::countDown);

        assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(MDC.get("caller")).isEqualTo("CALLER-A");
    }

    @TestTemplate
    void contextPropagatorCarriesValueToFiringThread() throws InterruptedException {
        ThreadLocal<String> tenant = new ThreadLocal<>();
        tenant.set("TENANT-X");

        ContextPropagator<String> tenantPropagator = new ContextPropagator<>() {
            @Override
            public Supplier<Optional<String>> retrieve() {
                return () -> Optional.ofNullable(tenant.get());
            }

            @Override
            public Consumer<Optional<String>> copy() {
                return v -> v.ifPresent(tenant::set);
            }

            @Override
            public Consumer<Optional<String>> clear() {
                return v -> tenant.remove();
            }
        };

        AtomicReference<String> seen = new AtomicReference<>();
        CountDownLatch fired = new CountDownLatch(1);

        OneShotDelayedScheduler.schedule(
            Duration.ofMillis(20),
            List.of(tenantPropagator),
            () -> {
                seen.set(tenant.get());
                fired.countDown();
            });

        assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(seen.get()).isEqualTo("TENANT-X");
    }

    @TestTemplate
    void concurrentSchedulesAreIsolated() throws InterruptedException {
        int n = 16;
        CountDownLatch allFired = new CountDownLatch(n);
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < n; i++) {
            OneShotDelayedScheduler.schedule(
                Duration.ofMillis(30),
                Collections.emptyList(),
                () -> {
                    counter.incrementAndGet();
                    allFired.countDown();
                });
        }

        assertThat(allFired.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(counter.get()).isEqualTo(n);
    }

    @Test
    void nullTaskIsRejected() {
        assertThatThrownBy(() -> OneShotDelayedScheduler.schedule(
            Duration.ofMillis(10),
            Collections.emptyList(),
            null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullDelayIsRejected() {
        assertThatThrownBy(() -> OneShotDelayedScheduler.schedule(
            null,
            Collections.emptyList(),
            () -> {}))
            .isInstanceOf(NullPointerException.class);
    }
}
