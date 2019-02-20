/*
 * Copyright 2018 Julien Hoarau
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
package io.github.resilience4j.micrometer;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static io.github.resilience4j.bulkhead.utils.MetricNames.AVAILABLE_CONCURRENT_CALLS;
import static io.github.resilience4j.bulkhead.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.bulkhead.utils.MetricNames.MAX_ALLOWED_CONCURRENT_CALLS;
import static io.github.resilience4j.micrometer.MetricUtils.getName;
import static java.util.Objects.requireNonNull;

public class BulkheadMetrics implements MeterBinder {

    private final Iterable<Bulkhead> bulkheads;
    private final String prefix;

    private BulkheadMetrics(Iterable<Bulkhead> bulkheads) {
        this(bulkheads, DEFAULT_PREFIX);
    }

    private BulkheadMetrics(Iterable<Bulkhead> bulkheads, String prefix) {
        this.bulkheads = requireNonNull(bulkheads);
        this.prefix = requireNonNull(prefix);
    }

    /**
     * Creates a new instance BulkheadMetrics {@link BulkheadMetrics} with
     * a {@link BulkheadRegistry} as a source.
     *
     * @param bulkheadRegistry the registry of bulkheads
     */
    public static BulkheadMetrics ofBulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
        return new BulkheadMetrics(bulkheadRegistry.getAllBulkheads());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Bulkhead bulkhead : bulkheads) {
            final String name = bulkhead.getName();
            Gauge.builder(getName(prefix, name, AVAILABLE_CONCURRENT_CALLS), bulkhead, (cb) -> cb.getMetrics().getAvailableConcurrentCalls())
                    .register(registry);
            Gauge.builder(getName(prefix, name, MAX_ALLOWED_CONCURRENT_CALLS), bulkhead, (bh) -> bh.getMetrics().getMaxAllowedConcurrentCalls())
                    .register(registry);
        }
    }
}
