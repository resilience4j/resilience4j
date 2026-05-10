package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Parameterized test class that runs all common RateLimiter implementation tests
 * in both platform thread and virtual thread modes to ensure comprehensive coverage
 * and behavioral consistency across thread types.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class SemaphoreBasedRateLimiterParameterizedTest extends RateLimitersImplementationTest {

    public SemaphoreBasedRateLimiterParameterizedTest(ThreadType threadType) {
        super(threadType);
    }

    private SemaphoreBasedRateLimiter testLimiter;

    @Override
    protected RateLimiter buildRateLimiter(RateLimiterConfig config) {
        // Create limiter with null scheduler to use default scheduler
        // Thread mode is already configured by ThreadModeTestBase
        testLimiter = new SemaphoreBasedRateLimiter("parameterized-test", config, (ScheduledExecutorService) null);
        return testLimiter;
    }

    @After
    public void shutdownLimiter() {
        // Ensure proper cleanup of the limiter
        if (testLimiter != null) {
            testLimiter.shutdown();
        }
    }
}