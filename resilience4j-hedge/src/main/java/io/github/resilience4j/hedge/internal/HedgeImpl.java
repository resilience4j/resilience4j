/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.emptyMap;

public class HedgeImpl extends ExecutorServiceHedge<ScheduledThreadPoolExecutor> implements Hedge {

    private final HedgeMetrics metrics;

    public HedgeImpl(String name, HedgeConfig hedgeConfig) {
        this(name, hedgeConfig, emptyMap());
    }

    /**
     * Creates a new HedgeImpl with the given name, config, and tags.
     *
     * @param name                      the name of the Hedge
     * @param hedgeConfig               the Hedge configuration
     * @param tags                      the tags of the Hedge
     * @param scheduledThreadPoolExecutor the ScheduledThreadPoolExecutor to use
     */
    public HedgeImpl(String name, HedgeConfig hedgeConfig,
                     Map<String, String> tags,
                     @NonNull ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        super(name, tags, hedgeConfig, scheduledThreadPoolExecutor);
        this.metrics = new HedgeMetrics();
        eventProcessor.onPrimarySuccess(__ -> metrics.primarySuccess.incrementAndGet());
        eventProcessor.onPrimaryFailure(__ -> metrics.primaryFailure.incrementAndGet());
        eventProcessor.onSecondarySuccess(__ -> metrics.secondarySuccess.incrementAndGet());
        eventProcessor.onSecondaryFailure(__ -> metrics.secondaryFailure.incrementAndGet());
    }

    /**
     * Creates a new HedgeImpl with the given name, config, and tags using a default context-aware executor.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig the Hedge configuration
     * @param tags        the tags of the Hedge
     */
    public HedgeImpl(String name, HedgeConfig hedgeConfig,
                     Map<String, String> tags) {
        this(name, hedgeConfig, tags, ContextAwareScheduledThreadPoolExecutor
                .newScheduledThreadPool()
                .corePoolSize(hedgeConfig.getConcurrentHedges())
                .contextPropagators(hedgeConfig.getContextPropagators())
                .build());
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    private final class HedgeMetrics implements Metrics {

        private AtomicLong primarySuccess = new AtomicLong(0);
        private AtomicLong primaryFailure = new AtomicLong(0);
        private AtomicLong secondarySuccess = new AtomicLong(0);
        private AtomicLong secondaryFailure = new AtomicLong(0);

        @Override
        public Duration getCurrentHedgeDelay() {
            return getDuration();
        }

        @Override
        public long getPrimarySuccessCount() {
            return primarySuccess.get();
        }

        @Override
        public long getSecondarySuccessCount() {
            return secondarySuccess.get();
        }

        @Override
        public long getPrimaryFailureCount() {
            return primaryFailure.get();
        }

        @Override
        public long getSecondaryFailureCount() {
            return secondaryFailure.get();
        }

        @Override
        public int getSecondaryPoolActiveCount() {
            return getConfiguredHedgeExecutor().getActiveCount();
        }
    }

}

