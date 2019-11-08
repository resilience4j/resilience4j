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
package io.github.resilience4j.cache.event;

/**
 * A CacheEvent which informs about a cache hit.
 */
public class CacheOnHitEvent<K> extends AbstractCacheEvent {

    private final K cacheKey;

    public CacheOnHitEvent(String cacheName, K cacheKey) {
        super(cacheName);
        this.cacheKey = cacheKey;
    }

    @Override
    public Type getEventType() {
        return Type.CACHE_HIT;
    }

    public K getCacheKey() {
        return cacheKey;
    }

    @Override
    public String toString() {
        return String.format("%s: Cache '%s' recorded a cache hit on cache key '%s'.",
            getCreationTime(),
            getCacheName(),
            getCacheKey().toString());
    }
}
