/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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

import io.github.resilience4j.ratelimiter.internal.RefillRateLimiter;

import java.time.Duration;

/**
 * {@link RefillRateLimiter} is a permission rate based Rate Limiter.
 * Instead of resetting permits based on a permission period the permission release is based on a rate.
 * Therefore {@link RefillRateLimiterConfig#nanosPerPermission} is used which is a product of the division
 * of {@link RateLimiterConfig#limitRefreshPeriod} to {@link RateLimiterConfig#limitForPeriod}.
 */
public class RefillRateLimiterConfig extends RateLimiterConfig {

    private static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;

    private final int permitCapacity;
    private final int initialPermits;
    private final long nanosPerPermission;

    private RefillRateLimiterConfig(Duration timeoutDuration, int permitCapacity, long nanosPerPermission,
                                    int initialPermits, boolean writableStackTraceEnabled) {
        super(timeoutDuration, Duration.ofNanos(nanosPerPermission*permitCapacity), permitCapacity, writableStackTraceEnabled);
        this.permitCapacity = permitCapacity;
        this.initialPermits = initialPermits;
        this.nanosPerPermission = nanosPerPermission;
    }

    /**
     * Returns a builder to create a custom RefillRateLimiterConfig.
     *
     * @return a {@link RefillRateLimiterConfig.Builder}
     */
    public static RefillRateLimiterConfig.Builder custom() {
        return new RefillRateLimiterConfig.Builder();
    }

    /**
     * Returns a builder to create a custom RefillRateLimiterConfig using specified config as prototype
     *
     * @param prototype A {@link RefillRateLimiterConfig} prototype.
     * @return a {@link RefillRateLimiterConfig.Builder}
     */
    public static RefillRateLimiterConfig.Builder from(RefillRateLimiterConfig prototype) {
        return new RefillRateLimiterConfig.Builder(prototype);
    }

    /**
     * Creates a default RateLimiter configuration.
     *
     * @return a default RateLimiter configuration.
     */
    public static RefillRateLimiterConfig ofDefaults() {
        return new RefillRateLimiterConfig.Builder().build();
    }

    public int getPermitCapacity() {
        return permitCapacity;
    }

    public int getInitialPermits() {
        return initialPermits;
    }

    @Override
    public String toString() {
        return "RefillRateLimiterConfig{" +
            "timeoutDuration=" + getTimeoutDuration() +
            ", permitCapacity=" + permitCapacity +
            ", nanosPerPermission="+ nanosPerPermission +
            ", writableStackTraceEnabled=" + isWritableStackTraceEnabled() +
            '}';
    }

    public static class Builder extends RateLimiterConfig.Builder {

        private Duration timeoutDuration = Duration.ofSeconds(5);
        private Duration limitRefreshPeriod = Duration.ofNanos(500);
        private int limitForPeriod = 50;
        private int permitCapacity = 0;
        private int initialPermits = 0;
        private boolean initialPermitsSet;
        private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

        public Builder() {
        }

        public Builder(RefillRateLimiterConfig prototype) {
            this.timeoutDuration = prototype.getTimeoutDuration();
            this.limitRefreshPeriod = Duration.ofNanos(prototype.nanosPerPermission);
            this.limitForPeriod = 1;
            this.permitCapacity = prototype.permitCapacity;
            this.writableStackTraceEnabled = prototype.isWritableStackTraceEnabled();
        }

        /**
         * Builds a RefillRateLimiterConfig
         *
         * @return the RefillRateLimiterConfig
         */
        @Override
        public RefillRateLimiterConfig build() {
            if(permitCapacity < limitForPeriod) {
                permitCapacity = limitForPeriod;
            }

            if(!initialPermitsSet) {
                initialPermits = limitForPeriod;
            }

            final long nanosPerPermission = calculateNanosPerPermission(limitRefreshPeriod, limitForPeriod);

            return new RefillRateLimiterConfig(timeoutDuration, permitCapacity, nanosPerPermission,
                initialPermits ,writableStackTraceEnabled);
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
        @Override
        public RefillRateLimiterConfig.Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        /**
         * Configures the default wait for permission duration. Default value is 5 seconds.
         *
         * @param timeoutDuration the default wait for permission duration
         * @return the RateLimiterConfig.Builder
         */
        @Override
        public RefillRateLimiterConfig.Builder timeoutDuration(final Duration timeoutDuration) {
            this.timeoutDuration = checkTimeoutDuration(timeoutDuration);
            return this;
        }

        /**
         * Configures the period needed for the permissions specified. After each period
         * permissions up to {@link RefillRateLimiterConfig.Builder#limitForPeriod} should be released.
         * Default value is 500 nanoseconds.
         *
         * @param limitRefreshPeriod the period of limit refresh
         * @return the RefillRateLimiterConfig.Builder
         */
        @Override
        public RefillRateLimiterConfig.Builder limitRefreshPeriod(final Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = checkLimitRefreshPeriod(limitRefreshPeriod);
            return this;
        }

        /**
         * Configures the permissions limit for refresh period. Count of permissions released
         * during one rate limiter period specified by {@link RefillRateLimiterConfig.Builder#limitRefreshPeriod}
         * value. Default value is 50.
         *
         * @param limitForPeriod the permissions limit for refresh period
         * @return the RefillRateLimiterConfig.Builder
         */
        @Override
        public RefillRateLimiterConfig.Builder limitForPeriod(final int limitForPeriod) {
            this.limitForPeriod = checkLimitForPeriod(limitForPeriod);
            return this;
        }

        /**
         * Configures the permissions capacity. Count of max permissions available
         * If no value specified the default value is the one
         * specified for {@link RefillRateLimiterConfig.Builder#limitForPeriod}.
         *
         * @param permitCapacity the capacity of permissions
         * @return the RateLimiterConfig.Builder
         */
        public RefillRateLimiterConfig.Builder permitCapacity(final int permitCapacity) {
            this.permitCapacity = permitCapacity;
            return this;
        }

        /**
         * Configures the initial permissions available.
         * If no value specified the default value is the one
         * specified for {@link RefillRateLimiterConfig.Builder#limitForPeriod}.
         *
         * @param initialPermits the initial permits
         * @return the RateLimiterConfig.Builder
         */
        public RefillRateLimiterConfig.Builder initialPermits(final int initialPermits) {
            this.initialPermits = initialPermits;
            this.initialPermitsSet = true;
            return this;
        }

        /**
         * Calculate the nanos needed for one permission
         * @param limitRefreshPeriod
         * @param limitForPeriod
         * @return
         */
        private long calculateNanosPerPermission(Duration limitRefreshPeriod, int limitForPeriod) {
            long permissionsPeriodInNanos = limitRefreshPeriod.toNanos();
            return permissionsPeriodInNanos/limitForPeriod;
        }

    }
}
