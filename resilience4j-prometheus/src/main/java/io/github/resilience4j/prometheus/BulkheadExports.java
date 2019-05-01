/*
 *
 *  Copyright 2018 Valtteri Walldén
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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.vavr.collection.Array;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * An adapter from builtin {@link Bulkhead.Metrics} to prometheus
 * {@link io.prometheus.client.CollectorRegistry}.
 *
 * @deprecated use {@link io.github.resilience4j.prometheus.collectors.BulkheadMetricsCollector} instead
 */
@Deprecated
public class BulkheadExports extends Collector {
    private static final String DEFAULT_NAME = "resilience4j_bulkhead";

    private final String                       name;
    private final Supplier<Iterable<Bulkhead>> bulkheadsSupplier;

    /**
     * Creates a new instance of {@link BulkheadExports} with specified metrics names prefix and
     * {@link Supplier} of bulkheads
     *
     * @param prefix the prefix of metrics names
     * @param bulkheadSupplier the supplier of bulkheads
     */
    public static BulkheadExports ofSupplier(String prefix, Supplier<Iterable<Bulkhead>> bulkheadSupplier) {
        return new BulkheadExports(prefix, bulkheadSupplier);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with default metrics names prefix and
     * {@link Supplier} of bulkheads
     *
     * @param bulkheadSupplier the supplier of bulkheads
     */
    public static BulkheadExports ofSupplier(Supplier<Iterable<Bulkhead>> bulkheadSupplier) {
        return new BulkheadExports(DEFAULT_NAME, bulkheadSupplier);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with default metrics names prefix and
     * {@link BulkheadRegistry} as a source of bulkheads.

     * @param bulkheadRegistry the registry of bulkheads
     */
    public static BulkheadExports ofBulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
        return new BulkheadExports(bulkheadRegistry);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with default metrics names prefix and
     * a bulkhead as a source.
     *
     * @param bulkhead the bulkhead
     */
    public static BulkheadExports ofBulkhead(Bulkhead bulkhead) {
        return new BulkheadExports(Array.of(bulkhead));
    }


    /**
     * Creates a new instance of {@link BulkheadExports} with default metrics names prefix and
     * {@link Iterable} of bulkheads.
     *
     * @param bulkheads the bulkheads
     */
    public static BulkheadExports ofIterable(Iterable<Bulkhead> bulkheads) {
        return new BulkheadExports(bulkheads);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with specified metrics names prefix and
     * {@link BulkheadRegistry} as a source of bulkheads.
     *
     * @param prefix the prefix of metrics names
     * @param bulkheadRegistry the registry of bulkheads
     */
    public static BulkheadExports ofBulkheadRegistry(String prefix, BulkheadRegistry bulkheadRegistry) {
        return new BulkheadExports(prefix, bulkheadRegistry);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with specified metrics names prefix and
     * {@link Iterable} of bulkheads.
     *
     * @param prefix the prefix of metrics names
     * @param bulkheads the bulkheads
     */
    public static BulkheadExports ofIterable(String prefix, Iterable<Bulkhead> bulkheads) {
        return new BulkheadExports(prefix, bulkheads);
    }


    /**
     * Creates a new instance of {@link BulkheadExports} with default metrics names prefix and
     * a bulkhead as a source.
     *
     * @param prefix the prefix of metrics names
     * @param bulkhead the bulkhead
     */
    public static BulkheadExports ofBulkhead(String prefix, Bulkhead bulkhead) {
        return new BulkheadExports(prefix, Array.of(bulkhead));
    }


    /**
     * Creates a new instance of {@link BulkheadExports} with default metric name and
     * {@link BulkheadRegistry}.
     *
     * @param bulkheadRegistry the bulkhead registry
     */
    private BulkheadExports(BulkheadRegistry bulkheadRegistry) {
        this(bulkheadRegistry::getAllBulkheads);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with default metric name and
     * {@link Iterable} of bulkheads.
     *
     * @param bulkheads the bulkheads
     */
    private BulkheadExports(Iterable<Bulkhead> bulkheads) {
        this(() -> bulkheads);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with default metric name and
     * {@link Supplier} of bulkheads
     *
     * @param bulkheadsSupplier the supplier of bulkheads
     */
    private BulkheadExports(Supplier<Iterable<Bulkhead>> bulkheadsSupplier) {
        this(DEFAULT_NAME, bulkheadsSupplier);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with specified metric name and
     * {@link BulkheadRegistry}.
     *
     * @param name the name of metric
     * @param bulkheadRegistry the bulkhead registry
     */
    public BulkheadExports(String name, BulkheadRegistry bulkheadRegistry) {
        this(name, bulkheadRegistry::getAllBulkheads);
    }


    /**
     * Creates a new instance of {@link BulkheadExports} with specified metric name and
     * {@link Iterable} of bulkheads.
     *
     * @param name the name of metric
     * @param bulkheads the bulkheads
     */
    private BulkheadExports(String name, Iterable<Bulkhead> bulkheads) {
        this(name, () -> bulkheads);
    }

    /**
     * Creates a new instance of {@link BulkheadExports} with specified metric name and
     * {@link Supplier} of bulkheads
     *
     * @param name the name of metric
     * @param bulkheadsSupplier the supplier of bulkheads
     */
    private BulkheadExports(String name, Supplier<Iterable<Bulkhead>> bulkheadsSupplier) {
        requireNonNull(name);
        requireNonNull(bulkheadsSupplier);

        this.name = name;
        this.bulkheadsSupplier = bulkheadsSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetricFamilySamples> collect() {
        final GaugeMetricFamily stats = new GaugeMetricFamily(
                name,
                "Bulkhead Stats",
                asList("name", "param"));

        for (Bulkhead bulkhead : bulkheadsSupplier.get()) {

            final Bulkhead.Metrics metrics = bulkhead.getMetrics();

            stats.addMetric(
                    asList(bulkhead.getName(), "available_concurrent_calls"),
                    metrics.getAvailableConcurrentCalls());
        }

        return singletonList(stats);
    }
}
