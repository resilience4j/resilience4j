package io.github.resilience4j.retry;

/**
 * A {@link MaxRetriesExceededException} signals that a {@link Retry} has exhausted all attempts,
 * and the result is still not satisfactory to {@link RetryConfig#getResultPredicate()}
 */
public class MaxRetriesExceededException extends RuntimeException {

    private final transient String causingRetryName;

    private MaxRetriesExceededException(String causingRetryName, String message, boolean writeableStackTrace) {
        super(message, null, false, writeableStackTrace);
        this.causingRetryName = causingRetryName;
    }

    /**
     * Static method to construct a {@link MaxRetriesExceededException}
     * @param retry the Retry which failed
     */
    public static MaxRetriesExceededException createMaxRetriesExceededException(Retry retry) {
        boolean writeStackTrace = retry.getRetryConfig().isWritableStackTraceEnabled();
        String message = String.format(
            "Retry '%s' has exhausted all attempts (%d)",
            retry.getName(),
            retry.getRetryConfig().getMaxAttempts()
        );
        return new MaxRetriesExceededException(retry.getName(), message, writeStackTrace);
    }

    /**
     * @return the name of the {@link Retry} that caused this exception
     */
    public String getCausingRetryName() {
        return causingRetryName;
    }
}
