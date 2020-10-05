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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.Bulkhead.Metrics;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register Bulkhead exposed {@link Metrics metrics}.
 */
public class TaggedBulkheadMetrics extends AbstractBulkheadMetrics implements MeterBinder {

    private final BulkheadRegistry bulkheadRegistry;

    private TaggedBulkheadMetrics(BulkheadMetricNames names, BulkheadRegistry bulkheadRegistry) {
        super(names);
        this.bulkheadRegistry = requireNonNull(bulkheadRegistry);
    }

    /**
     * Creates a new binder that uses given {@code registry} as source of bulkheads.
     *
     * @param bulkheadRegistry the source of bulkheads
     * @return The {@link TaggedBulkheadMetrics} instance.
     */
    public static TaggedBulkheadMetrics ofBulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
        return new TaggedBulkheadMetrics(BulkheadMetricNames.ofDefaults(), bulkheadRegistry);
    }

    /**
     * Creates a new binder defining custom metric names and using given {@code registry} as source
     * of bulkheads.
     *
     * @param names            custom names of the metrics
     * @param bulkheadRegistry the source of bulkheads
     * @return The {@link TaggedBulkheadMetrics} instance.
     */
    public static TaggedBulkheadMetrics ofBulkheadRegistry(BulkheadMetricNames names,
                                                           BulkheadRegistry bulkheadRegistry) {
        return new TaggedBulkheadMetrics(names, bulkheadRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Bulkhead bulkhead : bulkheadRegistry.getAllBulkheads()) {
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
