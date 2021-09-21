/*
 * Copyright 2019 Ingyu Hwang, Mahmoud Romeh
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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

abstract class AbstractBulkheadMetrics extends AbstractMetrics {

    protected final BulkheadMetricNames names;

    protected AbstractBulkheadMetrics(BulkheadMetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, Bulkhead bulkhead) {
        List<Tag> customTags = mapToTagsList(bulkhead.getTags());
        registerMetrics(meterRegistry, bulkhead, customTags);
    }

    private void registerMetrics(
        MeterRegistry meterRegistry, Bulkhead bulkhead, List<Tag> customTags) {
        // Remove previous meters before register
        removeMetrics(meterRegistry, bulkhead.getName());

        Set<Meter.Id> idSet = new HashSet<>();
        idSet.add(Gauge.builder(names.getAvailableConcurrentCallsMetricName(), bulkhead,
            bh -> bh.getMetrics().getAvailableConcurrentCalls())
            .description("The number of available permissions")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());
        idSet.add(Gauge.builder(names.getMaxAllowedConcurrentCallsMetricName(), bulkhead,
            bh -> bh.getMetrics().getMaxAllowedConcurrentCalls())
            .description("The maximum number of available permissions")
            .tag(TagNames.NAME, bulkhead.getName())
            .tags(customTags)
            .register(meterRegistry).getId());

        meterIdMap.put(bulkhead.getName(), idSet);

    }

}
