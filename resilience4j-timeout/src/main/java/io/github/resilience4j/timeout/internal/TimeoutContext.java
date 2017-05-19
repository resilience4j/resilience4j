package io.github.resilience4j.timeout.internal;

import io.github.resilience4j.timeout.Timeout;
import io.github.resilience4j.timeout.TimeoutConfig;

public class TimeoutContext implements Timeout {
    private final TimeoutConfig timeoutConfig;

    public TimeoutContext(TimeoutConfig timeoutConfig) {
        this.timeoutConfig = timeoutConfig;
    }

    @Override
    public TimeoutConfig getTimeoutConfig() {
        return timeoutConfig;
    }
}
