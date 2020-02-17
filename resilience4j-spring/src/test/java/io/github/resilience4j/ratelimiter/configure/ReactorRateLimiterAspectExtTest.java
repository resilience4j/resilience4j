/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.vavr.CheckedFunction0;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * aspect unit test
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactorRateLimiterAspectExtTest {

    @Mock
    CheckedFunction0<Object> function;

    @InjectMocks
    ReactorDecoratorExt reactorRateLimiterAspectExt;


    @Test
    public void testCheckTypes() {
        assertThat(reactorRateLimiterAspectExt.canDecorateReturnType(Mono.class)).isTrue();
        assertThat(reactorRateLimiterAspectExt.canDecorateReturnType(Flux.class)).isTrue();
    }

    @Test
    public void testReactorTypes() throws Throwable {
        RateLimiter rateLimiter = RateLimiter.ofDefaults("test");

        when(function.apply()).thenReturn(Mono.just("Test"));
        assertThat(
            reactorRateLimiterAspectExt.decorate(function, rateLimiter, "testMethod").apply())
            .isNotNull();

        when(function.apply()).thenReturn(Flux.just("Test"));
        assertThat(
            reactorRateLimiterAspectExt.decorate(function, rateLimiter, "testMethod").apply())
            .isNotNull();
    }


}