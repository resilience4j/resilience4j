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
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.metrics.CircuitBreakerMetrics;
import io.github.resilience4j.metrics.RateLimiterMetrics;
import io.github.resilience4j.metrics.RetryMetrics;
import io.github.resilience4j.prometheus.CircuitBreakerExports;
import io.github.resilience4j.prometheus.RateLimiterExports;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreakerMethodInterceptor;
import io.github.resilience4j.ratpack.circuitbreaker.endpoint.CircuitBreakerChain;
import io.github.resilience4j.ratpack.ratelimiter.RateLimiter;
import io.github.resilience4j.ratpack.ratelimiter.RateLimiterMethodInterceptor;
import io.github.resilience4j.ratpack.ratelimiter.endpoint.RateLimiterChain;
import io.github.resilience4j.ratpack.retry.Retry;
import io.github.resilience4j.ratpack.retry.RetryMethodInterceptor;
import io.github.resilience4j.ratpack.retry.endpoint.RetryChain;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.prometheus.client.CollectorRegistry;
import ratpack.api.Nullable;
import ratpack.dropwizard.metrics.DropwizardMetricsModule;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.Handlers;
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
        // interceptors
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CircuitBreaker.class), injected(new CircuitBreakerMethodInterceptor()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(RateLimiter.class), injected(new RateLimiterMethodInterceptor()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Retry.class), injected(new RetryMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(CircuitBreaker.class), Matchers.any(), injected(new CircuitBreakerMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(RateLimiter.class), Matchers.any(), injected(new RateLimiterMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(Retry.class), Matchers.any(), injected(new RetryMethodInterceptor()));

        // default registries
        OptionalBinder.newOptionalBinder(binder(), CircuitBreakerRegistry.class).setDefault().toInstance(CircuitBreakerRegistry.ofDefaults());
        OptionalBinder.newOptionalBinder(binder(), RateLimiterRegistry.class).setDefault().toInstance(RateLimiterRegistry.ofDefaults());
        OptionalBinder.newOptionalBinder(binder(), RetryRegistry.class).setDefault().toInstance(RetryRegistry.ofDefaults());

        // event consumers
        bind(new TypeLiteral<EventConsumerRegistry<CircuitBreakerEvent>>() {}).toInstance(new DefaultEventConsumerRegistry<>());
        bind(new TypeLiteral<EventConsumerRegistry<RateLimiterEvent>>() {}).toInstance(new DefaultEventConsumerRegistry<>());
        bind(new TypeLiteral<EventConsumerRegistry<RetryEvent>>() {}).toInstance(new DefaultEventConsumerRegistry<>());

        // event chains
        Multibinder<HandlerDecorator> binder = Multibinder.newSetBinder(binder(), HandlerDecorator.class);
        bind(CircuitBreakerChain.class).in(Scopes.SINGLETON);
        bind(RateLimiterChain.class).in(Scopes.SINGLETON);
        bind(RetryChain.class).in(Scopes.SINGLETON);
        binder.addBinding().toProvider(() -> (registry, rest) -> {
            if (registry.get(Resilience4jConfig.class).getEndpoints().getCircuitBreakers().isEnabled()) {
                return Handlers.chain(Handlers.chain(registry, registry.get(CircuitBreakerChain.class)), rest);
            } else {
                return rest;
            }
        });
        binder.addBinding().toProvider(() -> (registry, rest) -> {
            if (registry.get(Resilience4jConfig.class).getEndpoints().getRateLimiters().isEnabled()) {
                return Handlers.chain(Handlers.chain(registry, registry.get(RateLimiterChain.class)), rest);
            } else {
                return rest;
            }
        });
        binder.addBinding().toProvider(() -> (registry, rest) -> {
            if (registry.get(Resilience4jConfig.class).getEndpoints().getRetries().isEnabled()) {
                return Handlers.chain(Handlers.chain(registry, registry.get(RetryChain.class)), rest);
            } else {
                return rest;
            }
        });

        // startup
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

            EndpointsConfig endpointsConfig = event.getRegistry().get(Resilience4jConfig.class).getEndpoints();

            // build circuit breakers
            CircuitBreakerRegistry circuitBreakerRegistry = injector.getInstance(CircuitBreakerRegistry.class);
            EventConsumerRegistry<CircuitBreakerEvent> cbConsumerRegistry = injector.getInstance(Key.get(new TypeLiteral<EventConsumerRegistry<CircuitBreakerEvent>>() {}));
            config.getCircuitBreakers().forEach((name, circuitBreakerConfig) -> {
                io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
                if (circuitBreakerConfig.getDefaults()) {
                    circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
                } else {
                    circuitBreaker = circuitBreakerRegistry.circuitBreaker(name, CircuitBreakerConfig.custom()
                            .failureRateThreshold(circuitBreakerConfig.getFailureRateThreshold())
                            .ringBufferSizeInClosedState(circuitBreakerConfig.getRingBufferSizeInClosedState())
                            .ringBufferSizeInHalfOpenState(circuitBreakerConfig.getRingBufferSizeInHalfOpenState())
                            .waitDurationInOpenState(Duration.ofMillis(circuitBreakerConfig.getWaitIntervalInMillis()))
                            .build());
                }
                if (endpointsConfig.getCircuitBreakers().isEnabled()) {
                    circuitBreaker.getEventPublisher().onEvent(cbConsumerRegistry.createEventConsumer(name, endpointsConfig.getCircuitBreakers().getEventConsumerBufferSize()));
                }
            });

            // build rate limiters
            RateLimiterRegistry rateLimiterRegistry = injector.getInstance(RateLimiterRegistry.class);
            EventConsumerRegistry<RateLimiterEvent> rlConsumerRegistry = injector.getInstance(Key.get(new TypeLiteral<EventConsumerRegistry<RateLimiterEvent>>() {}));
            config.getRateLimiters().forEach((name, rateLimiterConfig) -> {
                io.github.resilience4j.ratelimiter.RateLimiter rateLimiter;
                if (rateLimiterConfig.getDefaults()) {
                    rateLimiter = rateLimiterRegistry.rateLimiter(name);
                } else {
                    rateLimiter = rateLimiterRegistry.rateLimiter(name, RateLimiterConfig.custom()
                            .limitForPeriod(rateLimiterConfig.getLimitForPeriod())
                            .limitRefreshPeriod(Duration.ofNanos(rateLimiterConfig.getLimitRefreshPeriodInNanos()))
                            .timeoutDuration(Duration.ofMillis(rateLimiterConfig.getTimeoutInMillis()))
                            .build());
                }
                if (endpointsConfig.getRateLimiters().isEnabled()) {
                    rateLimiter.getEventPublisher().onEvent(rlConsumerRegistry.createEventConsumer(name, endpointsConfig.getRateLimiters().getEventConsumerBufferSize()));
                }
            });

            // build retries
            RetryRegistry retryRegistry = injector.getInstance(RetryRegistry.class);
            EventConsumerRegistry<RetryEvent> rConsumerRegistry = injector.getInstance(Key.get(new TypeLiteral<EventConsumerRegistry<RetryEvent>>() {}));
            config.getRetries().forEach((name, retryConfig) -> {
                io.github.resilience4j.retry.Retry retry;
                if (retryConfig.getDefaults()) {
                    retry = retryRegistry.retry(name);
                } else {
                    retry = retryRegistry.retry(name, RetryConfig.custom()
                            .maxAttempts(retryConfig.getMaxAttempts())
                            .waitDuration(Duration.ofMillis(retryConfig.getWaitDurationInMillis()))
                            .build());
                }
                if (endpointsConfig.getRetries().isEnabled()) {
                    retry.getEventPublisher().onEvent(rConsumerRegistry.createEventConsumer(name, endpointsConfig.getRetries().getEventConsumerBufferSize()));
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
