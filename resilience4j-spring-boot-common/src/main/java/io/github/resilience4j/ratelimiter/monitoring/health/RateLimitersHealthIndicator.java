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
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;
import java.util.stream.Collectors;

public class RateLimitersHealthIndicator implements HealthIndicator {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final RateLimiterConfigurationProperties rateLimiterProperties;
    private final HealthAggregator healthAggregator;

    public RateLimitersHealthIndicator(RateLimiterRegistry rateLimiterRegistry,
                                       RateLimiterConfigurationProperties rateLimiterProperties,
                                       HealthAggregator healthAggregator) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.rateLimiterProperties = rateLimiterProperties;
        this.healthAggregator = healthAggregator;
    }

    @Override
    public Health health() {
        Map<String, Health> healths = rateLimiterRegistry.getAllRateLimiters().toJavaStream()
                .filter(this::isRegisterHealthIndicator)
                .collect(Collectors.toMap(RateLimiter::getName, this::mapRateLimiterHealth));

        return healthAggregator.aggregate(healths);
    }

    private boolean isRegisterHealthIndicator(RateLimiter rateLimiter) {
        return rateLimiterProperties.findRateLimiterProperties(rateLimiter.getName())
                .map(RateLimiterConfigurationProperties.InstanceProperties::getRegisterHealthIndicator)
                .orElse(false);
    }

    private Health mapRateLimiterHealth(RateLimiter rateLimiter) {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        int availablePermissions = metrics.getAvailablePermissions();
        int numberOfWaitingThreads = metrics.getNumberOfWaitingThreads();
        long timeoutInNanos = rateLimiter.getRateLimiterConfig().getTimeoutDuration().toNanos();

        if (availablePermissions > 0 || numberOfWaitingThreads == 0) {
            return rateLimiterHealth(Status.UP, availablePermissions, numberOfWaitingThreads);
        }
        if (rateLimiter instanceof AtomicRateLimiter) {
            AtomicRateLimiter atomicRateLimiter = (AtomicRateLimiter) rateLimiter;
            AtomicRateLimiter.AtomicRateLimiterMetrics detailedMetrics = atomicRateLimiter.getDetailedMetrics();
            if (detailedMetrics.getNanosToWait() > timeoutInNanos) {
                return rateLimiterHealth(Status.DOWN, availablePermissions, numberOfWaitingThreads);
            }
        }
        return rateLimiterHealth(Status.UNKNOWN, availablePermissions, numberOfWaitingThreads);
    }

    private static Health rateLimiterHealth(Status status, int availablePermissions, int numberOfWaitingThreads) {
        return Health.status(status)
            .withDetail("availablePermissions", availablePermissions)
            .withDetail("numberOfWaitingThreads", numberOfWaitingThreads)
            .build();
    }
}
