package io.github.resilience4j.timelimiter;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class TimeLimiterConfig {

    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";

    private Duration timeoutDuration = Duration.ofSeconds(1);
    private boolean cancelRunningFuture = true;

    private TimeLimiterConfig() {
    }

    /**
     * Returns a builder to create a custom TimeLimiterConfig.
     *
     * @return a {@link TimeLimiterConfig.Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    public static Builder from(TimeLimiterConfig baseConfig) {
        return new Builder(baseConfig);
    }
    /**
     * Creates a default TimeLimiter configuration.
     *
     * @return a default TimeLimiter configuration.
     */
    public static TimeLimiterConfig ofDefaults() {
        return new Builder().build();
    }

    private static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public boolean shouldCancelRunningFuture() {
        return cancelRunningFuture;
    }

    @Override
    public String toString() {
        return "TimeLimiterConfig{" +
            "timeoutDuration=" + timeoutDuration +
            "cancelRunningFuture=" + cancelRunningFuture +
            '}';
    }

    public static class Builder {

        private Duration timeoutDuration = Duration.ofSeconds(1);
        private boolean cancelRunningFuture = true;

        public Builder() {
        }

        public Builder(TimeLimiterConfig baseConfig) {
            this.timeoutDuration = baseConfig.timeoutDuration;
            this.cancelRunningFuture = baseConfig.cancelRunningFuture;
        }

        /**
         * Builds a TimeLimiterConfig
         *
         * @return the TimeLimiterConfig
         */
        public TimeLimiterConfig build() {
            TimeLimiterConfig config = new TimeLimiterConfig();
            config.timeoutDuration = timeoutDuration;
            config.cancelRunningFuture = cancelRunningFuture;
            return config;
        }

        /**
         * Configures the thread execution timeout Default value is 1 second.
         *
         * @param timeoutDuration the timeout Duration
         * @return the TimeLimiterConfig.Builder
         */
        public Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures whether cancel is called on the running future Defaults to TRUE
         *
         * @param cancelRunningFuture to cancel or not
         * @return the TimeLimiterConfig.Builder
         */
        public Builder cancelRunningFuture(final boolean cancelRunningFuture) {
            this.cancelRunningFuture = cancelRunningFuture;
            return this;
        }

    }
}