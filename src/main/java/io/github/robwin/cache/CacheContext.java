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
import io.reactivex.processors.PublishProcessor;
import javaslang.control.Option;

class CacheContext<K, V>  implements Cache<K,V> {

    private final javax.cache.Cache<K, V> cache;
    private final PublishProcessor<CacheEvent> eventPublisher;

    CacheContext(javax.cache.Cache<K, V> cache) {
        this.cache = cache;
        this.eventPublisher = PublishProcessor.create();
    }

    @Override
    public boolean containsKey(K cacheKey) {
        try {
            boolean cacheContainsKey = cache.containsKey(cacheKey);
            if(cacheContainsKey){
                onCacheHit(cacheKey);
            }else{
                onCacheMiss(cacheKey);
            }
            return cacheContainsKey;
        } catch (Exception exception){
            onError(exception);
            return false;
        }
    }

    @Override
    public void put(K cacheKey, V value) {
        try {
            cache.put(cacheKey, value);
        } catch (Exception exception){
            onError(exception);
        }
    }

    @Override
    public Option<V> get(K cacheKey) {
        try {
            return Option.of(cache.get(cacheKey));
        } catch (Exception exception){
            onError(exception);
            return Option.none();
        }
    }

    private void onError(Exception exception) {
        eventPublisher.onNext(new CacheOnErrorEvent(cache.getName(), exception));
    }

    private void onCacheMiss(K cacheKey) {
        eventPublisher.onNext(new CacheOnMissEvent<>(cache.getName(), cacheKey));
    }

    private void onCacheHit(K cacheKey) {
        eventPublisher.onNext(new CacheOnHitEvent<>(cache.getName(), cacheKey));
    }

    @Override
    public Flowable<CacheEvent> getEventStream() {
        return eventPublisher;
    }
}
