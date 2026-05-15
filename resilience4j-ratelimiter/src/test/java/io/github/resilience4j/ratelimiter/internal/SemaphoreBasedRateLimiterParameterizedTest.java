package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Test class that runs all common RateLimiter implementation tests
 * in both platform thread and virtual thread modes to ensure comprehensive coverage
 * and behavioral consistency across thread types.
 */
class SemaphoreBasedRateLimiterParameterizedTest extends RateLimitersImplementationTest {

    private SemaphoreBasedRateLimiter testLimiter;

    @Override
    protected RateLimiter buildRateLimiter(RateLimiterConfig config) {
        testLimiter = new SemaphoreBasedRateLimiter("parameterized-test", config, (ScheduledExecutorService) null);
        return testLimiter;
    }

    @AfterEach
    void shutdownLimiter() {
        if (testLimiter != null) {
            testLimiter.shutdown();
        }
    }
}
