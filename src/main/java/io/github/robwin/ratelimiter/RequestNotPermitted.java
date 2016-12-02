package io.github.robwin.ratelimiter;

/**
 * Exception that indicates that current thread was not able to acquire permission
 * from {@link RateLimiter}.
 */
public class RequestNotPermitted extends RuntimeException {

    /**
     * The constructor with a message.
     *
     * @param message The message.
     */
    public RequestNotPermitted(final String message) {
        super(message);
    }
}