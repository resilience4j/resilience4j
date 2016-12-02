package io.github.robwin.ratelimiter;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

public class RateLimiterConfig {
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL = "LimitRefreshPeriod must not be null";
    private static final Duration ACCEPTABLE_REFRESH_PERIOD = Duration.ofNanos(1L);

    private final Duration timeoutDuration;
    private final Duration limitRefreshPeriod;
    private final int limitForPeriod;

    private RateLimiterConfig(final Duration timeoutDuration, final Duration limitRefreshPeriod, final int limitForPeriod) {
        this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
        this.limitRefreshPeriod = checkLimitRefreshPeriod(limitRefreshPeriod);
        this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public Duration getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public static class Builder {

        private Duration timeoutDuration;
        private Duration limitRefreshPeriod;
        private int limitForPeriod;

        /**
         * Builds a RateLimiterConfig
         *
         * @return the RateLimiterConfig
         */
        public RateLimiterConfig build() {
            return new RateLimiterConfig(
                timeoutDuration,
                limitRefreshPeriod,
                limitForPeriod
            );
        }

        /**
         * Configures the default wait for permission duration.
         *
         * @param timeoutDuration the default wait for permission duration
         * @return the RateLimiterConfig.Builder
         */
        public Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures the period of limit refresh.
         * After each period rate limiter sets its permissions
         * count to {@link RateLimiterConfig#limitForPeriod} value.
         *
         * @param limitRefreshPeriod the period of limit refresh
         * @return the RateLimiterConfig.Builder
         */
        public Builder limitRefreshPeriod(final Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = checkLimitRefreshPeriod(limitRefreshPeriod);
            return this;
        }

        /**
         * Configures the permissions limit for refresh period.
         * Count of permissions available during one rate limiter period
         * specified by {@link RateLimiterConfig#limitRefreshPeriod} value.
         *
         * @param limitForPeriod the permissions limit for refresh period
         * @return the RateLimiterConfig.Builder
         */
        public Builder limitForPeriod(final int limitForPeriod) {
            this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
            return this;
        }

    }

    private static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    private static Duration checkLimitRefreshPeriod(Duration limitRefreshPeriod) {
        requireNonNull(limitRefreshPeriod, LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL);
        boolean refreshPeriodIsTooShort = limitRefreshPeriod.compareTo(ACCEPTABLE_REFRESH_PERIOD) < 0;
        if (refreshPeriodIsTooShort) {
            throw new IllegalArgumentException("LimitRefreshPeriod is too short");
        }
        return limitRefreshPeriod;
    }

    private static int checkLimitForPeriod(final int limitForPeriod) {
        if (limitForPeriod < 1) {
            throw new IllegalArgumentException("LimitForPeriod should be greater than 0");
        }
        return limitForPeriod;
    }
}
