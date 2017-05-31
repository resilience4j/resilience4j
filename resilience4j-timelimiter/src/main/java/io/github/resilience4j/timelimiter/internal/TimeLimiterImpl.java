package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

public class TimeLimiterImpl implements TimeLimiter {
    private final TimeLimiterConfig timeLimiterConfig;

    public TimeLimiterImpl(TimeLimiterConfig timeLimiterConfig) {
        this.timeLimiterConfig = timeLimiterConfig;
    }

    @Override
    public TimeLimiterConfig getTimeLimiterConfig() {
        return timeLimiterConfig;
    }
}
