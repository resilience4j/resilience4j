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

import static io.github.resilience4j.ratelimiter.RateLimiterConfigBase.*;

public interface RateLimiterConfig {

    /**
     * Returns a builder to create a custom RateLimiterConfig.
     *
     * @return a {@link RateLimiterConfig.Builder}
     */
    static Builder custom() {
        return new Builder();
    }

    /**
     * Returns a builder to create a custom RateLimiterConfig using specified config as prototype
     *
     * @param prototype A {@link RateLimiterConfig} prototype.
     * @return a {@link RateLimiterConfig.Builder}
     */
    static Builder from(RateLimiterConfig prototype) {
        return new Builder(prototype);
    }

    /**
     * Creates a default RateLimiter configuration.
     *
     * @return a default RateLimiter configuration.
     */
    static RateLimiterConfig ofDefaults() {
        return new Builder().build();
    }

    Duration getTimeoutDuration();

    Duration getLimitRefreshPeriod();

    int getLimitForPeriod();

    boolean isWritableStackTraceEnabled();


}
