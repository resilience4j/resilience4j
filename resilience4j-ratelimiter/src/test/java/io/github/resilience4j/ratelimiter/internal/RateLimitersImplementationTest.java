package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnDrainedEvent;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for functions that are common in all implementations should go here.
 * Now supports both platform and virtual thread testing modes.
 */
public abstract class RateLimitersImplementationTest extends ThreadModeTestBase {

    public RateLimitersImplementationTest(ThreadType threadType) {
        super(threadType);
    }

    protected abstract RateLimiter buildRateLimiter(RateLimiterConfig config);

    @Test
    public void acquireBigNumberOfPermitsAtStartOfCycleTest() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(500_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config);

        boolean firstPermission = limiter.acquirePermission(5);
        then(firstPermission).describedAs("First permission acquisition in " + getThreadModeDescription()).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).describedAs("Second permission acquisition in " + getThreadModeDescription()).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(1);
        then(firstNoPermission).describedAs("Should reject additional permission in " + getThreadModeDescription()).isFalse();

        waitForRefresh(metrics, config);

        boolean retryInNewCyclePermission = limiter.acquirePermission(1);
        then(retryInNewCyclePermission).describedAs("Retry after refresh in " + getThreadModeDescription()).isTrue();
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

        waitForRefresh(metrics, config);

        boolean firstPermission = limiter.acquirePermission(1);
        then(firstPermission).describedAs("First permission (1 permit) in " + getThreadModeDescription()).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).describedAs("Second permission (5 permits) in " + getThreadModeDescription()).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(5);
        then(firstNoPermission).describedAs("Should reject 5 more permits (only 4 left) in " + getThreadModeDescription()).isFalse();

        waitForRefresh(metrics, config);

        boolean retryInSecondCyclePermission = limiter.acquirePermission(5);
        then(retryInSecondCyclePermission).describedAs("Should acquire 5 permits after refresh in " + getThreadModeDescription()).isTrue();
    }

    @Test
    public void tryToAcquirePermitsAfterDrainBeforeCycleEndsTest() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = buildRateLimiter(config);

        RateLimiter.Metrics metrics = limiter.getMetrics();
        ensureRefreshIsComplete(metrics, config);

        limiter.drainPermissions();

        boolean firstNoPermission = limiter.acquirePermission();
        then(firstNoPermission).describedAs("Should not acquire permission after draining in " + getThreadModeDescription()).isFalse();

        ensureNextCycleStarted(metrics, config);

        boolean retryInSecondCyclePermission = limiter.acquirePermission();
        then(retryInSecondCyclePermission).describedAs("Should acquire permission after cycle refresh in " + getThreadModeDescription()).isTrue();
    }

    @Test
    public void drainCycleWhichAlreadyHashNoPremitsLeftTest() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config);

        limiter.drainPermissions();
        boolean firstPermission = limiter.acquirePermission(10);
        then(firstPermission).describedAs("Should not acquire permissions after draining in " + getThreadModeDescription()).isFalse();

        AtomicReference<Object> eventAfterDrainCatcher = new AtomicReference<>();
        limiter.getEventPublisher().onEvent(event -> eventAfterDrainCatcher.set(event));
        limiter.drainPermissions();
        Object event = eventAfterDrainCatcher.get();
        then(event).describedAs("Event should be RateLimiterOnDrainedEvent in " + getThreadModeDescription()).isInstanceOf(RateLimiterOnDrainedEvent.class);
        then(((RateLimiterOnDrainedEvent) event).getNumberOfPermits()).describedAs("Drained permits should be zero in " + getThreadModeDescription()).isZero();
    }

    protected void waitForRefresh(RateLimiter.Metrics metrics, RateLimiterConfig config) {
        try {
            await()
                .pollInterval(25, MILLISECONDS)
                .atMost(config.getLimitRefreshPeriod().multipliedBy(3).toMillis(), MILLISECONDS)
                .until(() -> metrics.getAvailablePermissions() == config.getLimitForPeriod());
        } catch (Exception e) {
            throw new AssertionError("Failed to wait for refresh: " + e.getMessage() +
                ". Current permits: " + metrics.getAvailablePermissions() +
                ", Expected: " + config.getLimitForPeriod(), e);
        }
    }
    
    protected void ensureNextCycleStarted(RateLimiter.Metrics metrics, RateLimiterConfig config) {
        try {
            await()
                .pollInterval(25, MILLISECONDS)
                .atMost(config.getLimitRefreshPeriod().multipliedBy(3).toMillis(), MILLISECONDS)
                .until(() -> metrics.getAvailablePermissions() > 0);
        } catch (Exception e) {
            throw new AssertionError("Cycle did not refresh within expected time. " +
                "Current permits: " + metrics.getAvailablePermissions() + 
                ". Error: " + e.getMessage(), e);
        }
    }
    
    protected void ensureRefreshIsComplete(RateLimiter.Metrics metrics, RateLimiterConfig config) {
        try {
            await()
                .pollInterval(25, MILLISECONDS)
                .atMost(config.getLimitRefreshPeriod().multipliedBy(3).toMillis(), MILLISECONDS)
                .until(() -> metrics.getAvailablePermissions() == config.getLimitForPeriod());
        } catch (Exception e) {
            throw new AssertionError("Refresh did not complete within expected time. " +
                "Current permits: " + metrics.getAvailablePermissions() + 
                ", Expected: " + config.getLimitForPeriod() + 
                ". Error: " + e.getMessage(), e);
        }
    }
}
