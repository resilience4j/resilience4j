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

import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register ThreadPoolBulkheadM exposed {@link Metrics
 * metrics}.
 */
public class TaggedThreadPoolBulkheadMetrics extends AbstractThreadPoolBulkheadMetrics implements
    MeterBinder {

    private final ThreadPoolBulkheadRegistry bulkheadRegistry;

    private TaggedThreadPoolBulkheadMetrics(ThreadPoolBulkheadMetricNames names,
                                            ThreadPoolBulkheadRegistry bulkheadRegistry) {
        super(names);
        this.bulkheadRegistry = requireNonNull(bulkheadRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of bulkheads.
     *
     * @param bulkheadRegistry the source of bulkheads
     * @return The {@link TaggedThreadPoolBulkheadMetrics} instance.
     */
    public static TaggedThreadPoolBulkheadMetrics ofThreadPoolBulkheadRegistry(
        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new TaggedThreadPoolBulkheadMetrics(ThreadPoolBulkheadMetricNames.ofDefaults(), bulkheadRegistry);
    }

    /**
     * Creates a new binder defining custom metric names and using given {@code registry} as source
     * of bulkheads.
     *
     * @param names            custom names of the metrics
     * @param bulkheadRegistry the source of bulkheads
     * @return The {@link TaggedThreadPoolBulkheadMetrics} instance.
     */
    public static TaggedThreadPoolBulkheadMetrics ofThreadPoolBulkheadRegistry(ThreadPoolBulkheadMetricNames names,
                                                                               ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new TaggedThreadPoolBulkheadMetrics(names, bulkheadRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (ThreadPoolBulkhead bulkhead : bulkheadRegistry.getAllBulkheads()) {
            addMetrics(registry, bulkhead);
        }
        bulkheadRegistry.getEventPublisher()
            .onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        bulkheadRegistry.getEventPublisher()
            .onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        bulkheadRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

}
