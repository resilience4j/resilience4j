package io.github.resilience4j.timeout;

/**
 * Exception that indicates that current thread timed out
 * from {@link Timeout}.
 */
public class TimeoutException extends RuntimeException {

    /**
     * The constructor with a message.
     *
     * @param message The message.
     */
    public TimeoutException(final String message) {
        super(message);
    }

    /**
     * The constructor with a cause.
     *
     * @param cause the cause
     */
    public TimeoutException(final Throwable cause) {
        super(cause);
    }
}
