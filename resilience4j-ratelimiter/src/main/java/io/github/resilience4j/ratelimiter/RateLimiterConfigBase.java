package io.github.resilience4j.ratelimiter;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class RateLimiterConfigBase implements RateLimiterConfig {

    static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    static final String LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL = "LimitRefreshPeriod must not be null";
    static final Duration ACCEPTABLE_REFRESH_PERIOD = Duration.ofNanos(1L);
    static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;

    private final Duration timeoutDuration;
    private final Duration limitRefreshPeriod;
    private final int limitForPeriod;
    private final boolean writableStackTraceEnabled;

    protected RateLimiterConfigBase(Duration timeoutDuration, Duration limitRefreshPeriod,
                                int limitForPeriod, boolean writableStackTraceEnabled) {
        this.timeoutDuration = timeoutDuration;
        this.limitRefreshPeriod = limitRefreshPeriod;
        this.limitForPeriod = limitForPeriod;
        this.writableStackTraceEnabled = writableStackTraceEnabled;
    }

    protected static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    protected static Duration checkLimitRefreshPeriod(Duration limitRefreshPeriod) {
        requireNonNull(limitRefreshPeriod, LIMIT_REFRESH_PERIOD_MUST_NOT_BE_NULL);
        boolean refreshPeriodIsTooShort =
            limitRefreshPeriod.compareTo(ACCEPTABLE_REFRESH_PERIOD) < 0;
        if (refreshPeriodIsTooShort) {
            throw new IllegalArgumentException("LimitRefreshPeriod is too short");
        }
        return limitRefreshPeriod;
    }

    protected static int checkLimitForPeriod(final int limitForPeriod) {
        if (limitForPeriod < 1) {
            throw new IllegalArgumentException("LimitForPeriod should be greater than 0");
        }
        return limitForPeriod;
    }

    @Override
    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    @Override
    public Duration getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    @Override
    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    @Override
    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }

    @Override
    public RateLimiterConfigBase withTimeoutDuration(Duration timeoutDuration) {
        return new RateLimiterConfigBase.Builder(this)
            .timeoutDuration(timeoutDuration)
            .build();
    }

    @Override
    public RateLimiterConfigBase withLimitForPeriod(int limitForPeriod) {
        return new Builder(this)
            .limitForPeriod(limitForPeriod)
            .build();
    }

    @Override
    public String toString() {
        return "RateLimiterConfig{" +
            "timeoutDuration=" + timeoutDuration +
            ", limitRefreshPeriod=" + limitRefreshPeriod +
            ", limitForPeriod=" + limitForPeriod +
            ", writableStackTraceEnabled=" + writableStackTraceEnabled +
            '}';
    }

    static class Builder implements RateLimiterConfig.Builder {

        private Duration timeoutDuration = Duration.ofSeconds(5);
        private Duration limitRefreshPeriod = Duration.ofNanos(500);
        private int limitForPeriod = 50;
        private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

        public Builder() {
        }

        public Builder(RateLimiterConfig prototype) {
            this.timeoutDuration = prototype.getTimeoutDuration();
            this.limitRefreshPeriod = prototype.getLimitRefreshPeriod();
            this.limitForPeriod = prototype.getLimitForPeriod();
            this.writableStackTraceEnabled = prototype.isWritableStackTraceEnabled();
        }

        /**
         * Builds a RateLimiterConfig
         *
         * @return the RateLimiterConfig
         */
        public RateLimiterConfigBase build() {
            return new RateLimiterConfigBase(timeoutDuration, limitRefreshPeriod, limitForPeriod,
                writableStackTraceEnabled);
        }

        /**
         * Enables writable stack traces. When set to false, {@link Exception#getStackTrace()}
         * returns a zero length array. This may be used to reduce log spam when the circuit breaker
         * is open as the cause of the exceptions is already known (the circuit breaker is
         * short-circuiting calls).
         *
         * @param writableStackTraceEnabled flag to control if stack trace is writable
         * @return the BulkheadConfig.Builder
         */
        public Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        /**
         * Configures the default wait for permission duration. Default value is 5 seconds.
         *
         * @param timeoutDuration the default wait for permission duration
         * @return the RateLimiterConfig.Builder
         */
        public Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures the period of limit refresh. After each period rate limiter sets its
         * permissions count to {@link RateLimiterConfigBase#limitForPeriod} value. Default value is 500
         * nanoseconds.
         *
         * @param limitRefreshPeriod the period of limit refresh
         * @return the RateLimiterConfig.Builder
         */
        public Builder limitRefreshPeriod(final Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = checkLimitRefreshPeriod(limitRefreshPeriod);
            return this;
        }

        /**
         * Configures the permissions limit for refresh period. Count of permissions available
         * during one rate limiter period specified by {@link RateLimiterConfigBase#limitRefreshPeriod}
         * value. Default value is 50.
         *
         * @param limitForPeriod the permissions limit for refresh period
         * @return the RateLimiterConfig.Builder
         */
        public Builder limitForPeriod(final int limitForPeriod) {
            this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
            return this;
        }
    }

}
