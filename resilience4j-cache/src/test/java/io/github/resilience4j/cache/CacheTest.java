/*
 *
 *  Copyright 2026 Robert Winkler
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.util.function.Function;

import static io.github.resilience4j.adapter.RxJava2Adapter.toFlowable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

class CacheTest {

    private javax.cache.Cache<String, String> cache;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        cache = mock(javax.cache.Cache.class);
    }

    @Test
    void shouldReturnValueFromDecoratedCheckedSupplier() throws Throwable {
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
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isOne();
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_MISS);
    }

    @Test
    void shouldReturnValueFromDecoratedSupplier() {
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
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isOne();
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_MISS);
    }

    @Test
    void shouldReturnValueFromDecoratedCallable() throws Throwable {
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
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isOne();
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_MISS);
    }

    @Test
    void shouldReturnValueOfSupplier() throws Throwable {
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
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isOne();
        testSubscriber
            .assertValueCount(2)
            .assertValues(CacheEvent.Type.CACHE_MISS, CacheEvent.Type.ERROR);
    }

    @Test
    void shouldReturnCachedValue() throws Throwable {
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
        assertThat(cacheContext.getMetrics().getNumberOfCacheHits()).isOne();
        assertThat(cacheContext.getMetrics().getNumberOfCacheMisses()).isZero();
        testSubscriber
            .assertValueCount(1)
            .assertValues(CacheEvent.Type.CACHE_HIT);
    }

    @Test
    void shouldReturnValueFromDecoratedCallableBecauseOfException() throws Throwable {
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
