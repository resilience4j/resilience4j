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
package io.github.robwin.cache;

import io.github.robwin.cache.event.CacheEvent;
import io.github.robwin.cache.event.CacheOnErrorEvent;
import io.github.robwin.cache.event.CacheOnHitEvent;
import io.github.robwin.cache.event.CacheOnMissEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import javaslang.control.Option;
import javaslang.control.Try;

import java.util.function.Supplier;

class CacheContext<K, V>  implements Cache<K,V> {

    private final javax.cache.Cache<K, V> cache;
    private final FlowableProcessor<CacheEvent> eventPublisher;

    CacheContext(javax.cache.Cache<K, V> cache) {
        this.cache = cache;
        PublishProcessor<CacheEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public V computeIfAbsent(K cacheKey, Try.CheckedSupplier<V> supplier) {
        return getValueFromCache(cacheKey)
                .getOrElse(() -> computeAndPut(cacheKey, supplier));
    }

    private V computeAndPut(K cacheKey, Try.CheckedSupplier<V> supplier) {
        return Try.of(supplier)
                .andThen(value -> putValueIntoCache(cacheKey, value))
            .get();
    }

    private synchronized Option<V> getValueFromCache(K cacheKey){
        try {
            if (cache.containsKey(cacheKey)) {
                onCacheHit(cacheKey);
                return Option.of(cache.get(cacheKey));
            } else {
                onCacheMiss(cacheKey);
                return Option.none();
            }
        }catch (Exception exception){
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
            onError(exception);
        }
    }

    private void onError(Exception exception) {
        publishCacheEvent(() -> new CacheOnErrorEvent(cache.getName(), exception));
    }

    private void onCacheMiss(K cacheKey) {
        publishCacheEvent(() -> new CacheOnMissEvent<>(cache.getName(), cacheKey));
    }

    private void onCacheHit(K cacheKey) {
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
}
