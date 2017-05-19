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
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.vavr.CheckedFunction0;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public class CacheImpl<K, V>  implements Cache<K,V> {

    private static final Logger LOG = LoggerFactory.getLogger(CacheImpl.class);

    private final javax.cache.Cache<K, V> cache;
    private final FlowableProcessor<CacheEvent> eventPublisher;
    private final CacheMetrics metrics;

    public CacheImpl(javax.cache.Cache<K, V> cache) {
        this.cache = cache;
        PublishProcessor<CacheEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
        this.metrics = new CacheMetrics();
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
    public V computeIfAbsent(K cacheKey, CheckedFunction0<V> supplier) {
        return getValueFromCache(cacheKey)
                .getOrElse(() -> computeAndPut(cacheKey, supplier));
    }

    private V computeAndPut(K cacheKey, CheckedFunction0<V> supplier) {
        return Try.of(supplier)
                .andThen(value -> putValueIntoCache(cacheKey, value))
            .get();
    }

    private Option<V> getValueFromCache(K cacheKey){
        try {
            Option<V> result = Option.of(cache.get(cacheKey));
            if (result.isDefined()) {
                onCacheHit(cacheKey);
                return result;
            } else {
                onCacheMiss(cacheKey);
                return result;
            }
        }catch (Exception exception){
            LOG.warn(String.format("Failed to get a value from Cache %s", getName()), exception);
            onError(exception);
            return Option.none();
        }
    }

    private void putValueIntoCache(K cacheKey, V value) {
        try {
            if(value != null) {
                cache.put(cacheKey, value);
            }
        } catch (Exception exception){
            LOG.warn(String.format("Failed to put a value into Cache %s", getName()), exception);
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
        if(eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(event.get());
        }
    }

    @Override
    public Flowable<CacheEvent> getEventStream() {
        return eventPublisher;
    }

    private final class CacheMetrics implements Metrics {

        private final LongAdder cacheMisses;
        private final LongAdder cacheHits;
        private CacheMetrics() {
            cacheMisses = new LongAdder();
            cacheHits = new LongAdder();
        }

        public void onCacheMiss(){
            cacheMisses.increment();
        }

        public void onCacheHit(){
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
