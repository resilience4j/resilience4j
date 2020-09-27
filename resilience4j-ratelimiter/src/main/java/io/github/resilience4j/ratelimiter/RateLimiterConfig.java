/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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

public interface RateLimiterConfig {

    /**
     * Returns a builder to create a custom RateLimiterConfig.
     *
     * @return a {@link RateLimiterConfig.Builder}
     */
    static Builder custom() {
        return new RateLimiterConfigBase.Builder();
    }

    /**
     * Returns a builder to create a custom RateLimiterConfig using specified config as prototype
     *
     * @param prototype A {@link RateLimiterConfig} prototype.
     * @return a {@link RateLimiterConfig.Builder}
     */
    static Builder from(RateLimiterConfig prototype) {
        return new RateLimiterConfigBase.Builder(prototype);
    }

    /**
     * Creates a default RateLimiter configuration.
     *
     * @return a default RateLimiter configuration.
     */
    static RateLimiterConfig ofDefaults() {
        return new RateLimiterConfigBase.Builder().build();
    }

    Duration getTimeoutDuration();

    Duration getLimitRefreshPeriod();

    int getLimitForPeriod();

    boolean isWritableStackTraceEnabled();

    <T extends RateLimiterConfig> T withTimeoutDuration(Duration timeoutDuration);

    <T extends RateLimiterConfig> T withLimitForPeriod(int limitForPeriod);

    interface Builder {

        /**
         * Builds a RateLimiterConfig
         *
         * @return the RateLimiterConfig
         */
        RateLimiterConfig build();

        /**
         * Enables writable stack traces. When set to false, {@link Exception#getStackTrace()}
         * returns a zero length array. This may be used to reduce log spam when the circuit breaker
         * is open as the cause of the exceptions is already known (the circuit breaker is
         * short-circuiting calls).
         *
         * @param writableStackTraceEnabled flag to control if stack trace is writable
         * @return the BulkheadConfig.Builder
         */
        Builder writableStackTraceEnabled(boolean writableStackTraceEnabled);

        /**
         * Configures the default wait for permission duration. Default value is 5 seconds.
         *
         * @param timeoutDuration the default wait for permission duration
         * @return the RateLimiterConfig.Builder
         */
        Builder timeoutDuration(final Duration timeoutDuration);

        /**
         * Configures the period of limit refresh. After each period rate limiter sets its
         * permissions count to {@link RateLimiterConfigBase#limitForPeriod} value. Default value is 500
         * nanoseconds.
         *
         * @param limitRefreshPeriod the period of limit refresh
         * @return the RateLimiterConfig.Builder
         */
        Builder limitRefreshPeriod(final Duration limitRefreshPeriod);


        /**
         * Configures the permissions limit for refresh period. Count of permissions available
         * during one rate limiter period specified by {@link RateLimiterConfigBase#limitRefreshPeriod}
         * value. Default value is 50.
         *
         * @param limitForPeriod the permissions limit for refresh period
         * @return the RateLimiterConfig.Builder
         */
        Builder limitForPeriod(final int limitForPeriod);

    }

}
