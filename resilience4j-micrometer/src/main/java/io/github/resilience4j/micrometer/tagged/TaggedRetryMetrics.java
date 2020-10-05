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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static io.github.resilience4j.retry.Retry.Metrics;
import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register Retry exposed {@link Metrics metrics}.
 */
public class TaggedRetryMetrics extends AbstractRetryMetrics implements MeterBinder {

    private final RetryRegistry retryRegistry;

    private TaggedRetryMetrics(RetryMetricNames names, RetryRegistry retryRegistry) {
        super(names);
        this.retryRegistry = requireNonNull(retryRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param retryRegistry the source of retries
     * @return The {@link TaggedRetryMetrics} instance.
     */
    public static TaggedRetryMetrics ofRetryRegistry(RetryRegistry retryRegistry) {
        return new TaggedRetryMetrics(RetryMetricNames.ofDefaults(), retryRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of retries.
     *
     * @param names         custom metric names
     * @param retryRegistry the source of retries
     * @return The {@link TaggedRetryMetrics} instance.
     */
    public static TaggedRetryMetrics ofRetryRegistry(RetryMetricNames names,
                                                     RetryRegistry retryRegistry) {
        return new TaggedRetryMetrics(names, retryRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Retry retry : retryRegistry.getAllRetries()) {
            addMetrics(registry, retry);
        }
        retryRegistry.getEventPublisher()
            .onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        retryRegistry.getEventPublisher()
            .onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        retryRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

}
