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
package com.google.common.util.concurrent;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

/**
 * This class contains tests that are applied both on the SmoothBursty and the SmoothBurstyFacade introduced rate limiter.
 * Also some negative tests highlight the limitations of the guava RateLimiter
 */
public class SmoothBurstyDecoratorTest {

    @Test
    public void testSmoothBurstyRateLimiter() throws InterruptedException {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        RateLimiter limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 0.25);
        limiter.setRate(40);
        Thread.sleep(240);
        assertTrue(limiter.tryAcquire(0, SECONDS));
        for(int i=0;i<9;i++) {
            assertTrue(limiter.tryAcquire(0, SECONDS));
        }
        assertFalse(limiter.tryAcquire(0, SECONDS));
        assertTrue(limiter.tryAcquire(25, MILLISECONDS));
        assertFalse(limiter.tryAcquire(0, MILLISECONDS));
        assertTrue(limiter.tryAcquire(25, MILLISECONDS));
        assertFalse(limiter.tryAcquire(0, SECONDS));
        Thread.sleep(1000);

        for (int i=0;i<10;i++) {
            assertTrue(limiter.tryAcquire(0, SECONDS));
        }
    }

    @Test
    public void testSmoothBurstyDecoratorRateLimiter() throws InterruptedException {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .burstForPeriod(10)
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .build();

        SmoothBurstyDecorator limiter = SmoothBurstyDecorator.create(rateLimiterConfig);
        limiter.setRate(40);

        Thread.sleep(240);
        for(int i=0;i<10;i++) {
            assertTrue(limiter.tryAcquire(0, SECONDS));
        }
        assertFalse(limiter.tryAcquire(0, SECONDS));
        assertTrue(limiter.tryAcquire(25, MILLISECONDS));
        assertFalse(limiter.tryAcquire(0, MILLISECONDS));
        assertTrue(limiter.tryAcquire(25, MILLISECONDS));
        assertFalse(limiter.tryAcquire(0, SECONDS));
        Thread.sleep(1000);

        for (int i=0;i<10;i++) {
            assertTrue(limiter.tryAcquire(0, SECONDS));
        }
    }


    /**
     * This is a showcase on the guava rate limiter not operating properly on multiple acquisitions
     * Actually through the codebase it's obvious that the num of permits is being ignored
     * @throws InterruptedException
     */
    @Test
    public void testSmoothBurstyBadMultiplePermitAcquisition() throws InterruptedException {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 0.25);
        limiter.setRate(40);
        Thread.sleep(250);
        /**
         * This obviously a value that could never get served in the first place
         */
        assertTrue(limiter.tryAcquire(50,0, SECONDS));

        Thread.sleep(1250);
        assertNotEquals(limiter.storedPermits, 10.0d, 0.5d);

        /**
         * Due to having permissions this would give a positive result, however only
         * one permission is consumed although multiple permissions have been specified
         */
        assertTrue(limiter.tryAcquire(1,0, SECONDS));
        assertEquals(limiter.storedPermits, 9.0d, 0.5d);
    }

    /**
     * This is a showcase on the guava rate limiter not operating properly on multiple acquisitions
     * Actually through the codebase it's obvious that the num of permits is being ignored
     * @throws InterruptedException
     */
    @Test
    public void testSmoothBurstyFacadeBadMultiplePermitAcquisition() throws InterruptedException {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .burstForPeriod(10)
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .build();

        SmoothBurstyDecorator limiter = SmoothBurstyDecorator.create(rateLimiterConfig);

        Thread.sleep(1250);
        /**
         * This obviously a value that could never get served in the first place
         */
        assertTrue(limiter.tryAcquire(50,0, SECONDS));

        Thread.sleep(1250);
        assertNotEquals(limiter.getAvailablePermissions(), 10.0d, 0.5d);

        /**
         * Due to having permissions this would give a positive result, however only
         * one permission is consumed although multiple permissions have been specified
         */
        assertTrue(limiter.tryAcquire(1,0, SECONDS));
        assertEquals(limiter.getAvailablePermissions(), 9.0d, 0.5d);
    }

    @Test
    public void testSmoothBadMetrics() throws InterruptedException {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 0.25);
        limiter.setRate(40);
        Thread.sleep(250);

        /**
         * 10 permissions should be present
         */
        Assert.assertEquals(0.0d,limiter.storedPermits,0.5);

        /**
         * The actual metrics are being up to date only when a permit is required
         */
        for(double i=9.0d; i>=0; i--) {
            assertTrue(limiter.tryAcquire(1,0, SECONDS));
            Assert.assertEquals(i,limiter.storedPermits,0.5);
        }

        Thread.sleep(250);
        /**
         * Should be back to 10 but it will stay zero because no permit was requested
         */
        Assert.assertEquals((double) 0,limiter.storedPermits,0.5);

        assertTrue(limiter.tryAcquire(1,0, SECONDS));

        /**
         * The permit acquisition triggered the updated of the storedPermits
         */
        Assert.assertEquals(9.0d,limiter.storedPermits,0.5);
    }

    @Test
    public void testSmoothFacadeBadMetrics() throws InterruptedException {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .burstForPeriod(10)
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .build();

        SmoothBurstyDecorator limiter = SmoothBurstyDecorator.create(rateLimiterConfig);

        Thread.sleep(250);

        /**
         * 10 permissions should be present
         */
        Assert.assertEquals(0.0d,limiter.getAvailablePermissions(),0.5);

        /**
         * The actual metrics are being up to date only when a permit is required
         */
        for(double i=9.0d; i>=0; i--) {
            assertTrue(limiter.tryAcquire(1,0, SECONDS));
            Assert.assertEquals(i,limiter.getAvailablePermissions(),0.5);
        }

        Thread.sleep(250);
        /**
         * Should be back to 10 but it will stay zero because no permit was requested
         */
        Assert.assertEquals((double) 0,limiter.getAvailablePermissions(),0.5);

        assertTrue(limiter.tryAcquire(1,0, SECONDS));

        /**
         * The permit acquisition triggered the updated of the storedPermits
         */
        Assert.assertEquals(9.0d,limiter.getAvailablePermissions(),0.5);
    }

    /**
     * Also there won't be a max capacity
     */
}