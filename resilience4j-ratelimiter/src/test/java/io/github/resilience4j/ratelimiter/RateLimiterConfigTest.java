/*
 *
 *  Copyright 2026 Robert Winkler and Bohdan Storozhuk
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

import io.github.resilience4j.core.functions.Either;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

class RateLimiterConfigTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);
    private static final Predicate<Either<? extends Throwable, ?>> DRAIN_CONDITION_CHECKER = result -> false;
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NEGATIVE = "TimeoutDuration must not be negative";
    private static final String REFRESH_PERIOD_MUST_NOT_BE_NULL = "LimitRefreshPeriod must not be null";

    @Test
    void builderPositive() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .drainPermissionsOnResult(DRAIN_CONDITION_CHECKER)
            .build();

        then(config.getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getDrainPermissionsOnResult()).isEqualTo(DRAIN_CONDITION_CHECKER);
    }

    @Test
    void builderTimeoutIsNull() {
        assertThatThrownBy(() -> RateLimiterConfig.custom().timeoutDuration(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    @Test
    void builderTimeoutIsNegative() {
        assertThatThrownBy(() -> RateLimiterConfig.custom().timeoutDuration(Duration.ofNanos(-1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(TIMEOUT_DURATION_MUST_NOT_BE_NEGATIVE);
    }

    @Test
    void builderRefreshPeriodIsNull() {
        assertThatThrownBy(() -> RateLimiterConfig.custom().limitRefreshPeriod(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(REFRESH_PERIOD_MUST_NOT_BE_NULL);
    }

    @Test
    void builderRefreshPeriodTooShort() {
        assertThatThrownBy(() -> RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(Duration.ZERO)
            .limitForPeriod(LIMIT)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RefreshPeriod is too short");
    }

    @Test
    void builderRefreshPeriodNegative() {
        assertThatThrownBy(() -> RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(Duration.ofNanos(-1))
            .limitForPeriod(LIMIT)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RefreshPeriod is too short");
    }

    @Test
    void builderLimitIsLessThanOne() {
        assertThatThrownBy(() -> RateLimiterConfig.custom().limitForPeriod(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LimitForPeriod should be greater than 0");
    }

    @Test
    void buildTimeoutDurationIsNotWithinLimits() {
        assertThatThrownBy(() -> RateLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(Long.MAX_VALUE)))
            .hasMessageContaining("TimeoutDuration too large")
            .hasCauseInstanceOf(ArithmeticException.class);
    }

    @Test
    void buildLimitRefreshPeriodIsNotWithinLimits() {
        assertThatThrownBy(() -> RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(Long.MAX_VALUE)))
            .hasMessageContaining("LimitRefreshPeriod too large")
            .hasCauseInstanceOf(ArithmeticException.class);
    }
}
