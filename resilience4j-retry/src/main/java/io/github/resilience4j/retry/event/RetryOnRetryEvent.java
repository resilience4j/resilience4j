package io.github.resilience4j.retry.event;

import io.github.resilience4j.core.lang.Nullable;

import java.time.Duration;

/**
 * A RetryEvent which informs that a call failed, and is to be retried.
 */
public class RetryOnRetryEvent extends AbstractRetryEvent {

    private final Duration waitInterval;
    @Nullable
    private final Object lastResult;

    public RetryOnRetryEvent(String name, int numberOfAttempts, @Nullable Throwable lastThrowable,
        long waitInterval) {
        this(name, numberOfAttempts, lastThrowable, waitInterval, null);
    }

    public RetryOnRetryEvent(String name, int numberOfAttempts, @Nullable Throwable lastThrowable,
        long waitInterval, @Nullable Object lastResult) {
        super(name, numberOfAttempts, lastThrowable);
        this.waitInterval = Duration.ofMillis(waitInterval);
        this.lastResult = lastResult;
    }

    @Override
    public Type getEventType() {
        return Type.RETRY;
    }

    @Override
    @Nullable
    public Object getLastResult() {
        return lastResult;
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
        if (lastResult != null) {
            return String.format(
                "%s: Retry '%s', waiting %s until attempt '%d'. Last attempt returned result '%s'.",
                getCreationTime(),
                getName(),
                getWaitInterval(),
                getNumberOfRetryAttempts(),
                lastResult);
        }
        return String.format(
            "%s: Retry '%s', waiting %s until attempt '%d'. Last attempt failed with exception '%s'.",
            getCreationTime(),
            getName(),
            getWaitInterval(),
            getNumberOfRetryAttempts(),
            getLastThrowable());
    }
}
