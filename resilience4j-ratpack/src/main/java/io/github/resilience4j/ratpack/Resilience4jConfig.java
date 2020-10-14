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

import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import ratpack.func.Function;

import static ratpack.util.Exceptions.uncheck;

public class Resilience4jConfig {

    private BulkheadConfigurationProperties bulkhead = new BulkheadConfigurationProperties();
    // ratpack does not have the ability to name things with "-", so we must use threadpoolbulkhead instead of thread-pool-bulkhead
    private ThreadPoolBulkheadConfigurationProperties threadpoolbulkhead = new ThreadPoolBulkheadConfigurationProperties();
    private CircuitBreakerConfigurationProperties circuitbreaker = new CircuitBreakerConfigurationProperties();
    private RateLimiterConfigurationProperties ratelimiter = new RateLimiterConfigurationProperties();
    private RetryConfigurationProperties retry = new RetryConfigurationProperties();
    private TimeLimiterConfigurationProperties timeLimiter = new TimeLimiterConfigurationProperties();
    private boolean metrics = false;
    private boolean prometheus = false;
    private EndpointsConfig endpoints = new EndpointsConfig();

    public Resilience4jConfig circuitBreaker(String name) {
        return circuitBreaker(name, config -> config);
    }

    public Resilience4jConfig circuitBreaker(String name,
        Function<? super CircuitBreakerConfigurationProperties.InstanceProperties, ? extends CircuitBreakerConfigurationProperties.InstanceProperties> configure) {
        try {
            CircuitBreakerConfigurationProperties.InstanceProperties finalConfig = configure
                .apply(new CircuitBreakerConfigurationProperties.InstanceProperties());
            circuitbreaker.getInstances().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig rateLimiter(String name) {
        return rateLimiter(name, config -> config);
    }

    public Resilience4jConfig rateLimiter(String name,
        Function<? super RateLimiterConfigurationProperties.InstanceProperties, ? extends RateLimiterConfigurationProperties.InstanceProperties> configure) {
        try {
            RateLimiterConfigurationProperties.InstanceProperties finalConfig = configure
                .apply(new RateLimiterConfigurationProperties.InstanceProperties());
            ratelimiter.getInstances().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig retry(String name) {
        return retry(name, config -> config);
    }

    public Resilience4jConfig retry(String name,
        Function<? super RetryConfigurationProperties.InstanceProperties, ? extends RetryConfigurationProperties.InstanceProperties> configure) {
        try {
            RetryConfigurationProperties.InstanceProperties finalConfig = configure
                .apply(new RetryConfigurationProperties.InstanceProperties());
            retry.getInstances().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig bulkhead(String name) {
        return bulkhead(name, config -> config);
    }

    public Resilience4jConfig bulkhead(String name,
        Function<? super BulkheadConfigurationProperties.InstanceProperties, ? extends BulkheadConfigurationProperties.InstanceProperties> configure) {
        try {
            BulkheadConfigurationProperties.InstanceProperties finalConfig = configure
                .apply(new BulkheadConfigurationProperties.InstanceProperties());
            bulkhead.getInstances().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig threadPoolBulkhead(String name) {
        return threadPoolBulkhead(name, config -> config);
    }

    public Resilience4jConfig threadPoolBulkhead(String name,
        Function<? super ThreadPoolBulkheadConfigurationProperties.InstanceProperties, ? extends ThreadPoolBulkheadConfigurationProperties.InstanceProperties> configure) {
        try {
            ThreadPoolBulkheadConfigurationProperties.InstanceProperties finalConfig = configure
                .apply(new ThreadPoolBulkheadConfigurationProperties.InstanceProperties());
            threadpoolbulkhead.getInstances().put(name, finalConfig);
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public Resilience4jConfig timeLimiter(String name) {
        return timeLimiter(name, config -> config);
    }

    public Resilience4jConfig timeLimiter(String name,
                                          Function<? super TimeLimiterConfigurationProperties.InstanceProperties, ? extends TimeLimiterConfigurationProperties.InstanceProperties> configure) {
        try {
            TimeLimiterConfigurationProperties.InstanceProperties finalConfig = configure
                .apply(new TimeLimiterConfigurationProperties.InstanceProperties());
            timeLimiter.getInstances().put(name, finalConfig);
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

    public Resilience4jConfig endpoints(
        Function<? super EndpointsConfig, ? extends EndpointsConfig> configure) {
        try {
            endpoints = configure.apply(new EndpointsConfig());
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public CircuitBreakerConfigurationProperties getCircuitbreaker() {
        return circuitbreaker;
    }

    public RateLimiterConfigurationProperties getRatelimiter() {
        return ratelimiter;
    }

    public RetryConfigurationProperties getRetry() {
        return retry;
    }

    public BulkheadConfigurationProperties getBulkhead() {
        return bulkhead;
    }

    public ThreadPoolBulkheadConfigurationProperties getThreadpoolbulkhead() {
        return threadpoolbulkhead;
    }

    public TimeLimiterConfigurationProperties getTimeLimiter() {
        return timeLimiter;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public boolean isPrometheus() {
        return prometheus;
    }

    public EndpointsConfig getEndpoints() {
        return endpoints;
    }
}
