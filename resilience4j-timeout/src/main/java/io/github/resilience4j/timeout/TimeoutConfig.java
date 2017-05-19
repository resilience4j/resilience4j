package io.github.resilience4j.timeout;

import java.time.Duration;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;

public class TimeoutConfig {
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String CANCEL_ON_EXCEPTION_MUST_NOT_BE_NULL = "CancelOnExecution must not be null";

    private Duration timeoutDuration =  Duration.ofSeconds(0);
    private Boolean cancelOnException = TRUE;

    private TimeoutConfig() {
    }

    /**
     * Returns a builder to create a custom TimeoutConfig.
     *
     * @return a {@link TimeoutConfig.Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Creates a default Timeout configuration.
     *
     * @return a default Timeout configuration.
     */
    public static TimeoutConfig ofDefaults(){
        return new Builder().build();
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public Boolean shouldCancelOnException() {
       return cancelOnException;
    }

    @Override public String toString() {
        return "TimeoutConfig{" +
                "timeoutDuration=" + timeoutDuration +
                "cancelOnException=" + cancelOnException +
                '}';
    }

    public static class Builder {

        private TimeoutConfig config = new TimeoutConfig();

        /**
         * Builds a TimeoutConfig
         *
         * @return the TimeoutConfig
         */
        public TimeoutConfig build() {
            return config;
        }

        /**
         * Configures the thread execution timeout
         * Default value is 5 seconds.
         *
         * @param timeoutDuration the timeout Duration
         * @return the TimeoutConfig.Builder
         */
        public Builder timeoutDuration(final Duration timeoutDuration) {
            config.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures canceling on Future thread execution
         * Default value is TRUE
         *
         * @param cancelOnException should cancel on exception
         * @return the TimeoutConfig.Builder
         */
        public Builder cancelOnException(final Boolean cancelOnException) {
            config.cancelOnException = checkCancelOnException(cancelOnException);
            return this;
        }

    }

    private static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    private static Boolean checkCancelOnException(final Boolean cancelOnException) {
        return requireNonNull(cancelOnException, CANCEL_ON_EXCEPTION_MUST_NOT_BE_NULL);
    }

}
