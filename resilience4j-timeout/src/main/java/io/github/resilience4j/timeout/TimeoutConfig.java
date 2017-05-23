package io.github.resilience4j.timeout;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class TimeoutConfig {
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";

    private Duration timeoutDuration =  Duration.ofSeconds(1);

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

    @Override public String toString() {
        return "TimeoutConfig{" +
                "timeoutDuration=" + timeoutDuration +
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

    }

    private static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

}