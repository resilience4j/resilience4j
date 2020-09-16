/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.cache.internal;

import io.github.resilience4j.cache.Cache;
import io.github.resilience4j.cache.event.CacheEvent;
import io.github.resilience4j.cache.event.CacheOnErrorEvent;
import io.github.resilience4j.cache.event.CacheOnHitEvent;
import io.github.resilience4j.cache.event.CacheOnMissEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public class CacheImpl<K, V> implements Cache<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(CacheImpl.class);

    private final javax.cache.Cache<K, V> cache;
    private final CacheMetrics metrics;
    private final CacheEventProcessor eventProcessor;

    public CacheImpl(javax.cache.Cache<K, V> cache) {
        this.cache = cache;
        this.metrics = new CacheMetrics();
        this.eventProcessor = new CacheEventProcessor();
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public V computeIfAbsent(K cacheKey, Supplier<V> supplier) {
        return getValueFromCache(cacheKey)
            .orElseGet(() -> computeAndPut(cacheKey, supplier));
    }

    private V computeAndPut(K cacheKey, Supplier<V> supplier) {
        final V value = supplier.get();
        putValueIntoCache(cacheKey, value);
        return value;
    }

    private Optional<V> getValueFromCache(K cacheKey) {
        try {
            Optional<V> result = Optional.ofNullable(cache.get(cacheKey));
            if (result.isPresent()) {
                onCacheHit(cacheKey);
            } else {
                onCacheMiss(cacheKey);
            }
            return result;
        } catch (Exception exception) {
            LOG.warn("Failed to get a value from Cache {}", getName(), exception);
            onError(exception);
            return Optional.empty();
        }
    }

    private void putValueIntoCache(K cacheKey, V value) {
        try {
            if (value != null) {
                cache.put(cacheKey, value);
            }
        } catch (Exception exception) {
            LOG.warn("Failed to put a value into Cache {}", getName(), exception);
            onError(exception);
        }
    }

    private void onError(Throwable throwable) {
        publishCacheEvent(() -> new CacheOnErrorEvent(cache.getName(), throwable));
    }

    private void onCacheMiss(K cacheKey) {
        metrics.onCacheMiss();
        publishCacheEvent(() -> new CacheOnMissEvent<>(cache.getName(), cacheKey));
    }

    private void onCacheHit(K cacheKey) {
        metrics.onCacheHit();
        publishCacheEvent(() -> new CacheOnHitEvent<>(cache.getName(), cacheKey));
    }

    private void publishCacheEvent(Supplier<CacheEvent> event) {
        if (eventProcessor.hasConsumers()) {
            eventProcessor.processEvent(event.get());
        }
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    private class CacheEventProcessor extends EventProcessor<CacheEvent> implements
        EventConsumer<CacheEvent>, EventPublisher {

        @Override
        public EventPublisher onCacheHit(EventConsumer<CacheOnHitEvent> eventConsumer) {
            registerConsumer(CacheOnHitEvent.class.getSimpleName(), eventConsumer);
            return this;
        }

        @Override
        public EventPublisher onCacheMiss(EventConsumer<CacheOnMissEvent> eventConsumer) {
            registerConsumer(CacheOnMissEvent.class.getSimpleName(), eventConsumer);
            return this;
        }

        @Override
        public EventPublisher onError(EventConsumer<CacheOnErrorEvent> eventConsumer) {
            registerConsumer(CacheOnErrorEvent.class.getSimpleName(), eventConsumer);
            return this;
        }

        @Override
        public void consumeEvent(CacheEvent event) {
            super.processEvent(event);
        }
    }

    private final class CacheMetrics implements Metrics {

        private final LongAdder cacheMisses;
        private final LongAdder cacheHits;

        private CacheMetrics() {
            cacheMisses = new LongAdder();
            cacheHits = new LongAdder();
        }

        void onCacheMiss() {
            cacheMisses.increment();
        }

        void onCacheHit() {
            cacheHits.increment();
        }

        @Override
        public long getNumberOfCacheHits() {
            return cacheHits.longValue();
        }

        @Override
        public long getNumberOfCacheMisses() {
            return cacheMisses.longValue();
        }
    }
}
