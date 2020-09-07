package io.github.resilience4j.retry.event;

import io.github.resilience4j.core.lang.Nullable;

import java.time.Duration;

/**
 * A RetryEvent which informs that a call failed, and is to be retried.
 */
public class RetryOnRetryEvent extends AbstractRetryEvent {

    private final Duration waitInterval;

    public RetryOnRetryEvent(String name, int numberOfAttempts, @Nullable Throwable lastThrowable,
        long waitInterval) {
        super(name, numberOfAttempts, lastThrowable);
        this.waitInterval = Duration.ofMillis(waitInterval);
    }

    @Override
    public Type getEventType() {
        return Type.RETRY;
    }

    /**
     * Returns the interval used to wait before next retry.
     *
     * @return the wait interval
     */
    public Duration getWaitInterval() {
        return waitInterval;
    }

    @Override
    public String toString() {
        return String.format(
            "%s: Retry '%s', waiting %s until attempt '%d'. Last attempt failed with exception '%s'.",
            getCreationTime(),
            getName(),
            getWaitInterval(),
            getNumberOfRetryAttempts(),
            getLastThrowable());
    }
}
