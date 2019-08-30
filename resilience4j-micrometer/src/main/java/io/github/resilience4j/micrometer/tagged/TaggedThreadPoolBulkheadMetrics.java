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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A micrometer binder that is used to register ThreadPoolBulkheadM exposed {@link Metrics metrics}.
 */
public class TaggedThreadPoolBulkheadMetrics extends AbstractMetrics implements MeterBinder {

    /**
     * Creates a new binder that uses given {@code registry} as source of bulkheads.
     *
     * @param bulkheadRegistry the source of bulkheads
     * @return The {@link TaggedThreadPoolBulkheadMetrics} instance.
     */
    public static TaggedThreadPoolBulkheadMetrics ofThreadPoolBulkheadRegistry(ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new TaggedThreadPoolBulkheadMetrics(MetricNames.ofDefaults(), bulkheadRegistry);
    }

    /**
     * Creates a new binder defining custom metric names and
     * using given {@code registry} as source of bulkheads.
     *
     * @param names custom names of the metrics
     * @param bulkheadRegistry the source of bulkheads
     * @return The {@link TaggedThreadPoolBulkheadMetrics} instance.
     */
    public static TaggedThreadPoolBulkheadMetrics ofThreadPoolBulkheadRegistry(MetricNames names, ThreadPoolBulkheadRegistry bulkheadRegistry) {
        return new TaggedThreadPoolBulkheadMetrics(names, bulkheadRegistry);
    }

    private final MetricNames names;
    private final ThreadPoolBulkheadRegistry bulkheadRegistry;

    private TaggedThreadPoolBulkheadMetrics(MetricNames names, ThreadPoolBulkheadRegistry bulkheadRegistry) {
        super();
        this.names = requireNonNull(names);
        this.bulkheadRegistry = requireNonNull(bulkheadRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (ThreadPoolBulkhead bulkhead : bulkheadRegistry.getAllBulkheads()) {
            addMetrics(registry, bulkhead);
        }
        bulkheadRegistry.getEventPublisher().onEntryAdded(event -> addMetrics(registry, event.getAddedEntry()));
        bulkheadRegistry.getEventPublisher().onEntryRemoved(event -> removeMetrics(registry, event.getRemovedEntry().getName()));
        bulkheadRegistry.getEventPublisher().onEntryReplaced(event -> {
            removeMetrics(registry, event.getOldEntry().getName());
            addMetrics(registry, event.getNewEntry());
        });
    }

    private void addMetrics(MeterRegistry registry, ThreadPoolBulkhead bulkhead) {
        Set<Meter.Id> idSet = new HashSet<>();

        idSet.add(Gauge.builder(names.getQueueDepthMetricName(), bulkhead, bh -> bh.getMetrics().getQueueDepth())
                .description("The queue depth")
                .tag(TagNames.NAME, bulkhead.getName())
                .register(registry).getId());
        idSet.add(Gauge.builder(names.getThreadPoolSizeMetricName(), bulkhead, bh -> bh.getMetrics().getThreadPoolSize())
                .description("The thread pool size")
                .tag(TagNames.NAME, bulkhead.getName())
                .register(registry).getId());

        idSet.add(Gauge.builder(names.getQueueCapacityMetricName(), bulkhead, bh -> bh.getMetrics().getQueueCapacity())
                .description("The queue capacity")
                .tag(TagNames.NAME, bulkhead.getName())
                .register(registry).getId());

        idSet.add(Gauge.builder(names.getMaxThreadPoolSizeMetricName(), bulkhead, bh -> bh.getMetrics().getMaximumThreadPoolSize())
                .description("The maximum thread pool size")
                .tag(TagNames.NAME, bulkhead.getName())
                .register(registry).getId());

        idSet.add(Gauge.builder(names.getCoreThreadPoolSizeMetricName(), bulkhead, bh -> bh.getMetrics().getCoreThreadPoolSize())
                .description("The core thread pool size")
                .tag(TagNames.NAME, bulkhead.getName())
                .register(registry).getId());

        meterIdMap.put(bulkhead.getName(), idSet);
    }

    /** Defines possible configuration for metric names. */
    public static class MetricNames {

        private static final String DEFAULT_PREFIX = "resilience4j.bulkhead";

        public static final String DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME = DEFAULT_PREFIX + ".queue.depth";
        public static final String DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME = DEFAULT_PREFIX + ".queue.capacity";
        public static final String DEFAULT_THREAD_POOL_SIZE_METRIC_NAME = DEFAULT_PREFIX + ".thread.pool.size";
        public static final String DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME = DEFAULT_PREFIX + ".max.thread.pool.size";
        public static final String DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME = DEFAULT_PREFIX + ".core.thread.pool.size";

        /**
         * Returns a builder for creating custom metric names.
         * Note that names have default values, so only desired metrics can be renamed.
         * @return The builder.
         */
        public static Builder custom() {
            return new Builder();
        }

        /** Returns default metric names.
         * @return The default {@link MetricNames} instance.
         */
        public static MetricNames ofDefaults() {
            return new MetricNames();
        }

        private String queueDepthMetricName = DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME;
        private String threadPoolSizeMetricName = DEFAULT_THREAD_POOL_SIZE_METRIC_NAME;
        private String maxThreadPoolSizeMetricName = DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME;
        private String coreThreadPoolSizeMetricName = DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME;
        private String queueCapacityMetricName = DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME;

        private MetricNames() {}

        /**
         * Returns the metric name for queue depth,
         * defaults to {@value DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME}.
         * @return The queue depth metric name.
         */
        public String getQueueDepthMetricName() {
            return queueDepthMetricName;
        }

        /**
         * Returns the metric name for thread pool size,
         * defaults to {@value DEFAULT_THREAD_POOL_SIZE_METRIC_NAME}.
         * @return The thread pool size metric name.
         */
        public String getThreadPoolSizeMetricName() {
            return threadPoolSizeMetricName;
        }

        /**
         * Returns the metric name for max thread pool size,
         * defaults to {@value DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME}.
         * @return The max thread pool size metric name.
         */
        public String getMaxThreadPoolSizeMetricName() {
            return maxThreadPoolSizeMetricName;
        }

        /**
         * Returns the metric name for core thread pool size,
         * defaults to {@value DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME}.
         * @return The core thread pool size metric name.
         */
        public String getCoreThreadPoolSizeMetricName() {
            return coreThreadPoolSizeMetricName;
        }

        /**
         * Returns the metric name for queue capacity,
         * defaults to {@value DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME}.
         * @return The queue capacity metric name.
         */
        public String getQueueCapacityMetricName() {
            return queueCapacityMetricName;
        }

        /** Helps building custom instance of {@link MetricNames}. */
        public static class Builder {

            private final MetricNames metricNames = new MetricNames();

            /** Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_QUEUE_DEPTH_METRIC_NAME} with a given one.
             * @param queueDepthMetricName The queue depth metric name.
             * @return The builder.
             */
            public Builder queueDepthMetricName(String queueDepthMetricName) {
                metricNames.queueDepthMetricName = requireNonNull(queueDepthMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_THREAD_POOL_SIZE_METRIC_NAME} with a given one.
             * @param threadPoolSizeMetricName The thread pool size metric name.
             * @return The builder.
             */
            public Builder threadPoolSizeMetricName(String threadPoolSizeMetricName) {
                metricNames.threadPoolSizeMetricName = requireNonNull(threadPoolSizeMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_MAX_THREAD_POOL_SIZE_METRIC_NAME} with a given one.
             * @param maxThreadPoolSizeMetricName The max thread pool size metric name.
             * @return The builder.
             */
            public Builder maxThreadPoolSizeMetricName(String maxThreadPoolSizeMetricName) {
                metricNames.maxThreadPoolSizeMetricName = requireNonNull(maxThreadPoolSizeMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_CORE_THREAD_POOL_SIZE_METRIC_NAME} with a given one.
             * @param coreThreadPoolSizeMetricName The core thread pool size metric name.
             * @return The builder.
             */
            public Builder coreThreadPoolSizeMetricName(String coreThreadPoolSizeMetricName) {
                metricNames.coreThreadPoolSizeMetricName = requireNonNull(coreThreadPoolSizeMetricName);
                return this;
            }

            /** Overrides the default metric name {@value MetricNames#DEFAULT_BULKHEAD_QUEUE_CAPACITY_METRIC_NAME} with a given one.
             * @param queueCapacityMetricName The queue capacity metric name.
             * @return The builder.
             */
            public Builder queueCapacityMetricName(String queueCapacityMetricName) {
                metricNames.queueCapacityMetricName = requireNonNull(queueCapacityMetricName);
                return this;
            }

            /** Builds {@link MetricNames} instance.
             * @return The built {@link MetricNames} instance.
             */
            public MetricNames build() {
                return metricNames;
            }
        }
    }
}
