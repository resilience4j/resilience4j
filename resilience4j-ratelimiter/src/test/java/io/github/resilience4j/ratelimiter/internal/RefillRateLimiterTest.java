/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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

import com.jayway.awaitility.core.ConditionFactory;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RefillRateLimiterConfig;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.equalTo;


@RunWith(PowerMockRunner.class)
@PrepareForTest(RefillRateLimiter.class)
public class RefillRateLimiterTest {

    private static final String LIMITER_NAME = "test";
    private static final long PERIOD_IN_NANOS = 250_000_000L;
    private static final long POLL_INTERVAL_IN_NANOS = 2_000_000L;
    private static final int PERMISSIONS_IN_PERIOD = 1;
    private RefillRateLimiter rateLimiter;
    private RefillRateLimiter.RefillRateLimiterMetrics metrics;

    private static ConditionFactory awaitImpatiently() {
        return await()
            .pollDelay(1, TimeUnit.MICROSECONDS)
            .pollInterval(POLL_INTERVAL_IN_NANOS, TimeUnit.NANOSECONDS);
    }

    protected RateLimiter buildRateLimiter(RefillRateLimiterConfig config) {
        RefillRateLimiterConfig refillConfig = RefillRateLimiterConfig.from(config)
            .permitCapacity(10)
            .initialPermits(0)
            .build();
        return new RefillRateLimiter("refill", refillConfig);
    }

    private void setTimeOnNanos(long nanoTime) throws Exception {
        PowerMockito.doReturn(nanoTime)
            .when(rateLimiter, "currentNanoTime");
    }

    public void setup(Duration timeoutDuration) {
        setup(Duration.ofNanos(PERIOD_IN_NANOS), timeoutDuration, PERMISSIONS_IN_PERIOD);
    }

    public void setup(Duration periodDuration, Duration timeoutDuration, int permissionPerCycle) {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(permissionPerCycle)
            .limitRefreshPeriod(periodDuration)
            .timeoutDuration(timeoutDuration)
            .build();
        RefillRateLimiter testLimiter = new RefillRateLimiter(LIMITER_NAME, rateLimiterConfig);
        rateLimiter = PowerMockito.spy(testLimiter);
        metrics = rateLimiter.getDetailedMetrics();
    }

    /**
     * Added more nanos in order to handle the refills
     */
    @Test
    public void acquireBigNumberOfPermitsAtStartTest() {
        RefillRateLimiterConfig config = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .initialPermits(0)
            .limitRefreshPeriod(Duration.ofNanos(600_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config, '.');

        boolean firstPermission = limiter.acquirePermission(5);
        then(firstPermission).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(5);
        then(firstNoPermission).isFalse();

        waitForRefresh(metrics, config, '*');

        boolean retryInNewCyclePermission = limiter.acquirePermission(1);
        then(retryInNewCyclePermission).isTrue();
    }

    @Test
    public void notSpyRawTest() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(PERMISSIONS_IN_PERIOD)
            .limitRefreshPeriod(Duration.ofNanos(PERIOD_IN_NANOS))
            .timeoutDuration(Duration.ZERO)
            .build();

        RefillRateLimiter rateLimiter = new RefillRateLimiter("refillBasedLimiter", rateLimiterConfig);
        RefillRateLimiter.RefillRateLimiterMetrics rateLimiterMetrics = rateLimiter
            .getDetailedMetrics();

        waitForMaxPermissions(rateLimiterMetrics, '.');

        boolean firstPermission = rateLimiter.acquirePermission();
        then(firstPermission).isTrue();

        waitForPermissionRenewal(rateLimiterMetrics, '*');
        boolean secondPermission = rateLimiter.acquirePermission();
        then(secondPermission).isTrue();

        boolean firstNoPermission = rateLimiter.acquirePermission();
        then(firstNoPermission).isFalse();

        rateLimiter.changeLimitForPeriod(PERMISSIONS_IN_PERIOD * 2);
        waitForPermissionRenewal(rateLimiterMetrics, '^');
        boolean thirdPermission = rateLimiter.acquirePermission();
        then(thirdPermission).isTrue();

        /**
         * Permission renewal happens per nanos thus the cycle splitting is not taking effect
         */
        waitNanos(PERIOD_IN_NANOS/2l,'&');
        boolean fourthPermission = rateLimiter.acquirePermission();
        then(fourthPermission).isTrue();

        boolean secondNoPermission = rateLimiter.acquirePermission();
        then(secondNoPermission).isFalse();
    }

    @Test
    public void notSpyRawNonBlockingTest() {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(PERMISSIONS_IN_PERIOD)
            .limitRefreshPeriod(Duration.ofNanos(PERIOD_IN_NANOS))
            .timeoutDuration(Duration.ZERO)
            .build();

        RefillRateLimiter rateLimiter = new RefillRateLimiter("refillBasedLimiter", rateLimiterConfig);
        RefillRateLimiter.RefillRateLimiterMetrics rateLimiterMetrics = rateLimiter
            .getDetailedMetrics();

        waitForMaxPermissions(rateLimiterMetrics, '.');

        long firstPermission = rateLimiter.reservePermission();
        waitForPermissionRenewal(rateLimiterMetrics, '*');

        long secondPermission = rateLimiter.reservePermission();
        long firstNoPermission = rateLimiter.reservePermission();

        rateLimiter.changeLimitForPeriod(PERMISSIONS_IN_PERIOD * 2);

        /**
         * Permission renewal happens per nanos thus the cycle splitting is not taking effect
         */
        waitNanos(PERIOD_IN_NANOS, '^');
        long thirdPermission = rateLimiter.reservePermission();
        long fourthPermission = rateLimiter.reservePermission();
        long secondNoPermission = rateLimiter.reservePermission();


        then(firstPermission).isZero();
        then(secondPermission).isZero();
        then(thirdPermission).isZero();
        then(fourthPermission).isZero();

        then(firstNoPermission).isNegative();
        then(secondNoPermission).isNegative();
    }

    @Test
    public void defaultPermissionsAtStartup() throws Exception {
        setup(Duration.ZERO);

        setTimeOnNanos(PERIOD_IN_NANOS - 10);
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        int availablePermissions = metrics.getAvailablePermissions();
        then(availablePermissions).isEqualTo(PERMISSIONS_IN_PERIOD);
    }

    /**
     * TODO Zero permissions at startup
     *
     * @throws Exception
     */

    @Test
    public void acquireAndRefreshWithEventPublishing() throws Exception {
        setup(Duration.ZERO);

        setTimeOnNanos(PERIOD_IN_NANOS);

        boolean permission = rateLimiter.acquirePermission();
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        boolean secondPermission = rateLimiter.acquirePermission();
        then(secondPermission).isFalse();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);

        setTimeOnNanos(PERIOD_IN_NANOS * 2);
        boolean thirdPermission = rateLimiter.acquirePermission();
        then(thirdPermission).isTrue();

        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        boolean fourthPermission = rateLimiter.acquirePermission();

        then(fourthPermission).isFalse();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
    }

    @Test
    public void reserveAndRefresh() throws Exception {
        setup(Duration.ofNanos(PERIOD_IN_NANOS));

        setTimeOnNanos(PERIOD_IN_NANOS);
        boolean permission = rateLimiter.acquirePermission();
        then(permission).isTrue();
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);

        AtomicReference<Boolean> reservedPermission = new AtomicReference<>(null);
        Thread caller = new Thread(
            () -> reservedPermission.set(rateLimiter.acquirePermission()));
        caller.setDaemon(true);
        caller.start();
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(caller::getState, equalTo(Thread.State.TIMED_WAITING));
        then(metrics.getAvailablePermissions()).isEqualTo(-1);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS + PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(1);

        setTimeOnNanos(PERIOD_IN_NANOS * 2 + 10);
        awaitImpatiently()
            .atMost(5, SECONDS)
            .until(reservedPermission::get, equalTo(true));

        /**
         * Due to not acting in the context of cycles put on the time expected to reserve
         */
        then(metrics.getAvailablePermissions()).isEqualTo(0);
        then(metrics.getNanosToWait()).isEqualTo(PERIOD_IN_NANOS);
        then(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void refillTest() throws InterruptedException {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMillis(1000))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiter limiter = new RefillRateLimiter("refill", rateLimiterConfig);
        Assert.assertTrue(limiter.acquirePermission(10));
        Thread.sleep(100);
        Assert.assertTrue(limiter.acquirePermission(1));
        Assume.assumeFalse(limiter.acquirePermission(4));
        Thread.sleep(410);
        Assume.assumeTrue(limiter.acquirePermission(4));
        Assume.assumeFalse(limiter.acquirePermission(1));
    }

    @Test
    public void testMaxCapacity() throws InterruptedException {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .limitForPeriod(10)
            .permitCapacity(20)
            .limitRefreshPeriod(Duration.ofMillis(1000))
            .timeoutDuration(Duration.ZERO)
            .build();

        RefillRateLimiter refillRateLimiter = new RefillRateLimiter("refill", rateLimiterConfig);
        Assert.assertTrue(refillRateLimiter.acquirePermission(10));

        for (int i = 1; i <= 20; i++) {
            Thread.sleep(100 * i);
            Assert.assertTrue(refillRateLimiter.acquirePermission(i));
        }
    }

    @Test
    public void testZeroInitialPermits() throws InterruptedException {
        RefillRateLimiterConfig rateLimiterConfig = RefillRateLimiterConfig.custom()
            .initialPermits(0)
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMillis(1000))
            .timeoutDuration(Duration.ZERO)
            .build();

        RefillRateLimiter refillRateLimiter = new RefillRateLimiter("refill", rateLimiterConfig);

        for (int i = 1; i <= 10; i++) {
            Thread.sleep(100 * i);
            Assert.assertTrue(refillRateLimiter.acquirePermission(i));
        }

        Assert.assertFalse(refillRateLimiter.acquirePermission(1));
    }

    private void waitForMaxPermissions(
        RefillRateLimiter.RefillRateLimiterMetrics rateLimiterMetrics, char printedWhileWaiting) {

        while (PERMISSIONS_IN_PERIOD > rateLimiterMetrics.getAvailablePermissions()) {
            System.out.print(printedWhileWaiting);
        }

        System.out.println();
    }

    private void waitForPermissionRenewal(
        RefillRateLimiter.RefillRateLimiterMetrics rawDetailedMetrics, char printedWhileWaiting) {
        long nanosToWait = rawDetailedMetrics.getNanosToWait();
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < nanosToWait) {
            System.out.print(printedWhileWaiting);
        }
        System.out.println();
    }

    private void waitNanos(long nanosToWait,char printedWhileWaiting) {
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < nanosToWait) {
            System.out.print(printedWhileWaiting);
        }
        System.out.println();
    }

    protected void waitForRefresh(RateLimiter.Metrics metrics, RefillRateLimiterConfig config,
                                  char printedWhileWaiting) {
        Instant start = Instant.now();
        while (Instant.now().isBefore(start.plus(config.getLimitRefreshPeriod()))) {
            try {
                if (metrics.getAvailablePermissions() >= config.getPermitCapacity()) {
                    break;
                }
                System.out.print(printedWhileWaiting);
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
        System.out.println();
    }
}
