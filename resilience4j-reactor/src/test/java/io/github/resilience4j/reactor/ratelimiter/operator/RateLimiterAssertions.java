/*
 * Copyright 2018 Julien Hoarau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimiterAssertions {
    protected static final int LIMIT_FOR_PERIOD = 5;

    protected final RateLimiter rateLimiter = RateLimiter.of("test",
            RateLimiterConfig.custom().limitForPeriod(LIMIT_FOR_PERIOD).timeoutDuration(Duration.ZERO).limitRefreshPeriod(Duration.ofSeconds(10)).build());

    protected void assertUsedPermits(int used) {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        assertThat(metrics.getAvailablePermissions()).isEqualTo(LIMIT_FOR_PERIOD - used);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    protected void assertSinglePermitUsed() {
        assertUsedPermits(1);
    }

    protected void assertNoPermitLeft() {
        assertUsedPermits(LIMIT_FOR_PERIOD);
    }

    protected void saturateRateLimiter() {
        IntStream.range(0, 5).forEach(i -> assertThat(rateLimiter.getPermission(Duration.ofMillis(50))).isTrue());
    }

}
