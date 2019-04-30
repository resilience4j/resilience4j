/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.monitoring.health;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class RateLimiterHealthIndicator implements HealthIndicator {

    private final RateLimiter rateLimiter;
    private final long timeoutInNanos;

    public RateLimiterHealthIndicator(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        this.timeoutInNanos = rateLimiter.getRateLimiterConfig().getTimeoutDuration().toNanos();
    }

    @Override
    public Health health() {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        int availablePermissions = metrics.getAvailablePermissions();
        int numberOfWaitingThreads = metrics.getNumberOfWaitingThreads();
        if (availablePermissions > 0 || numberOfWaitingThreads == 0) {
            return rateLimiterHealth(Status.UP, availablePermissions, numberOfWaitingThreads);
        }
        if (rateLimiter instanceof AtomicRateLimiter) {
            AtomicRateLimiter atomicRateLimiter = (AtomicRateLimiter) this.rateLimiter;
            AtomicRateLimiter.AtomicRateLimiterMetrics detailedMetrics = atomicRateLimiter.getDetailedMetrics();
            if (detailedMetrics.getNanosToWait() > timeoutInNanos) {
                return rateLimiterHealth(Status.DOWN, availablePermissions, numberOfWaitingThreads);
            }
        }
        return rateLimiterHealth(Status.UNKNOWN, availablePermissions, numberOfWaitingThreads);
    }

    private Health rateLimiterHealth(Status status, int availablePermissions, int numberOfWaitingThreads) {
        return Health.status(status)
            .withDetail("availablePermissions", availablePermissions)
            .withDetail("numberOfWaitingThreads", numberOfWaitingThreads)
            .build();
    }
}