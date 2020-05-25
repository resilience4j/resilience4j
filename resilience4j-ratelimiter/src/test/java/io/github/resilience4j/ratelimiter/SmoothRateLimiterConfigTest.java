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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;


public class SmoothRateLimiterConfigTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFILL_PERIOD = Duration.ofNanos(500);
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String REFILL_PERIOD_MUST_NOT_BE_NULL = "RefillPeriod must not be null";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void builderPositive() throws Exception {
        SmoothRateLimiterConfig config = SmoothRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefillPeriod(REFILL_PERIOD)
            .limitForPeriod(LIMIT)
            .build();

        then(config.getLimitForPeriod()).isEqualTo(LIMIT);
        then(config.getLimitRefillPeriod()).isEqualTo(REFILL_PERIOD);
        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.getBurstCapacity()).isEqualTo(LIMIT);
    }

    @Test
    public void builderTimeoutIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);
        SmoothRateLimiterConfig.custom()
            .timeoutDuration(null);
    }

    @Test
    public void builderRefreshPeriodIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(REFILL_PERIOD_MUST_NOT_BE_NULL);
        SmoothRateLimiterConfig.custom()
            .limitRefillPeriod(null);
    }

    @Test
    public void builderRefreshPeriodTooShort() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("LimitRefillPeriod is too short");
        SmoothRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefillPeriod(Duration.ZERO)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void builderLimitIsLessThanOne() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("LimitForPeriod should be greater than 0");
        SmoothRateLimiterConfig.custom()
            .limitForPeriod(0);
    }

}