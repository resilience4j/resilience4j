/*
 *
 *  Copyright 2026 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for a scheduler-death bug in {@link SemaphoreBasedRateLimiter#refreshLimit()}.
 *
 * <p>{@code refreshLimit()} computes {@code permissionsToRelease = limitForPeriod -
 * semaphore.availablePermits()} and then calls {@code semaphore.release(permissionsToRelease)}.
 * When {@link RateLimiterConfig#getLimitForPeriod()} is lowered at runtime via
 * {@code changeLimitForPeriod(int)} below the number of permits currently available, the
 * subtraction goes negative and {@link java.util.concurrent.Semaphore#release(int)} throws
 * {@link IllegalArgumentException} (JDK contract: permits &lt; 0 is illegal).
 *
 * <p>Because {@code refreshLimit} is the task behind
 * {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate}, that uncaught
 * exception permanently suppresses every subsequent execution (JDK contract for fixed-rate
 * tasks), so the limiter stops replenishing permits forever — every later
 * {@code acquirePermission} eventually returns {@code false} / throws {@code RequestNotPermitted}
 * until the instance is discarded.
 *
 * <p>This test reproduces the failure deterministically with a <em>real</em>
 * {@code ScheduledThreadPoolExecutor} (a thin subclass that only counts periodic firings; it
 * delegates all scheduling to the real implementation — it is not a mock). It lowers the limit
 * below the available permits and asserts:
 * <ol>
 *   <li>the periodic refresh task keeps running, and</li>
 *   <li>excess permits are clamped down to the new limit rather than left in place
 *       (analogous to {@code AtomicRateLimiter} clamping excess permits at each cycle boundary).</li>
 * </ol>
 *
 * <p>Waits are condition-based with a nanoTime deadline (no {@code Thread.sleep}) and the test
 * depends only on JUnit 5, so it needs no extra test libraries on the module classpath.
 */
class SemaphoreBasedRateLimiterRefreshLimitRegressionTest {

    private static final int INITIAL_LIMIT_FOR_PERIOD = 5;
    private static final int LOWERED_LIMIT_FOR_PERIOD = 1;
    private static final Duration REFRESH_PERIOD = Duration.ofMillis(5);

    @Test
    void refreshSchedulerMustSurviveLimitDecreaseBelowAvailablePermits() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(INITIAL_LIMIT_FOR_PERIOD)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .timeoutDuration(Duration.ofSeconds(1))
            .build();

        CountingScheduledExecutor scheduler = new CountingScheduledExecutor();
        SemaphoreBasedRateLimiter limiter =
            new SemaphoreBasedRateLimiter("refresh-limit-regression", config, scheduler);
        try {
            // 1. Warm up: prove the periodic refresh task is actually firing. While
            //    limitForPeriod (5) == availablePermits (5), refreshLimit computes 0 and never throws.
            assertTrue(waitForWithin(Duration.ofSeconds(2), () -> scheduler.refreshCount.get() >= 5),
                "periodic refresh task should fire during warm-up");

            // 2. Arm the bug: lower the limit below the currently available permits.
            //    No permits have been acquired, so availablePermits == 5 > new limit (1). On the
            //    unfixed code the next refreshLimit evaluates 1 - 5 = -4 and throws.
            limiter.changeLimitForPeriod(LOWERED_LIMIT_FOR_PERIOD);

            // Capture the liveness baseline AFTER the new limit is in effect. This is deliberately
            // done after changeLimitForPeriod (not before) so the baseline is immune to main-thread
            // scheduling jitter between reading the count and applying the change — without this, the
            // scheduler could advance several "old-limit" ticks during the gap, making a pre-change
            // baseline stale and the margin unreliable.
            int baseline = scheduler.refreshCount.get();

            // 3. Assert the scheduler is STILL firing well past the baseline. On the unfixed code the
            //    first refreshLimit after the change throws IllegalArgumentException and
            //    scheduleAtFixedRate suppresses all later executions, so the count freezes at
            //    baseline (+ at most one in-flight tick) and this wait fails -> test fails.
            //    Requiring +20 firings is a wide, deterministic margin: a healthy scheduler reaches
            //    it in ~100ms; a dead one never does.
            int requiredFirings = baseline + 20;
            boolean keptRunning = waitForWithin(Duration.ofSeconds(2),
                () -> scheduler.refreshCount.get() >= requiredFirings);
            assertTrue(keptRunning,
                "periodic refresh task stopped after lowering the limit "
                    + "(refreshCount=" + scheduler.refreshCount.get() + ", baseline=" + baseline + ")");

            // 4. Complementary signal: the fixed-rate future must still be RUNNING. For a fixed-rate
            //    task, isDone() flips to true ONLY on an uncaught exception or cancellation — i.e.
            //    precisely the scheduler death this regression guards against.
            assertFalse(scheduler.periodicFuture.get().isDone(),
                "periodic refresh task must not have terminated after lowering the limit");

            // 5. Excess permits must be clamped down to the new limit, not just left in place.
            //    After lowering the limit below available permits, refreshLimit should reduce
            //    available permits to (or below) the new limit on the next refresh — matching
            //    AtomicRateLimiter's behaviour of capping excess permits at each cycle boundary.
            int availableAfter = limiter.getMetrics().getAvailablePermissions();
            assertTrue(availableAfter <= LOWERED_LIMIT_FOR_PERIOD,
                "excess permits must be clamped to the new limit; available="
                    + availableAfter + ", newLimit=" + LOWERED_LIMIT_FOR_PERIOD);
        } finally {
            limiter.shutdown();
            scheduler.shutdownNow();
        }
    }

    /**
     * Direct exercise of the clamp branch in {@link SemaphoreBasedRateLimiter#refreshLimit()}.
     *
     * <p>With {@code limitForPeriod=5} the semaphore starts with 5 permits and no acquire
     * happens, so after {@code changeLimitForPeriod(1)} available permits are still 5
     * (greater than the new limit 1). Calling {@code refreshLimit()} must remove the
     * excess via {@code tryAcquire(-delta)} and must not throw.
     *
     * <p>This guarantees the documented contract of the clamp path: {@code
     * Semaphore.tryAcquire(int)} is non-throwing and all-or-nothing — if permits are
     * concurrently consumed between the {@code availablePermits} read and the {@code
     * tryAcquire} call so that the full excess is no longer available, {@code tryAcquire}
     * simply returns {@code false} (no exception is raised) and the surplus persists
     * until the next refresh tick clamps it. The scheduled refresh task therefore
     * survives any concurrent acquire traffic.
     */
    @Test
    void refreshLimitClampsExcessPermitsWithoutThrowing() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(INITIAL_LIMIT_FOR_PERIOD)
            .limitRefreshPeriod(Duration.ofHours(1)) // prevent scheduled refresh during this test
            .timeoutDuration(Duration.ofSeconds(1))
            .build();
        SemaphoreBasedRateLimiter limiter = new SemaphoreBasedRateLimiter("clamp-direct", config);
        try {
            int before = limiter.getMetrics().getAvailablePermissions();
            limiter.changeLimitForPeriod(LOWERED_LIMIT_FOR_PERIOD);
            // No acquire happens, so availablePermits is still `before` (> new limit).
            // Call refreshLimit() directly (package-private) to exercise the clamp branch.
            limiter.refreshLimit();
            int after = limiter.getMetrics().getAvailablePermissions();
            assertTrue(before > LOWERED_LIMIT_FOR_PERIOD,
                "precondition: excess must exist before the clamp; before=" + before);
            assertTrue(after <= LOWERED_LIMIT_FOR_PERIOD,
                "refreshLimit must clamp excess permits; before=" + before + " after=" + after);
        } finally {
            limiter.shutdown();
        }
    }

    /**
     * Waits for {@code condition} to become true within {@code timeout}, polling without
     * {@code Thread.sleep}. Returns the last evaluated state of the condition. Bounded by a
     * nanoTime deadline so it cannot hang.
     */
    private static boolean waitForWithin(Duration timeout, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        boolean last = condition.getAsBoolean();
        while (!last && System.nanoTime() < deadline) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            last = condition.getAsBoolean();
        }
        return last;
    }

    /**
     * A real, fully-functional {@link ScheduledThreadPoolExecutor} that additionally counts how many
     * times the periodic refresh task has fired and retains a handle to its future. All scheduling
     * behaviour is inherited from the JDK implementation unchanged; only the periodic-task hook is
     * instrumented, so observed timing/semantics are those of a real executor.
     */
    static final class CountingScheduledExecutor extends ScheduledThreadPoolExecutor {

        final AtomicInteger refreshCount = new AtomicInteger();
        final AtomicReference<ScheduledFuture<?>> periodicFuture = new AtomicReference<>();

        CountingScheduledExecutor() {
            super(1);
            setRemoveOnCancelPolicy(true);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                      long period, TimeUnit unit) {
            Runnable counting = () -> {
                refreshCount.incrementAndGet(); // count every scheduled firing before it runs
                command.run();
            };
            ScheduledFuture<?> future =
                super.scheduleAtFixedRate(counting, initialDelay, period, unit);
            periodicFuture.set(future);
            return future;
        }
    }
}
