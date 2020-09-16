/*
 *
 *  Copyright 2017: Bohdan Storozhuk
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
package io.github.resilience4j.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.util.List;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link RateLimiter.Metrics} as Dropwizard Metrics Gauges.
 */
public class RateLimiterMetrics implements MetricSet {

    private static final String PREFIX_NULL = "Prefix must not be null";
    private static final String ITERABLE_NULL = "RateLimiters iterable must not be null";

    private final MetricRegistry metricRegistry;

    private RateLimiterMetrics(Iterable<RateLimiter> rateLimiters) {
        this(DEFAULT_PREFIX, rateLimiters, new MetricRegistry());
    }

    private RateLimiterMetrics(String prefix, Iterable<RateLimiter> rateLimiters,
        MetricRegistry metricRegistry) {
        requireNonNull(prefix, PREFIX_NULL);
        requireNonNull(rateLimiters, ITERABLE_NULL);
        requireNonNull(metricRegistry);
        this.metricRegistry = metricRegistry;
        rateLimiters.forEach(rateLimiter -> {
                String name = rateLimiter.getName();
                metricRegistry.register(name(prefix, name, WAITING_THREADS),
                    (Gauge<Integer>) rateLimiter.getMetrics()::getNumberOfWaitingThreads);
                metricRegistry.register(name(prefix, name, AVAILABLE_PERMISSIONS),
                    (Gauge<Integer>) rateLimiter.getMetrics()::getAvailablePermissions);
            }
        );
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with specified metrics names prefix and a
     * {@link RateLimiterRegistry} as a source.
     *
     * @param prefix              the prefix of metrics names
     * @param rateLimiterRegistry the registry of rate limiters
     * @param metricRegistry      the metric registry
     */
    public static RateLimiterMetrics ofRateLimiterRegistry(String prefix,
        RateLimiterRegistry rateLimiterRegistry, MetricRegistry metricRegistry) {
        return new RateLimiterMetrics(prefix, rateLimiterRegistry.getAllRateLimiters(),
            metricRegistry);
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with specified metrics names prefix and a
     * {@link RateLimiterRegistry} as a source.
     *
     * @param prefix              the prefix of metrics names
     * @param rateLimiterRegistry the registry of rate limiters
     */
    public static RateLimiterMetrics ofRateLimiterRegistry(String prefix,
        RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterMetrics(prefix, rateLimiterRegistry.getAllRateLimiters(),
            new MetricRegistry());
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with a {@link RateLimiterRegistry} as a
     * source.
     *
     * @param rateLimiterRegistry the registry of rate limiters
     * @param metricRegistry      the metric registry
     */
    public static RateLimiterMetrics ofRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry,
        MetricRegistry metricRegistry) {
        return new RateLimiterMetrics(DEFAULT_PREFIX, rateLimiterRegistry.getAllRateLimiters(),
            metricRegistry);
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with a {@link RateLimiterRegistry} as a
     * source.
     *
     * @param rateLimiterRegistry the registry of rate limiters
     */
    public static RateLimiterMetrics ofRateLimiterRegistry(
        RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterMetrics(rateLimiterRegistry.getAllRateLimiters());
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with an {@link Iterable} of rate limiters
     * as a source.
     *
     * @param rateLimiters the rate limiters
     */
    public static RateLimiterMetrics ofIterable(Iterable<RateLimiter> rateLimiters) {
        return new RateLimiterMetrics(rateLimiters);
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with an {@link Iterable} of rate limiters
     * as a source.
     *
     * @param rateLimiters the rate limiters
     */
    public static RateLimiterMetrics ofIterable(String prefix, Iterable<RateLimiter> rateLimiters) {
        return new RateLimiterMetrics(prefix, rateLimiters, new MetricRegistry());
    }


    /**
     * Creates a new instance of {@link RateLimiterMetrics} with a rate limiter as a source.
     *
     * @param rateLimiter the rate limiter
     */
    public static RateLimiterMetrics ofRateLimiter(RateLimiter rateLimiter) {
        return new RateLimiterMetrics(List.of(rateLimiter));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
