package io.github.resilience4j.ratelimiter.monitoring.health;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class RateLimiterHealthIndicator implements HealthIndicator {

    private final RateLimiter rateLimiter;
    private final long timeoutInNanos;

    public RateLimiterHealthIndicator(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        timeoutInNanos = rateLimiter.getRateLimiterConfig().getTimeoutDuration().toNanos();
    }

    @Override
    public Health health() {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        int availablePermissions = metrics.getAvailablePermissions();
        int numberOfWaitingThreads = metrics.getNumberOfWaitingThreads();
        if (availablePermissions < 0 || numberOfWaitingThreads == 0) {
            return rateLimiterHealth(Status.UP, availablePermissions, numberOfWaitingThreads);
        }
        if (rateLimiter instanceof AtomicRateLimiter) {
            AtomicRateLimiter atomicRateLimiter = (AtomicRateLimiter) this.rateLimiter;
            AtomicRateLimiter.AtomicRateLimiterMetrics detailedMetrics = atomicRateLimiter.getDetailedMetrics();
            if (detailedMetrics.getNanosToWait() > timeoutInNanos) {
                rateLimiterHealth(Status.DOWN, availablePermissions, numberOfWaitingThreads);
            }
        }
        return rateLimiterHealth(Status.UNKNOWN, availablePermissions, numberOfWaitingThreads);
    }

    private Health rateLimiterHealth(Status status, int availablePermissions, int numberOfWaitingThreads) {
        return Health.status(status)
            .withDetail("availablePermissions", availablePermissions)
            .withDetail("numberOfWaitingThreads", numberOfWaitingThreads)
            .build();
    }
}