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

import io.github.resilience4j.core.functions.CheckedFunction;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CacheEventPublisherTest {

    private javax.cache.Cache<String, String> cache;
    private Logger logger;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        cache = mock(javax.cache.Cache.class);
        logger = mock(Logger.class);
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        Cache<String, String> cacheContext = Cache.of(cache);
        Cache.EventPublisher eventPublisher = cacheContext.getEventPublisher();
        Cache.EventPublisher eventPublisher2 = cacheContext.getEventPublisher();

        assertThat(eventPublisher).isEqualTo(eventPublisher2);
    }

    @Test
    public void shouldConsumeOnCacheHitEvent() throws Throwable {
        given(cache.get("testKey")).willReturn("Hello world");
        Cache<String, String> cacheContext = Cache.of(cache);
        cacheContext.getEventPublisher().onCacheHit(
            event -> logger.info(event.getEventType().toString()));
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCheckedSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        then(logger).should(times(1)).info("CACHE_HIT");
    }

    @Test
    public void shouldConsumeOnCacheMissEvent() throws Throwable {
        given(cache.get("testKey")).willReturn(null);
        Cache<String, String> cacheContext = Cache.of(cache);
        cacheContext.getEventPublisher().onCacheMiss(
            event -> logger.info(event.getEventType().toString()));
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCheckedSupplier(cacheContext, () -> "Hello world");

        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");
        then(logger).should(times(1)).info("CACHE_MISS");
    }

    @Test
    public void shouldConsumeOnErrorEvent() throws Throwable {
        given(cache.get("testKey")).willThrow(new RuntimeException("BLA"));
        Cache<String, String> cacheContext = Cache.of(cache);
        cacheContext.getEventPublisher().onError(
            event -> logger.info(event.getEventType().toString()));
        CheckedFunction<String, String> cachedFunction = Cache
            .decorateCheckedSupplier(cacheContext, () -> "Hello world");
        String value = cachedFunction.apply("testKey");

        assertThat(value).isEqualTo("Hello world");

        then(logger).should(times(1)).info("ERROR");
    }

}
