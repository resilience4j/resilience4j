/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack;

import com.google.inject.matcher.Matchers;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratpack.annotation.CircuitBreaker;
import io.github.resilience4j.ratpack.annotation.RateLimiter;
import io.github.resilience4j.ratpack.internal.CircuitBreakerMethodInterceptor;
import io.github.resilience4j.ratpack.internal.RateLimiterMethodInterceptor;
import ratpack.guice.ConfigurableModule;

public class ResilienceModule extends ConfigurableModule<ResilienceModule.ResilienceConfig> {

    @Override
    protected void configure() {
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CircuitBreaker.class), injected(new CircuitBreakerMethodInterceptor(getProvider(CircuitBreakerRegistry.class))));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(RateLimiter.class), injected(new RateLimiterMethodInterceptor(getProvider(RateLimiterRegistry.class))));
    }

    private <T> T injected(T instance) {
        requestInjection(instance);
        return instance;
    }

    public static class ResilienceConfig {
        private boolean enableMetrics = false;

        public ResilienceConfig enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }

    }
}
