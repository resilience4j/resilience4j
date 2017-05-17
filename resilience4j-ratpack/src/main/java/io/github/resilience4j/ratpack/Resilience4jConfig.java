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

import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratpack.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratpack.retry.RetryConfig;
import ratpack.func.Function;

import java.util.HashMap;
import java.util.Map;

import static ratpack.util.Exceptions.uncheck;

public class Resilience4jConfig {
    private Map<String, CircuitBreakerConfig> circuitBreakers = new HashMap<>();
    private Map<String, RateLimiterConfig> rateLimiters = new HashMap<>();
    private Map<String, RetryConfig> retries = new HashMap<>();
    private boolean metrics = false;
    private boolean prometheus = false;

    public Resilience4jConfig circuitBreaker(String name, Function<? super CircuitBreakerConfig, ? extends CircuitBreakerConfig> configure) {
        try {
            CircuitBreakerConfig finalConfig = configure.apply(new CircuitBreakerConfig());
            circuitBreakers.put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig rateLimiter(String name, Function<? super RateLimiterConfig, ? extends RateLimiterConfig> configure) {
        try {
            RateLimiterConfig finalConfig = configure.apply(new RateLimiterConfig());
            rateLimiters.put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig retry(String name, Function<? super RetryConfig, ? extends RetryConfig> configure) {
        try {
            RetryConfig finalConfig = configure.apply(new RetryConfig());
            retries.put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig metrics(boolean metrics) {
        this.metrics = metrics;
        return this;
    }

    public Resilience4jConfig prometheus(boolean prometheus) {
        this.prometheus = prometheus;
        return this;
    }

    public Map<String, CircuitBreakerConfig> getCircuitBreakers() {
        return circuitBreakers;
    }

    public Map<String, RateLimiterConfig> getRateLimiters() {
        return rateLimiters;
    }

    public Map<String, RetryConfig> getRetries() {
        return retries;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public boolean isPrometheus() {
        return prometheus;
    }
}
