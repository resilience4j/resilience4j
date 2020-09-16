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
package io.github.resilience4j.cache;

import io.github.resilience4j.cache.event.CacheEvent;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;

import static io.github.resilience4j.adapter.RxJava2Adapter.toFlowable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

public class CacheTest {

    private javax.cache.Cache<String, String> cache;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        cache = mock(javax.cache.Cache.class);
    }

    @Test
    public void shouldReturnValueFromDecoratedSupplier() {
        given(cache.get("testKey")).willReturn(null);
        Cache<String, String> cacheContext = Cache.of(cache);
        TestSubscriber<CacheEvent.Type> testSubscriber = toFlowable(
            cacheContext.getEventPublisher())
            .map(CacheEvent::getEventType)
            .test();
        Function<String, String> cachedFunction = Cache
            .decorateSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isEqualTo(0);
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isEqualTo(1);
        then(cache).should().put("testKey", "Hello world");
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_MISS);
    }
}
