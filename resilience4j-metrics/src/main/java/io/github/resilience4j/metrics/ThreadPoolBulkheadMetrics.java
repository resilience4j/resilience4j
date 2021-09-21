/*
 * Copyright 2017 Jan Sykora
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
package io.github.resilience4j.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;

import java.util.List;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.bulkhead.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

/**
 * An adapter which exports {@link Bulkhead.Metrics} as Dropwizard Metrics Gauges.
 */
public class ThreadPoolBulkheadMetrics implements MetricSet {

    private final MetricRegistry metricRegistry;

    private ThreadPoolBulkheadMetrics(Iterable<ThreadPoolBulkhead> bulkheads) {
        this(DEFAULT_PREFIX_THREAD_POOL, bulkheads, new MetricRegistry());
    }

    private ThreadPoolBulkheadMetrics(String prefix, Iterable<ThreadPoolBulkhead> bulkheads,
        MetricRegistry metricRegistry) {
        requireNonNull(prefix);
        requireNonNull(bulkheads);
        requireNonNull(metricRegistry);
        this.metricRegistry = metricRegistry;
        bulkheads.forEach(bulkhead -> {
            String name = bulkhead.getName();
            //number of available concurrent calls as an integer
            metricRegistry.register(name(prefix, name, CURRENT_THREAD_POOL_SIZE),
                (Gauge<Integer>) () -> bulkhead.getMetrics().getThreadPoolSize());
            metricRegistry.register(name(prefix, name, AVAILABLE_QUEUE_CAPACITY),
                (Gauge<Integer>) () -> bulkhead.getMetrics().getRemainingQueueCapacity());
        });
    }

    /**
     * Creates a new instance BulkheadMetrics {@link ThreadPoolBulkheadMetrics} with specified
     * metrics names prefix and a {@link BulkheadRegistry} as a source.
     *
     * @param prefix           the prefix of metrics names
     * @param bulkheadRegistry the registry of bulkheads
     * @param metricRegistry   the metric registry
     */
    public static ThreadPoolBulkheadMetrics ofBulkheadRegistry(String prefix,
        ThreadPoolBulkheadRegistry bulkheadRegistry, MetricRegistry metricRegistry) {
        return new ThreadPoolBulkheadMetrics(prefix, bulkheadRegistry.getAllBulkheads(),
            metricRegistry);
    }

    /**
     * Creates a new instance BulkheadMetrics {@link ThreadPoolBulkheadMetrics} with specified
     * metrics names prefix and a {@link BulkheadRegistry} as a source.
     *
     * @param prefix           the prefix of metrics names
     * @param bulkheadRegistry the registry of bulkheads
     */
    public static ThreadPoolBulkheadMetrics ofBulkheadRegistry(String prefix,
        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new ThreadPoolBulkheadMetrics(prefix, bulkheadRegistry.getAllBulkheads(),
            new MetricRegistry());
    }

    /**
     * Creates a new instance BulkheadMetrics {@link ThreadPoolBulkheadMetrics} with a {@link
     * BulkheadRegistry} as a source.
     *
     * @param bulkheadRegistry the registry of bulkheads
     */
    public static ThreadPoolBulkheadMetrics ofBulkheadRegistry(
        ThreadPoolBulkheadRegistry bulkheadRegistry, MetricRegistry metricRegistry) {
        return new ThreadPoolBulkheadMetrics(DEFAULT_PREFIX, bulkheadRegistry.getAllBulkheads(),
            metricRegistry);
    }

    /**
     * Creates a new instance BulkheadMetrics {@link ThreadPoolBulkheadMetrics} with a {@link
     * BulkheadRegistry} as a source.
     *
     * @param bulkheadRegistry the registry of bulkheads
     */
    public static ThreadPoolBulkheadMetrics ofBulkheadRegistry(
        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new ThreadPoolBulkheadMetrics(bulkheadRegistry.getAllBulkheads());
    }

    /**
     * Creates a new instance BulkheadMetrics {@link ThreadPoolBulkheadMetrics} with an {@link
     * Iterable} of bulkheads as a source.
     *
     * @param bulkheads the bulkheads
     */
    public static ThreadPoolBulkheadMetrics ofIterable(Iterable<ThreadPoolBulkhead> bulkheads) {
        return new ThreadPoolBulkheadMetrics(bulkheads);
    }

    /**
     * Creates a new instance BulkheadMetrics {@link ThreadPoolBulkheadMetrics} with an {@link
     * Iterable} of bulkheads as a source.
     *
     * @param bulkheads the bulkheads
     */
    public static ThreadPoolBulkheadMetrics ofIterable(String prefix,
        Iterable<ThreadPoolBulkhead> bulkheads) {
        return new ThreadPoolBulkheadMetrics(prefix, bulkheads, new MetricRegistry());
    }


    /**
     * Creates a new instance of BulkheadMetrics {@link ThreadPoolBulkheadMetrics} with a bulkhead
     * as a source.
     *
     * @param bulkhead the circuit breaker
     */
    public static ThreadPoolBulkheadMetrics ofBulkhead(ThreadPoolBulkhead bulkhead) {
        return new ThreadPoolBulkheadMetrics(List.of(bulkhead));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
