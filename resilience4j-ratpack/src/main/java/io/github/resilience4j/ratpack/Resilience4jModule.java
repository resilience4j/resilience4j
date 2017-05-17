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

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.OptionalBinder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.CircuitBreakerMetrics;
import io.github.resilience4j.metrics.RateLimiterMetrics;
import io.github.resilience4j.metrics.RetryMetrics;
import io.github.resilience4j.prometheus.CircuitBreakerExports;
import io.github.resilience4j.prometheus.RateLimiterExports;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreakerMethodInterceptor;
import io.github.resilience4j.ratpack.ratelimiter.RateLimiter;
import io.github.resilience4j.ratpack.ratelimiter.RateLimiterMethodInterceptor;
import io.github.resilience4j.ratpack.retry.Retry;
import io.github.resilience4j.ratpack.retry.RetryMethodInterceptor;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.prometheus.client.CollectorRegistry;
import ratpack.api.Nullable;
import ratpack.dropwizard.metrics.DropwizardMetricsModule;
import ratpack.guice.ConfigurableModule;
import ratpack.service.Service;
import ratpack.service.StartEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;

/**
 * This module registers class and method interceptors for circuit breakers, rate limiters, and retries.
 * <p>
 * This module also registers metrics:
 * - circuitbreaker, ratelimiter, and retry metrics with dropwizard metrics, if enabled.
 * - circuitbreaker, ratelimiter, and retry metrics with prometheus, if enabled.
 * <p>
 * Only enable metrics if you have dependencies for resilience4j-metrics in the classpath and an instance of
 * {@link MetricRegistry} is bound (usually this will happen when installing {@link DropwizardMetricsModule}).
 * This must be done manually, since guice doesn't know if dropwizard is on the runtime classpath.
 * <p>
 * Only enable prometheus if you have a dependency on resilience4j-prometheus in the classpath and an instance of
 * {@link CollectorRegistry} is bound. This must be done manually, since guice doesn't know if prometheus is on the runtime classpath.
 * <p>
 * Also note that for this to work, CircuitBreaker, RateLimiter, and Retry instances must be created
 * before the respective registries are bound.
 */
public class Resilience4jModule extends ConfigurableModule<Resilience4jConfig> {

    @Override
    protected void configure() {
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CircuitBreaker.class), injected(new CircuitBreakerMethodInterceptor()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(RateLimiter.class), injected(new RateLimiterMethodInterceptor()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Retry.class), injected(new RetryMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(CircuitBreaker.class), Matchers.any(), injected(new CircuitBreakerMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(RateLimiter.class), Matchers.any(), injected(new RateLimiterMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(Retry.class), Matchers.any(), injected(new RetryMethodInterceptor()));

        OptionalBinder.newOptionalBinder(binder(), CircuitBreakerRegistry.class).setDefault().toInstance(CircuitBreakerRegistry.ofDefaults());
        OptionalBinder.newOptionalBinder(binder(), RateLimiterRegistry.class).setDefault().toInstance(RateLimiterRegistry.ofDefaults());
        OptionalBinder.newOptionalBinder(binder(), RetryRegistry.class).setDefault().toInstance(RetryRegistry.ofDefaults());

        bind(Resilience4jService.class);
    }

    private <T> T injected(T instance) {
        requestInjection(instance);
        return instance;
    }

    @Provides
    @Singleton
    @Nullable
    public CircuitBreakerMetrics circuitBreakerMetrics(CircuitBreakerRegistry circuitBreakerRegistry, Resilience4jConfig config) {
        if (config.isMetrics()) {
            return CircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        } else {
            return null;
        }
    }

    @Provides
    @Singleton
    @Nullable
    public RateLimiterMetrics rateLimiterMetrics(RateLimiterRegistry rateLimiterRegistry, Resilience4jConfig config) {
        if (config.isMetrics()) {
            return RateLimiterMetrics.ofRateLimiterRegistry(rateLimiterRegistry);
        } else {
            return null;
        }
    }

    @Provides
    @Singleton
    @Nullable
    public RetryMetrics retryMetrics(RetryRegistry retryRegistry, Resilience4jConfig config) {
        if (config.isMetrics()) {
            return RetryMetrics.ofRetryRegistry(retryRegistry);
        } else {
            return null;
        }
    }

    @Provides
    @Singleton
    @Nullable
    public CircuitBreakerExports circuitBreakerExports(CircuitBreakerRegistry circuitBreakerRegistry, Resilience4jConfig config) {
        if (config.isPrometheus()) {
            return CircuitBreakerExports.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        } else {
            return null;
        }
    }

    @Provides
    @Singleton
    @Nullable
    public RateLimiterExports rateLimiterExports(RateLimiterRegistry rateLimiterRegistry, Resilience4jConfig config) {
        if (config.isPrometheus()) {
            return RateLimiterExports.ofRateLimiterRegistry(rateLimiterRegistry);
        } else {
            return null;
        }
    }

    private static class Resilience4jService implements Service {
        private final Injector injector;
        private final Resilience4jConfig config;

        @Inject
        public Resilience4jService(Injector injector, Resilience4jConfig config) {
            this.injector = injector;
            this.config = config;
        }

        @Override
        public void onStart(StartEvent event) throws Exception {

            // build circuit breakers
            CircuitBreakerRegistry circuitBreakerRegistry = injector.getInstance(CircuitBreakerRegistry.class);
            config.getCircuitBreakers().forEach((name, circuitBreakerConfig) -> {
                if (circuitBreakerConfig.getDefaults()) {
                    circuitBreakerRegistry.circuitBreaker(name);
                } else {
                    circuitBreakerRegistry.circuitBreaker(name, CircuitBreakerConfig.custom()
                            .failureRateThreshold(circuitBreakerConfig.getFailureRateThreshold())
                            .ringBufferSizeInClosedState(circuitBreakerConfig.getRingBufferSizeInClosedState())
                            .ringBufferSizeInHalfOpenState(circuitBreakerConfig.getRingBufferSizeInHalfOpenState())
                            .waitDurationInOpenState(Duration.ofMillis(circuitBreakerConfig.getWaitIntervalInMillis()))
                            .build());
                }
            });

            // build rate limiters
            RateLimiterRegistry rateLimiterRegistry = injector.getInstance(RateLimiterRegistry.class);
            config.getRateLimiters().forEach((name, rateLimiterConfig) -> {
                if (rateLimiterConfig.getDefaults()) {
                    rateLimiterRegistry.rateLimiter(name);
                } else {
                    rateLimiterRegistry.rateLimiter(name, RateLimiterConfig.custom()
                            .limitForPeriod(rateLimiterConfig.getLimitForPeriod())
                            .limitRefreshPeriod(Duration.ofNanos(rateLimiterConfig.getLimitRefreshPeriodInNanos()))
                            .timeoutDuration(Duration.ofMillis(rateLimiterConfig.getTimeoutInMillis()))
                            .build());
                }
            });

            // build retries
            RetryRegistry retryRegistry = injector.getInstance(RetryRegistry.class);
            config.getRetries().forEach((name, retryConfig) -> {
                if (retryConfig.getDefaults()) {
                    retryRegistry.retry(name);
                } else {
                    retryRegistry.retry(name, RetryConfig.custom()
                            .maxAttempts(retryConfig.getMaxAttempts())
                            .waitDuration(Duration.ofMillis(retryConfig.getWaitDurationInMillis()))
                            .build());
                }
            });

            // dropwizard metrics
            if (config.isMetrics() && injector.getExistingBinding(Key.get(MetricRegistry.class)) != null) {
                MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
                metricRegistry.registerAll(injector.getInstance(CircuitBreakerMetrics.class));
                metricRegistry.registerAll(injector.getInstance(RateLimiterMetrics.class));
                metricRegistry.registerAll(injector.getInstance(RetryMetrics.class));
            }

            // prometheus
            if (config.isPrometheus() && injector.getExistingBinding(Key.get(CollectorRegistry.class)) != null) {
                CollectorRegistry collectorRegistry = injector.getInstance(CollectorRegistry.class);
                injector.getInstance(CircuitBreakerExports.class).register(collectorRegistry);
                injector.getInstance(RateLimiterExports.class).register(collectorRegistry);
            }

        }

    }

}
