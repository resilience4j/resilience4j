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
import io.github.robwin.consumer.CircularEventConsumer;
import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CacheTest {

    private javax.cache.Cache<String, String> cache;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp(){
        cache = mock(javax.cache.Cache.class);
    }

    @Test
    public void shouldInvokeDecoratedCheckedSupplier() throws Throwable {
        // Given the cache does not contain the key
        given(cache.containsKey("testKey")).willReturn(false);

        Try.CheckedFunction<String, String> cachedFunction = Cache.decorateCheckedSupplier(Cache.of(cache), () -> "Hello world");
        String value = cachedFunction.apply("testKey");
        assertThat(value).isEqualTo("Hello world");
    }

    @Test
    public void shouldInvokeDecoratedSupplier() throws Throwable {
        // Given the cache does not contain the key
        given(cache.containsKey("testKey")).willReturn(false);

        Function<String, String> cachedFunction = Cache.decorateSupplier(Cache.of(cache), () -> "Hello world");
        String value = cachedFunction.apply("testKey");
        assertThat(value).isEqualTo("Hello world");
    }

    @Test
    public void shouldInvokeDecoratedCallable() throws Throwable {
        // Given the cache does not contain the key
        given(cache.containsKey("testKey")).willReturn(false);

        Cache<String, String> cacheContext = Cache.of(cache);
        CircularEventConsumer<CacheEvent> cacheEventConsumer = new CircularEventConsumer<>(10);
        cacheContext.getEventStream()
                .subscribe(cacheEventConsumer);

        Try.CheckedFunction<String, String> cachedFunction = Cache.decorateCallable(cacheContext, () -> "Hello world");
        String value = cachedFunction.apply("testKey");
        assertThat(value).isEqualTo("Hello world");


        assertThat(cacheEventConsumer.getBufferedEvents()).hasSize(1);
        assertThat(cacheEventConsumer.getBufferedEvents().get(0)).isInstanceOf(CacheOnMissEvent.class);
    }

    @Test
    public void shouldReturnCachedValue() throws Throwable {
        // Given the cache contains the key
        given(cache.containsKey("testKey")).willReturn(true);
        // Return the value from cache
        given(cache.get("testKey")).willReturn("Hello from cache");

        Cache<String, String> cacheContext = Cache.of(cache);
        CircularEventConsumer<CacheEvent> cacheEventConsumer = new CircularEventConsumer<>(10);
        cacheContext.getEventStream()
                .subscribe(cacheEventConsumer);

        Try.CheckedFunction<String, String> cachedFunction = Cache.decorateCheckedSupplier(cacheContext, () -> "Hello world");
        String value = cachedFunction.apply("testKey");
        assertThat(value).isEqualTo("Hello from cache");

        assertThat(cacheEventConsumer.getBufferedEvents()).hasSize(1);
        assertThat(cacheEventConsumer.getBufferedEvents().get(0)).isInstanceOf(CacheOnHitEvent.class);
    }

    @Test
    public void shouldInvokeDecoratedCallableBecauseOfException() throws Throwable {
        // Given the cache contains the key
        given(cache.containsKey("cacheKey")).willThrow(new RuntimeException("Cache is not available"));

        Cache<String, String> cacheContext = Cache.of(cache);
        CircularEventConsumer<CacheEvent> cacheEventConsumer = new CircularEventConsumer<>(10);
        cacheContext.getEventStream()
                .subscribe(cacheEventConsumer);

        Try.CheckedFunction<String, String> cachedFunction = Cache.decorateCheckedSupplier(cacheContext, () -> "Hello world");
        String value = cachedFunction.apply("cacheKey");
        assertThat(value).isEqualTo("Hello world");

        assertThat(cacheEventConsumer.getBufferedEvents()).hasSize(1);
        assertThat(cacheEventConsumer.getBufferedEvents().get(0)).isInstanceOf(CacheOnErrorEvent.class);


    }
}
