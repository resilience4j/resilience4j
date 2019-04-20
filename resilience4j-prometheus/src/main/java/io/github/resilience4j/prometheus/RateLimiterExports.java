/*
 *
 *  Copyright 2017 Oleksandr Goldobin
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
package io.github.resilience4j.prometheus;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.vavr.collection.Array;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * An adapter from builtin {@link RateLimiter.Metrics} to prometheus
 * {@link io.prometheus.client.CollectorRegistry}.
 *
 * @deprecated use {@link io.github.resilience4j.prometheus.collectors.RateLimiterMetricsCollector} instead.
 */
@Deprecated
public class RateLimiterExports extends Collector {
    private static final String DEFAULT_NAME = "resilience4j_ratelimiter";

    private final String name;
    private final Supplier<Iterable<RateLimiter>> rateLimitersSupplier;

    /**
     * Creates a new instance of {@link RateLimiterExports} with specified metrics names prefix and
     * {@link Supplier} of rate limiters
     *
     * @param prefix the prefix of metrics names
     * @param rateLimitersSupplier the supplier of rate limiters
     */
    public static RateLimiterExports ofSupplier(String prefix, Supplier<Iterable<RateLimiter>> rateLimitersSupplier) {
        return new RateLimiterExports(prefix, rateLimitersSupplier);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with default metrics names prefix and
     * {@link Supplier} of rate limiters
     *
     * @param rateLimitersSupplier the supplier of rate limiters
     */
    public static RateLimiterExports ofSupplier(Supplier<Iterable<RateLimiter>> rateLimitersSupplier) {
        return new RateLimiterExports(DEFAULT_NAME, rateLimitersSupplier);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with default metrics names prefix and
     * {@link RateLimiterRegistry} as a source of rate limiters.

     * @param rateLimiterRegistry the registry of rate limiters
     */
    public static RateLimiterExports ofRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterExports(rateLimiterRegistry);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with default metrics names prefix and
     * a rate limiter as a source.
     *
     * @param rateLimiter the rate limiter
     */
    public static RateLimiterExports ofRateLimiter(RateLimiter rateLimiter) {
        return new RateLimiterExports(Array.of(rateLimiter));
    }


    /**
     * Creates a new instance of {@link RateLimiterExports} with default metrics names prefix and
     * {@link Iterable} of rate limiters.
     *
     * @param rateLimiters the rate limiters
     */
    public static RateLimiterExports ofIterable(Iterable<RateLimiter> rateLimiters) {
        return new RateLimiterExports(rateLimiters);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with specified metrics names prefix and
     * {@link RateLimiterRegistry} as a source of rate limiters.
     *
     * @param prefix the prefix of metrics names
     * @param rateLimitersSupplier the registry of rate limiters
     */
    public static RateLimiterExports ofRateLimiterRegistry(String prefix, RateLimiterRegistry rateLimitersSupplier) {
        return new RateLimiterExports(prefix, rateLimitersSupplier);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with specified metrics names prefix and
     * {@link Iterable} of rate limiters.
     *
     * @param prefix the prefix of metrics names
     * @param rateLimiters the rate limiters
     */
    public static RateLimiterExports ofIterable(String prefix, Iterable<RateLimiter> rateLimiters) {
        return new RateLimiterExports(prefix, rateLimiters);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with default metrics names prefix and
     * a rate limiter as a source.
     *
     * @param prefix the prefix of metrics names
     * @param rateLimiter the rate limiter
     */
    public static RateLimiterExports ofRateLimiter(String prefix, RateLimiter rateLimiter) {
        return new RateLimiterExports(prefix, Array.of(rateLimiter));
    }
    
    /**
     * Creates a new instance of {@link RateLimiterExports} with default metric name and
     * {@link RateLimiterRegistry}.
     *
     * @param rateLimiterRegistry the rate limiter registry
     */
    private RateLimiterExports(RateLimiterRegistry rateLimiterRegistry) {
        this(rateLimiterRegistry::getAllRateLimiters);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with default metric name and
     * {@link Iterable} of rate limiters.
     *
     * @param rateLimiters the rate limiters
     */
    private RateLimiterExports(Iterable<RateLimiter> rateLimiters) {
        this(() -> rateLimiters);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with default metric name and
     * {@link Supplier} of rate limiters
     *
     * @param rateLimitersSupplier the supplier of rate limiters
     */
    private RateLimiterExports(Supplier<Iterable<RateLimiter>> rateLimitersSupplier) {
        this(DEFAULT_NAME, rateLimitersSupplier);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with specified metric name and
     * {@link RateLimiterRegistry}.
     *
     * @param name the name of metric
     * @param rateLimiterRegistry the rate limiter registry
     */
    public RateLimiterExports(String name, RateLimiterRegistry rateLimiterRegistry) {
        this(name, rateLimiterRegistry::getAllRateLimiters);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with specified metric name and
     * {@link Iterable} of rate limiters.
     *
     * @param name the name of metric
     * @param rateLimiters the rate limiters
     */
    private RateLimiterExports(String name, Iterable<RateLimiter> rateLimiters) {
        this(name, () -> rateLimiters);
    }

    /**
     * Creates a new instance of {@link RateLimiterExports} with specified metric name and
     * {@link Supplier} of rate limiters
     *
     * @param name the name of metric
     * @param rateLimitersSupplier the supplier of rate limiters
     */
    private RateLimiterExports(String name, Supplier<Iterable<RateLimiter>> rateLimitersSupplier) {
        requireNonNull(name);
        requireNonNull(rateLimitersSupplier);

        this.name = name;
        this.rateLimitersSupplier = rateLimitersSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetricFamilySamples> collect() {

        final GaugeMetricFamily stats = new GaugeMetricFamily(
                name,
                "Rate Limiter Stats",
                asList("name", "param"));

        for (RateLimiter rateLimiter : rateLimitersSupplier.get()) {

            final RateLimiter.Metrics metrics = rateLimiter.getMetrics();

            stats.addMetric(
                    asList(rateLimiter.getName(), "available_permissions"),
                    metrics.getAvailablePermissions());

            stats.addMetric(
                    asList(rateLimiter.getName(), "waiting_threads"),
                    metrics.getNumberOfWaitingThreads());
        }

        return singletonList(stats);
    }
}
