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

import io.vavr.CheckedFunction1;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.xml.ws.WebServiceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

public class CacheEventConsumerTest {

    private javax.cache.Cache<String, String> cache;
    private Logger logger;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp(){
        cache = mock(javax.cache.Cache.class);
        logger = mock(Logger.class);
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        Cache<String, String> cacheContext = Cache.of(cache);
        Cache.EventConsumer eventConsumer = cacheContext.getEventConsumer();
        Cache.EventConsumer eventConsumer2 = cacheContext.getEventConsumer();

        assertThat(eventConsumer).isEqualTo(eventConsumer2);
    }

    @Test
    public void shouldConsumeOnCacheHitEvent() throws Throwable {
        // Given the cache does not contain the key
        given(cache.get("testKey")).willReturn("Hello world");

        Cache<String, String> cacheContext = Cache.of(cache);
        cacheContext.getEventConsumer().onCacheHit(event ->
                logger.info(event.getEventType().toString()));

        CheckedFunction1<String, String> cachedFunction = Cache.decorateCheckedSupplier(cacheContext, () -> "Hello world");
        String value = cachedFunction.apply("testKey");
        assertThat(value).isEqualTo("Hello world");

        then(logger).should(times(1)).info("CACHE_HIT");
    }

    @Test
    public void shouldConsumeOnCacheMissEvent() throws Throwable {
        // Given the cache does not contain the key
        given(cache.get("testKey")).willReturn(null);

        Cache<String, String> cacheContext = Cache.of(cache);
        cacheContext.getEventConsumer().onCacheMiss(event ->
                logger.info(event.getEventType().toString()));

        CheckedFunction1<String, String> cachedFunction = Cache.decorateCheckedSupplier(cacheContext, () -> "Hello world");
        String value = cachedFunction.apply("testKey");
        assertThat(value).isEqualTo("Hello world");

        then(logger).should(times(1)).info("CACHE_MISS");
    }

    @Test
    public void shouldConsumeOnErrorEvent() throws Throwable {
        // Given the cache does not contain the key
        given(cache.get("testKey")).willThrow(new WebServiceException("BLA"));

        Cache<String, String> cacheContext = Cache.of(cache);
        cacheContext.getEventConsumer().onError(event ->
                logger.info(event.getEventType().toString()));

        CheckedFunction1<String, String> cachedFunction = Cache.decorateCheckedSupplier(cacheContext, () -> "Hello world");
        String value = cachedFunction.apply("testKey");
        assertThat(value).isEqualTo("Hello world");

        then(logger).should(times(1)).info("ERROR");
    }


}
