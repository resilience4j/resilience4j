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
import io.github.resilience4j.core.functions.CheckedFunction;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.util.function.Function;

import static io.github.resilience4j.adapter.RxJava2Adapter.toFlowable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

public class CacheTest {

    private javax.cache.Cache<String, String> cache;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        cache = mock(javax.cache.Cache.class);
    }

    @Test
    public void shouldReturnValueFromDecoratedCheckedSupplier() throws Throwable {
        given(cache.get("testKey")).willReturn(null);
        given(cache.invoke(eq("testKey"), any())).willAnswer(new CacheInvokeAnswer());
        Cache<String, String> cacheContext = Cache.of(cache);
        TestSubscriber<CacheEvent.Type> testSubscriber =
            toFlowable(cacheContext.getEventPublisher())
                .map(CacheEvent::getEventType)
                .test();
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCheckedSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isZero();
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isEqualTo(1);
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_MISS);
    }

    @Test
    public void shouldReturnValueFromDecoratedSupplier() {
        given(cache.get("testKey")).willReturn(null);
        given(cache.invoke(eq("testKey"), any())).willAnswer(new CacheInvokeAnswer());
        Cache<String, String> cacheContext = Cache.of(cache);
        TestSubscriber<CacheEvent.Type> testSubscriber = toFlowable(
            cacheContext.getEventPublisher())
            .map(CacheEvent::getEventType)
            .test();
        Function<String, String> cachedFunction = Cache
            .decorateSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isZero();
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isEqualTo(1);
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_MISS);
    }

    @Test
    public void shouldReturnValueFromDecoratedCallable() throws Throwable {
        given(cache.get("testKey")).willReturn(null);
        given(cache.invoke(eq("testKey"), any())).willAnswer(new CacheInvokeAnswer());
        Cache<String, String> cacheContext = Cache.of(cache);
        TestSubscriber<CacheEvent.Type> testSubscriber =
            toFlowable(cacheContext.getEventPublisher())
                .map(CacheEvent::getEventType)
                .test();
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCallable(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isZero();
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isEqualTo(1);
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_MISS);
    }

    @Test
    public void shouldReturnValueOfSupplier() throws Throwable {
        given(cache.get("testKey")).willReturn(null);
        willThrow(new RuntimeException("Cache is not available")).given(cache)
            .invoke(eq("testKey"), any());
        Cache<String, String> cacheContext = Cache.of(cache);
        TestSubscriber<CacheEvent.Type> testSubscriber =
            toFlowable(cacheContext.getEventPublisher())
                .map(CacheEvent::getEventType)
                .test();
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCheckedSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isZero();
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isEqualTo(1);
        testSubscriber
            .assertValueCount(2)
            .assertValues(CacheEvent.Type.CACHE_MISS, CacheEvent.Type.ERROR);
    }

    @Test
    public void shouldReturnCachedValue() throws Throwable {
        given(cache.get("testKey")).willReturn("Hello from cache");
        Cache<String, String> cacheContext = Cache.of(cache);
        TestSubscriber<CacheEvent.Type> testSubscriber =
            toFlowable(cacheContext.getEventPublisher())
                .map(CacheEvent::getEventType)
                .test();
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCheckedSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello from cache");
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isEqualTo(1);
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isZero();
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_HIT);
    }

    @Test
    public void shouldReturnValueFromDecoratedCallableBecauseOfException() throws Throwable {
        given(cache.get("testKey")).willThrow(new RuntimeException("Cache is not available"));
        given(cache.invoke(eq("testKey"), any())).willThrow(new RuntimeException("Cache is not available"));
        Cache<String, String> cacheContext = Cache.of(cache);
        TestSubscriber<CacheEvent.Type> testSubscriber =
            toFlowable(cacheContext.getEventPublisher())
                .map(CacheEvent::getEventType)
                .test();
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCheckedSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isZero();
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isZero();
        testSubscriber
            .assertValueCount(2)
            .assertValues(CacheEvent.Type.ERROR, CacheEvent.Type.ERROR);
    }

    private static class CacheInvokeAnswer implements Answer<String> {

        private final MutableEntry<String, String> mutableEntry;

        @SuppressWarnings("unchecked")
        CacheInvokeAnswer() {
            mutableEntry = mock(MutableEntry.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public String answer(InvocationOnMock invocation) {
            EntryProcessor<String, String, String> argument = invocation.getArgument(1, EntryProcessor.class);
            return argument.process(mutableEntry);
        }
    }
}
