package io.github.resilience4j.reactor.ratelimiter.operator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.stream.IntStream;

public class RateLimiterAssertions {
    private static final int LIMIT_FOR_PERIOD = 5;

    protected final RateLimiter rateLimiter = RateLimiter.of("test",
            RateLimiterConfig.custom().limitForPeriod(LIMIT_FOR_PERIOD).timeoutDuration(Duration.ZERO).limitRefreshPeriod(Duration.ofSeconds(10)).build());

    protected void assertUsedPermits(int used) {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        assertThat(metrics.getAvailablePermissions()).isEqualTo(LIMIT_FOR_PERIOD - used);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    protected void assertSinglePermitUsed() {
        assertUsedPermits(1);
    }

    protected void assertNoPermitLeft() {
        assertUsedPermits(LIMIT_FOR_PERIOD);
    }

    protected void saturateRateLimiter() {
        IntStream.range(0, 5).forEach(i -> assertThat(rateLimiter.getPermission(Duration.ofMillis(50))).isTrue());
    }

}
