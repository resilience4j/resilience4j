/*
 * Copyright 2025
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

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadType;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A metrics collector for Resilience4j thread usage.
 * <p>
 * Records metrics that monitor:
 * <ol>
 *     <li>Whether virtual threads are enabled</li>
 * </ol>
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
public class ThreadMetrics implements MeterBinder {

    /**
     * Metric name prefix for thread metrics.
     */
    public static final String DEFAULT_PREFIX = "resilience4j.thread";

    private static final String VIRTUAL_THREAD_ENABLED = "virtual_thread_enabled";

    private final String prefix;
    private final List<Tag> tags;

    private ThreadMetrics(String prefix, List<Tag> tags) {
        this.prefix = requireNonNull(prefix);
        this.tags = requireNonNull(tags);
    }

    /**
     * Creates a new instance of ThreadMetrics with default metric names.
     *
     * @param registry the registry to bind metrics to
     * @return The ThreadMetrics instance.
     */
    public static ThreadMetrics ofMeterRegistry(MeterRegistry registry) {
        return ofMeterRegistry(DEFAULT_PREFIX, registry);
    }

    /**
     * Creates a new instance of ThreadMetrics with default metric names.
     *
     * @param prefix   the prefix for metric names
     * @param registry the registry to bind metrics to
     * @return The ThreadMetrics instance.
     */
    public static ThreadMetrics ofMeterRegistry(String prefix, MeterRegistry registry) {
        return ofMeterRegistry(prefix, registry, new ArrayList<>());
    }

    /**
     * Creates a new instance of ThreadMetrics with default metric names.
     *
     * @param prefix   the prefix for metric names
     * @param registry the registry to bind metrics to
     * @param tags     additional tags to add to metrics
     * @return The ThreadMetrics instance.
     */
    public static ThreadMetrics ofMeterRegistry(String prefix, MeterRegistry registry, List<Tag> tags) {
        ThreadMetrics threadMetrics = new ThreadMetrics(prefix, tags);
        threadMetrics.bindTo(registry);
        return threadMetrics;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // Create gauge that reports whether virtual threads are enabled (1.0) or not (0.0)
        Gauge.builder(prefix + "." + VIRTUAL_THREAD_ENABLED, () -> 
                ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL ? 1.0 : 0.0)
            .tags(tags)
            .description("Whether virtual threads are enabled in Resilience4j (1.0 = enabled, 0.0 = disabled)")
            .register(registry);
    }
}