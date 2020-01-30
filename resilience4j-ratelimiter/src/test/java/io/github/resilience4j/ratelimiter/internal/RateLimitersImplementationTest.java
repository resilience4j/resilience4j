package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for functions that are common in all implementations should go here
 */
public abstract class RateLimitersImplementationTest {

    protected abstract RateLimiter buildRateLimiter(RateLimiterConfig config);

    @Test
    public void acquireBigNumberOfPermitsAtStartOfCycleTest() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config, '.');

        boolean firstPermission = limiter.acquirePermission(5);
        then(firstPermission).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(1);
        then(firstNoPermission).isFalse();

        waitForRefresh(metrics, config, '*');

        boolean retryInNewCyclePermission = limiter.acquirePermission(1);
        then(retryInNewCyclePermission).isTrue();
    }

    @Test
    public void tryToAcquireBigNumberOfPermitsAtEndOfCycleTest() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config, '.');

        boolean firstPermission = limiter.acquirePermission(1);
        then(firstPermission).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(5);
        then(firstNoPermission).isFalse();

        waitForRefresh(metrics, config, '*');

        boolean retryInSecondCyclePermission = limiter.acquirePermission(5);
        then(retryInSecondCyclePermission).isTrue();
    }

    protected void waitForRefresh(RateLimiter.Metrics metrics, RateLimiterConfig config,
                                  char printedWhileWaiting) {
        Instant start = Instant.now();
        while (Instant.now().isBefore(start.plus(config.getLimitRefreshPeriod()))) {
            try {
                if (metrics.getAvailablePermissions() == config.getLimitForPeriod()) {
                    break;
                }
                System.out.print(printedWhileWaiting);
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
        System.out.println();
    }
}
