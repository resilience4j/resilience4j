package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;

public class MockRateLimiter implements RateLimiter {

    static int CANT_GET_PERMIT = -1;

    private final long reserveDuration;

    MockRateLimiter(long reserveDuration) {
        this.reserveDuration = reserveDuration;
    }

    @Override
    public void changeTimeoutDuration(Duration timeoutDuration) {

    }

    @Override
    public void changeLimitForPeriod(int limitForPeriod) {

    }

    @Override
    public boolean getPermission(Duration timeoutDuration) {
        return false;
    }

    @Override
    public long reservePermission(Duration timeoutDuration) {
        return reserveDuration;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return RateLimiterConfig.custom().build();
    }

    @Override
    public Metrics getMetrics() {
        return null;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return null;
    }
}
