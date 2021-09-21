/*
 * Copyright 2019 Yevhenii Voievodin, Robert Winkler
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
package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiter.Metrics;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register RateLimiter exposed {@link Metrics metrics}.
 */
public class TaggedRateLimiterMetrics extends AbstractRateLimiterMetrics implements MeterBinder {

    private final RateLimiterRegistry rateLimiterRegistry;

    private TaggedRateLimiterMetrics(RateLimiterMetricNames names, RateLimiterRegistry rateLimiterRegistry) {
        super(names);
        this.rateLimiterRegistry = requireNonNull(rateLimiterRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of rate limiters.
     *
     * @param rateLimiterRegistry the source of rate limiters
     * @return The {@link TaggedRateLimiterMetrics} instance.
     */
    public static TaggedRateLimiterMetrics ofRateLimiterRegistry(
        RateLimiterRegistry rateLimiterRegistry) {
        return new TaggedRateLimiterMetrics(RateLimiterMetricNames.ofDefaults(), rateLimiterRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of rate limiters.
     *
     * @param names               custom metric names
     * @param rateLimiterRegistry the source of rate limiters
     * @return The {@link TaggedRateLimiterMetrics} instance.
     */
    public static TaggedRateLimiterMetrics ofRateLimiterRegistry(RateLimiterMetricNames names,
                                                                 RateLimiterRegistry rateLimiterRegistry) {
        return new TaggedRateLimiterMetrics(names, rateLimiterRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (RateLimiter rateLimiter : rateLimiterRegistry.getAllRateLimiters()) {
            addMetrics(registry, rateLimiter);
        }
        rateLimiterRegistry.getEventPublisher()
            .onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        rateLimiterRegistry.getEventPublisher()
            .onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        rateLimiterRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

}
