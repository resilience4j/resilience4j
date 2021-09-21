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
import org.springframework.boot.actuate.health.*;

import java.util.Map;
import java.util.stream.Collectors;

import static io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties.InstanceProperties;

public class RateLimitersHealthIndicator implements HealthIndicator {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final RateLimiterConfigurationProperties rateLimiterProperties;
    private final StatusAggregator statusAggregator;

    public RateLimitersHealthIndicator(RateLimiterRegistry rateLimiterRegistry,
        RateLimiterConfigurationProperties rateLimiterProperties,
        StatusAggregator statusAggregator) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.rateLimiterProperties = rateLimiterProperties;
        this.statusAggregator = statusAggregator;
    }

    private static Health rateLimiterHealth(Status status, int availablePermissions,
        int numberOfWaitingThreads) {
        return Health.status(status)
            .withDetail("availablePermissions", availablePermissions)
            .withDetail("numberOfWaitingThreads", numberOfWaitingThreads)
            .build();
    }

    @Override
    public Health health() {
        Map<String, Health> healths = rateLimiterRegistry.getAllRateLimiters().stream()
            .filter(this::isRegisterHealthIndicator)
            .collect(Collectors.toMap(RateLimiter::getName, this::mapRateLimiterHealth));

        Status status = statusAggregator.getAggregateStatus(healths.values().stream().map(Health::getStatus).collect(Collectors.toSet()));
        return Health.status(status).withDetails(healths).build();
    }

    private boolean isRegisterHealthIndicator(RateLimiter rateLimiter) {
        return rateLimiterProperties.findRateLimiterProperties(rateLimiter.getName())
            .map(InstanceProperties::getRegisterHealthIndicator)
            .orElse(false);
    }

    private boolean allowHealthIndicatorToFail(RateLimiter rateLimiter) {
        return rateLimiterProperties.findRateLimiterProperties(rateLimiter.getName())
                .map(InstanceProperties::getAllowHealthIndicatorToFail)
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
            AtomicRateLimiter.AtomicRateLimiterMetrics detailedMetrics = atomicRateLimiter
                .getDetailedMetrics();
            if (detailedMetrics.getNanosToWait() > timeoutInNanos) {
                boolean allowHealthIndicatorToFail = allowHealthIndicatorToFail(rateLimiter);

                return rateLimiterHealth(allowHealthIndicatorToFail ? Status.DOWN : new Status("RATE_LIMITED"), availablePermissions, numberOfWaitingThreads);
            }
        }
        return rateLimiterHealth(Status.UNKNOWN, availablePermissions, numberOfWaitingThreads);
    }
}
