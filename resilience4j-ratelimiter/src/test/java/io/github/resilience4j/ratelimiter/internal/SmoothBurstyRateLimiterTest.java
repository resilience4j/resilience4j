package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SmoothBurstyRateLimiter.class)
public class SmoothBurstyRateLimiterTest {

    private static final long PERIOD_IN_NANOS = 250_000_000L;
    private static final int PERMISSIONS_IN_PERIOD = 1;

    @Test
    public void acquireBigNumberOfPermitsAtStartTest() throws InterruptedException {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .initialPermits(0)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = new SmoothBurstyRateLimiter("smooth-bursty-rate-limiter", config);

        Thread.sleep(250l);

        //Five permissions one by one
        for (int i = 0; i < 5; i++) {
            boolean firstPermission = limiter.acquirePermission(1);
            then(firstPermission).isTrue();
        }

        for (int i = 0; i < 5; i++) {
            boolean secondPermission = limiter.acquirePermission(1);
            then(secondPermission).isTrue();
        }

        /**
         * A refill is highly likely based on the time it took the previous steps to complete
         */
        limiter.acquirePermission(1);
        limiter.acquirePermission(1);

        boolean firstNoPermission = limiter.acquirePermission(1);
        then(firstNoPermission).isFalse();
    }

    @Test
    public void tryToAcquireBigNumberOfPermitsIncrementally() throws InterruptedException {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = new SmoothBurstyRateLimiter("smooth-bursty-rate-limiter", config);

        Thread.sleep(250);

        /**
         * All permissions are present upfront
         */
        boolean firstPermission = limiter.acquirePermission(1);
        then(firstPermission).isTrue();
        for (int i = 0; i < 5; i++) {
            boolean secondPermission = limiter.acquirePermission(1);
            then(secondPermission).isTrue();
        }

        for (int i = 0; i < 5; i++) {
            boolean secondPermission = limiter.acquirePermission(1);
            then(secondPermission).isTrue();
        }

        /**
         * There has been a replenish due to the time passed
         */
        limiter.acquirePermission(1);

        boolean firstNoPermission = limiter.acquirePermission(1);
        then(firstNoPermission).isFalse();

        Thread.sleep(125l);

        for (int i = 0; i < 5; i++) {
            boolean retryInSecondCyclePermission = limiter.acquirePermission(1);
            then(retryInSecondCyclePermission).isTrue();
        }
    }

    @Test
    public void notSpyRawTest() throws InterruptedException {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(PERMISSIONS_IN_PERIOD)
            .limitRefreshPeriod(Duration.ofNanos(PERIOD_IN_NANOS))
            .timeoutDuration(Duration.ZERO)
            .build();

        SmoothBurstyRateLimiter rateLimiter = new SmoothBurstyRateLimiter("smooth-bursty-rate-limiter", config);

        Thread.sleep(250l);
        boolean firstPermission = rateLimiter.acquirePermission();
        then(firstPermission).isTrue();

        boolean secondPermission = rateLimiter.acquirePermission();
        boolean firstNoPermission = rateLimiter.acquirePermission();

        then(secondPermission).isTrue();
        then(firstNoPermission).isFalse();

        rateLimiter.changeLimitForPeriod(PERMISSIONS_IN_PERIOD * 2);
        Thread.sleep(250l);
        boolean thirdPermission = rateLimiter.acquirePermission();
        waitNanos(PERIOD_IN_NANOS/2l,'&');
        boolean fourthPermission = rateLimiter.acquirePermission();
        boolean secondNoPermission = rateLimiter.acquirePermission();
        then(thirdPermission).isTrue();
        then(fourthPermission).isTrue();
        then(secondNoPermission).isFalse();
    }


    @Test
    public void refillTest() throws InterruptedException {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMillis(1000))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiter limiter = new SmoothBurstyRateLimiter("smooth-bursty-rate-limiter", rateLimiterConfig);
        Thread.sleep(1000l);
        consumeUntilEmpty(limiter, 10);
        Thread.sleep(100);
        Assert.assertTrue(limiter.acquirePermission(1));
        Assume.assumeFalse(consumeIncludingFailure(limiter,4));
        Thread.sleep(410);
        consumeSuccessfully(limiter, 4);
        Assume.assumeFalse(limiter.acquirePermission(1));
    }

    /**
     * The guava rate limiter does not provide us with actual consume x permissions functionality
     * @param rateLimiter
     * @param permissions
     */
    private void consumeSuccessfully(RateLimiter rateLimiter, int permissions) {
        for(int i=0; i< permissions; i++) {
            Assert.assertTrue(rateLimiter.acquirePermission(1));
        }
    }

    private void consumeUntilEmpty(RateLimiter rateLimiter, int permissions) {
        for(int i=0; i< permissions; i++) {
            if(!rateLimiter.acquirePermission(1)) {
                break;
            }
        }
    }

    private boolean consumeIncludingFailure(RateLimiter rateLimiter, int permissions) {
        for(int i=0; i< permissions; i++) {
            if(!rateLimiter.acquirePermission(1)) {
                return false;
            }
        }

        return true;
    }

    private void waitNanos(long nanosToWait,char printedWhileWaiting) {
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < nanosToWait) {
            System.out.print(printedWhileWaiting);
        }
        System.out.println();
    }
}
