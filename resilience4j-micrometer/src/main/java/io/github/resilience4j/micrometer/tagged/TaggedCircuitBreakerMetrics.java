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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register CircuitBreaker exposed {@link Metrics metrics}.
 */
public class TaggedCircuitBreakerMetrics extends AbstractCircuitBreakerMetrics implements
    MeterBinder {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private TaggedCircuitBreakerMetrics(CircuitBreakerMetricNames names,
                                        CircuitBreakerRegistry circuitBreakerRegistry) {
        super(names);
        this.circuitBreakerRegistry = requireNonNull(circuitBreakerRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of circuit breakers.
     *
     * @param circuitBreakerRegistry the source of circuit breakers
     * @return The {@link TaggedCircuitBreakerMetrics} instance.
     */
    public static TaggedCircuitBreakerMetrics ofCircuitBreakerRegistry(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new TaggedCircuitBreakerMetrics(CircuitBreakerMetricNames.ofDefaults(), circuitBreakerRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of circuit breakers.
     *
     * @param circuitBreakerMetricNames            custom metric names
     * @param circuitBreakerRegistry the source of circuit breakers
     * @return The {@link TaggedCircuitBreakerMetrics} instance.
     */
    public static TaggedCircuitBreakerMetrics ofCircuitBreakerRegistry(CircuitBreakerMetricNames circuitBreakerMetricNames,
                                                                       CircuitBreakerRegistry circuitBreakerRegistry) {
        return new TaggedCircuitBreakerMetrics(circuitBreakerMetricNames, circuitBreakerRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            addMetrics(registry, circuitBreaker);
        }
        circuitBreakerRegistry.getEventPublisher()
            .onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        circuitBreakerRegistry.getEventPublisher()
            .onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        circuitBreakerRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

}
