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

import org.junit.Test;

import java.io.IOException;

import static io.github.resilience4j.cache.event.CacheEvent.Type;
import static org.assertj.core.api.Assertions.assertThat;

public class CacheEventTest {

    @Test
    public void testCacheOnErrorEvent() {
        CacheOnErrorEvent cacheOnErrorEvent = new CacheOnErrorEvent("test",
            new IOException());
        assertThat(cacheOnErrorEvent.getCacheName()).isEqualTo("test");
        assertThat(cacheOnErrorEvent.getThrowable()).isInstanceOf(IOException.class);
        assertThat(cacheOnErrorEvent.getEventType()).isEqualTo(Type.ERROR);
        assertThat(cacheOnErrorEvent.toString())
            .contains("Cache 'test' recorded an error: 'java.io.IOException'.");
    }

    @Test
    public void testCacheOnHitEvent() {
        CacheOnHitEvent<String> cacheOnHitEvent = new CacheOnHitEvent<>("test",
            "testKey");
        assertThat(cacheOnHitEvent.getCacheName()).isEqualTo("test");
        assertThat(cacheOnHitEvent.getCacheKey()).isEqualTo("testKey");
        assertThat(cacheOnHitEvent.getEventType()).isEqualTo(Type.CACHE_HIT);
        assertThat(cacheOnHitEvent.toString())
            .contains("Cache 'test' recorded a cache hit on cache key 'testKey'.");
    }

    @Test
    public void testCacheOnMissEvent() {
        CacheOnMissEvent<String> cacheOnMissEvent = new CacheOnMissEvent<>("test",
            "testKey");
        assertThat(cacheOnMissEvent.getCacheName()).isEqualTo("test");
        assertThat(cacheOnMissEvent.getCacheKey()).isEqualTo("testKey");
        assertThat(cacheOnMissEvent.getEventType()).isEqualTo(Type.CACHE_MISS);
        assertThat(cacheOnMissEvent.toString())
            .contains("Cache 'test' recorded a cache miss on cache key 'testKey'.");
    }

}
