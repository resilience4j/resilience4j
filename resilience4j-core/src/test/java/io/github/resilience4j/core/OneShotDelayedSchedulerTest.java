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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the contract of {@link OneShotDelayedScheduler}:
 * delay honoring, cancel semantics, MDC + ContextPropagator propagation.
 * Parameterized over platform/virtual thread modes.
 */
@RunWith(Parameterized.class)
public class OneShotDelayedSchedulerTest extends ThreadModeTestBase {

    public OneShotDelayedSchedulerTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    @Before
    public void cleanMdc() {
        MDC.clear();
    }

    @After
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void taskFiresAfterDelay() throws InterruptedException {
        CountDownLatch fired = new CountDownLatch(1);
        OneShotDelayedScheduler.schedule(
            Duration.ofMillis(50),
            Collections.emptyList(),
            fired::countDown);

        assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void cancelBeforeFirePreventsExecution() throws InterruptedException {
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

    @Test
    public void cancelIsIdempotent() {
        Cancellation cancellation = OneShotDelayedScheduler.schedule(
            Duration.ofMillis(500),
            Collections.emptyList(),
            () -> {});

        assertThat(cancellation.cancel()).isTrue();
        assertThat(cancellation.cancel()).isFalse();
        assertThat(cancellation.cancel()).isFalse();
    }

    @Test
    public void cancelAfterFireReturnsFalse() throws InterruptedException {
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

    @Test
    public void mdcIsPropagatedToFiringThread() throws InterruptedException {
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

    @Test
    public void mdcOnCallingThreadIsRestoredAfterTaskRuns() throws InterruptedException {
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

    @Test
    public void contextPropagatorCarriesValueToFiringThread() throws InterruptedException {
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

    @Test
    public void concurrentSchedulesAreIsolated() throws InterruptedException {
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

    @Test(expected = NullPointerException.class)
    public void nullTaskIsRejected() {
        OneShotDelayedScheduler.schedule(
            Duration.ofMillis(10),
            Collections.emptyList(),
            null);
    }

    @Test(expected = NullPointerException.class)
    public void nullDelayIsRejected() {
        OneShotDelayedScheduler.schedule(
            null,
            Collections.emptyList(),
            () -> {});
    }
}
