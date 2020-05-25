/*
 *
 *  Copyright 2018 Gkatziouras Emmanouil
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.ratelimiter;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class SmoothRateLimiterConfig {

    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String LIMIT_REFILL_PERIOD_MUST_NOT_BE_NULL = "LimitRefillPeriod must not be null";
    private static final Duration ACCEPTABLE_REFRESH_PERIOD = Duration.ofNanos(1L);
    private static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;

    private final Duration timeoutDuration;
    private final Duration limitRefillPeriod;
    private final int limitForPeriod;
    private final int burstCapacity;
    private final boolean writableStackTraceEnabled;

    private SmoothRateLimiterConfig(Duration timeoutDuration, Duration limitRefillPeriod,
                                    int limitForPeriod, int burstCapacity, boolean writableStackTraceEnabled) {
        this.timeoutDuration = timeoutDuration;
        this.limitRefillPeriod = limitRefillPeriod;
        this.limitForPeriod = limitForPeriod;
        this.burstCapacity = burstCapacity;
        this.writableStackTraceEnabled = writableStackTraceEnabled;
    }

    /**
     * Returns a builder to create a custom SmoothRateLimiterConfig.
     *
     * @return a {@link SmoothRateLimiterConfig.Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Returns a builder to create a custom SmoothRateLimiterConfig using specified config as prototype
     *
     * @param prototype A {@link SmoothRateLimiterConfig} prototype.
     * @return a {@link SmoothRateLimiterConfig.Builder}
     */
    public static Builder from(SmoothRateLimiterConfig prototype) {
        return new Builder(prototype);
    }

    /**
     * Creates a default SmoothRefillRateLimiter configuration.
     *
     * @return a default SmoothRefillRateLimiter configuration.
     */
    public static SmoothRateLimiterConfig ofDefaults() {
        return new Builder().build();
    }

    private static Duration checkTimeoutDuration(final Duration timeoutDuration) {
        return requireNonNull(timeoutDuration, TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    private static Duration checkLimitRefreshPeriod(Duration limitRefillPeriod) {
        requireNonNull(limitRefillPeriod, LIMIT_REFILL_PERIOD_MUST_NOT_BE_NULL);
        boolean refillPeriodIsTooShort =
            limitRefillPeriod.compareTo(ACCEPTABLE_REFRESH_PERIOD) < 0;
        if (refillPeriodIsTooShort) {
            throw new IllegalArgumentException("LimitRefillPeriod is too short");
        }
        return limitRefillPeriod;
    }

    private static int checkLimitForPeriod(final int limitForPeriod) {
        if (limitForPeriod < 1) {
            throw new IllegalArgumentException("LimitForPeriod should be greater than 0");
        }
        return limitForPeriod;
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public Duration getLimitRefillPeriod() {
        return limitRefillPeriod;
    }

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public int getBurstCapacity() {
        return burstCapacity;
    }

    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }

    @Override
    public String toString() {
        return "RateLimiterConfig{" +
            "timeoutDuration=" + timeoutDuration +
            ", limitRefillPeriod=" + limitRefillPeriod+
            ", limitForPeriod=" + limitForPeriod +
            ", burstCapacity=" + burstCapacity +
            ", writableStackTraceEnabled=" + writableStackTraceEnabled +
            '}';
    }

    public static class Builder {

        private Duration timeoutDuration = Duration.ofSeconds(5);;
        private Duration limitRefillPeriod = Duration.ofNanos(500);
        private int limitForPeriod = 50;
        private int burstCapacity = 0;
        private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

        public Builder() {
        }

        public Builder(SmoothRateLimiterConfig prototype) {
            this.timeoutDuration = prototype.timeoutDuration;
            this.limitRefillPeriod = prototype.limitRefillPeriod;
            this.limitForPeriod = prototype.limitForPeriod;
            this.burstCapacity = prototype.burstCapacity;
            this.writableStackTraceEnabled = prototype.writableStackTraceEnabled;
        }

        /**
         * Builds a SmoothRateLimiterConfig
         *
         * @return the SmoothRateLimiterConfig
        */
        public SmoothRateLimiterConfig build() {
            if(burstCapacity < limitForPeriod) {
                burstCapacity = limitForPeriod;
            }
            return new SmoothRateLimiterConfig(timeoutDuration, limitRefillPeriod, limitForPeriod, burstCapacity,
                writableStackTraceEnabled);
        }

        /**
         * Enables writable stack traces. When set to false, {@link Exception#getStackTrace()}
         * returns a zero length array. This may be used to reduce log spam when the circuit breaker
         * is open as the cause of the exceptions is already known (the circuit breaker is
         * short-circuiting calls).
         *
         * @param writableStackTraceEnabled flag to control if stack trace is writable
         * @return the SmoothRateLimiterConfig.Builder
         */
        public Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        /**
         * Configures the default wait for permission duration. Default value is 5 seconds.
         *
         * @param timeoutDuration the default wait for permission duration
         * @return the SmoothRateLimiterConfig.Builder
         */
        public Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures the period of limit refresh. After each period rate limiter sets its
         * permissions count to {@link SmoothRateLimiterConfig#limitForPeriod} value. Default value is 500
         * nanoseconds.
         *
         * @param limitRefillPeriod the period of limit refresh
         * @return the RateLimiterConfig.Builder
         */
        public Builder limitRefillPeriod(final Duration limitRefillPeriod) {
            this.limitRefillPeriod = checkLimitRefreshPeriod(limitRefillPeriod);
            return this;
        }

        /**
         * Configures the permissions limit for refresh period. Count of permissions available
         * during one rate limiter period specified by {@link SmoothRateLimiterConfig#limitRefillPeriod}
         * value. Default value is 50.
         *
         * @param limitForPeriod the permission refill limit for period
         * @return the SmoothRateLimiterConfig.Builder
         */
        public Builder limitForPeriod(final int limitForPeriod) {
            this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
            return this;
        }

        /**
         * Configures the permissions limit for burst capacity. Count of max permissions available
         * during one rate limiter period specified by {@link SmoothRateLimiterConfig#limitRefillPeriod}
         * value. If no value specified the default value is the one
         * specified for @{@link SmoothRateLimiterConfig#limitForPeriod}.
         *
         * @param burstCapacity the max permissions limit for the refresh period
         * @return the SmoothRateLimiterConfig.Builder
         */
        public Builder burstCapacity(final int burstCapacity) {
            this.burstCapacity = burstCapacity;
            return this;
        }

    }

}
