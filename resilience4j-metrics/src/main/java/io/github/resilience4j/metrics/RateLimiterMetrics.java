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
import io.vavr.collection.Array;

import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link RateLimiter.Metrics} as Dropwizard Metrics Gauges.
 */
public class RateLimiterMetrics implements MetricSet {

    private static final String PREFIX_NULL = "Prefix must not be null";
    private static final String ITERABLE_NULL = "RateLimiters iterable must not be null";

    private static final String DEFAULT_PREFIX = "resilience4j.ratelimiter";

    private final MetricRegistry metricRegistry = new MetricRegistry();

    private RateLimiterMetrics(Iterable<RateLimiter> rateLimiters) {
        this(DEFAULT_PREFIX, rateLimiters);
    }

    private RateLimiterMetrics(String prefix, Iterable<RateLimiter> rateLimiters) {
        requireNonNull(prefix, PREFIX_NULL);
        requireNonNull(rateLimiters, ITERABLE_NULL);

        rateLimiters.forEach(rateLimiter -> {
                String name = rateLimiter.getName();
                RateLimiter.Metrics metrics = rateLimiter.getMetrics();

                metricRegistry.register(name(prefix, name, "number_of_waiting_threads"),
                    (Gauge<Integer>) metrics::getNumberOfWaitingThreads);
                metricRegistry.register(name(prefix, name, "available_permissions"),
                    (Gauge<Integer>) metrics::getAvailablePermissions);
            }
        );
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with specified metrics names prefix and
     * a {@link RateLimiterRegistry} as a source.
     *
     * @param prefix              the prefix of metrics names
     * @param rateLimiterRegistry the registry of rate limiters
     */
    public static RateLimiterMetrics ofRateLimiterRegistry(String prefix, RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterMetrics(prefix, rateLimiterRegistry.getAllRateLimiters());
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with
     * a {@link RateLimiterRegistry} as a source.
     *
     * @param rateLimiterRegistry the registry of rate limiters
     */
    public static RateLimiterMetrics ofRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterMetrics(rateLimiterRegistry.getAllRateLimiters());
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with
     * an {@link Iterable} of rate limiters as a source.
     *
     * @param rateLimiters the rate limiters
     */
    public static RateLimiterMetrics ofIterable(Iterable<RateLimiter> rateLimiters) {
        return new RateLimiterMetrics(rateLimiters);
    }

    /**
     * Creates a new instance {@link RateLimiterMetrics} with
     * an {@link Iterable} of rate limiters as a source.
     *
     * @param rateLimiters the rate limiters
     */
    public static RateLimiterMetrics ofIterable(String prefix, Iterable<RateLimiter> rateLimiters) {
        return new RateLimiterMetrics(prefix, rateLimiters);
    }


    /**
     * Creates a new instance of {@link RateLimiterMetrics} with a rate limiter as a source.
     *
     * @param rateLimiter the rate limiter
     */
    public static RateLimiterMetrics ofRateLimiter(RateLimiter rateLimiter) {
        return new RateLimiterMetrics(Array.of(rateLimiter));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
