/*
 * Copyright 2017 Dan Maas, Jan Sykora
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
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CompositeCircuitBreakerCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.CompositeRetryCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.metrics.*;
import io.github.resilience4j.prometheus.collectors.*;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratpack.bulkhead.BulkheadMethodInterceptor;
import io.github.resilience4j.ratpack.bulkhead.monitoring.endpoint.BulkheadChain;
import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreakerMethodInterceptor;
import io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.CircuitBreakerChain;
import io.github.resilience4j.ratpack.ratelimiter.RateLimiterMethodInterceptor;
import io.github.resilience4j.ratpack.ratelimiter.monitoring.endpoint.RateLimiterChain;
import io.github.resilience4j.ratpack.retry.RetryMethodInterceptor;
import io.github.resilience4j.ratpack.retry.monitoring.endpoint.RetryChain;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.prometheus.client.CollectorRegistry;
import ratpack.dropwizard.metrics.DropwizardMetricsModule;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.Handlers;
import ratpack.registry.Registry;
import ratpack.service.Service;
import ratpack.service.StartEvent;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This module registers class and method interceptors for bulkheads, circuit breakers, rate
 * limiters, and retries.
 * <p>
 * This module also registers metrics: - bulkhead, circuitbreaker, ratelimiter, and retry metrics
 * with dropwizard metrics, if enabled. - circuitbreaker, ratelimiter, and retry metrics with
 * prometheus, if enabled.
 * <p>
 * Only enable metrics if you have dependencies for resilience4j-metrics in the classpath and an
 * instance of {@link MetricRegistry} is bound (usually this will happen when installing {@link
 * DropwizardMetricsModule}). This must be done manually, since guice doesn't know if dropwizard is
 * on the runtime classpath.
 * <p>
 * Only enable prometheus if you have a dependency on resilience4j-prometheus in the classpath and
 * an instance of {@link CollectorRegistry} is bound. This must be done manually, since guice
 * doesn't know if prometheus is on the runtime classpath.
 * <p>
 * Also note that for this to work, CircuitBreaker, RateLimiter, and Retry instances must be created
 * before the respective registries are bound.
 */
public class Resilience4jModule extends ConfigurableModule<Resilience4jConfig> {

    @Override
    protected void configure() {
        // interceptors
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CircuitBreaker.class),
            injected(new CircuitBreakerMethodInterceptor()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(RateLimiter.class),
            injected(new RateLimiterMethodInterceptor()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Retry.class),
            injected(new RetryMethodInterceptor()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Bulkhead.class),
            injected(new BulkheadMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(CircuitBreaker.class), Matchers.any(),
            injected(new CircuitBreakerMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(RateLimiter.class), Matchers.any(),
            injected(new RateLimiterMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(Retry.class), Matchers.any(),
            injected(new RetryMethodInterceptor()));
        bindInterceptor(Matchers.annotatedWith(Bulkhead.class), Matchers.any(),
            injected(new BulkheadMethodInterceptor()));

        // default registries
        OptionalBinder.newOptionalBinder(binder(), CircuitBreakerRegistry.class).setDefault()
            .toProvider(CircuitBreakerRegistryProvider.class).in(Scopes.SINGLETON);
        OptionalBinder.newOptionalBinder(binder(), RateLimiterRegistry.class).setDefault()
            .toProvider(RateLimiterRegistryProvider.class).in(Scopes.SINGLETON);
        OptionalBinder.newOptionalBinder(binder(), RetryRegistry.class).setDefault()
            .toProvider(RetryRegistryProvider.class).in(Scopes.SINGLETON);
        OptionalBinder.newOptionalBinder(binder(), BulkheadRegistry.class).setDefault()
            .toProvider(BulkheadRegistryProvider.class).in(Scopes.SINGLETON);
        OptionalBinder.newOptionalBinder(binder(), ThreadPoolBulkheadRegistry.class).setDefault()
            .toProvider(ThreadPoolBulkheadRegistryProvider.class).in(Scopes.SINGLETON);

        // event consumers
        bind(new TypeLiteral<EventConsumerRegistry<CircuitBreakerEvent>>() {
        }).toInstance(new DefaultEventConsumerRegistry<>());
        bind(new TypeLiteral<EventConsumerRegistry<RateLimiterEvent>>() {
        }).toInstance(new DefaultEventConsumerRegistry<>());
        bind(new TypeLiteral<EventConsumerRegistry<RetryEvent>>() {
        }).toInstance(new DefaultEventConsumerRegistry<>());
        bind(new TypeLiteral<EventConsumerRegistry<BulkheadEvent>>() {
        }).toInstance(new DefaultEventConsumerRegistry<>());

        // event chains
        Multibinder<HandlerDecorator> binder = Multibinder
            .newSetBinder(binder(), HandlerDecorator.class);
        bind(CircuitBreakerChain.class).in(Scopes.SINGLETON);
        bind(RateLimiterChain.class).in(Scopes.SINGLETON);
        bind(RetryChain.class).in(Scopes.SINGLETON);
        bind(BulkheadChain.class).in(Scopes.SINGLETON);
        binder.addBinding().toProvider(() -> (registry, rest) -> {
            if (registry.get(Resilience4jConfig.class).getEndpoints().getCircuitbreaker()
                .isEnabled()) {
                return Handlers
                    .chain(Handlers.chain(registry, registry.get(CircuitBreakerChain.class)), rest);
            } else {
                return rest;
            }
        });
        binder.addBinding().toProvider(() -> (registry, rest) -> {
            if (registry.get(Resilience4jConfig.class).getEndpoints().getRatelimiter()
                .isEnabled()) {
                return Handlers
                    .chain(Handlers.chain(registry, registry.get(RateLimiterChain.class)), rest);
            } else {
                return rest;
            }
        });
        binder.addBinding().toProvider(() -> (registry, rest) -> {
            if (registry.get(Resilience4jConfig.class).getEndpoints().getRetry().isEnabled()) {
                return Handlers
                    .chain(Handlers.chain(registry, registry.get(RetryChain.class)), rest);
            } else {
                return rest;
            }
        });
        binder.addBinding().toProvider(() -> (registry, rest) -> {
            if (registry.get(Resilience4jConfig.class).getEndpoints().getBulkhead().isEnabled()) {
                return Handlers
                    .chain(Handlers.chain(registry, registry.get(BulkheadChain.class)), rest);
            } else {
                return rest;
            }
        });

        // startup
        bind(Resilience4jService.class).in(Scopes.SINGLETON);
    }

    private <T> T injected(T instance) {
        requestInjection(instance);
        return instance;
    }

    private static class CircuitBreakerRegistryProvider implements
        Provider<CircuitBreakerRegistry> {

        private Resilience4jConfig resilience4jConfig;
        private EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry;

        @Inject
        public CircuitBreakerRegistryProvider(Resilience4jConfig resilience4jConfig,
            EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
            this.resilience4jConfig = resilience4jConfig;
            this.eventConsumerRegistry = eventConsumerRegistry;
        }

        @Override
        public CircuitBreakerRegistry get() {
            // build configs
            CircuitBreakerConfigurationProperties circuitBreakerProperties = resilience4jConfig
                .getCircuitbreaker();
            Map<String, CircuitBreakerConfig> configs = circuitBreakerProperties.getConfigs()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> circuitBreakerProperties
                        .createCircuitBreakerConfig(entry.getKey(), entry.getValue(),
                            new CompositeCircuitBreakerCustomizer(Collections.emptyList()))));
            CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(configs);

            // build circuit breakers
            EndpointsConfig endpointsConfig = resilience4jConfig.getEndpoints();
            circuitBreakerProperties.getInstances().forEach((name, circuitBreakerConfig) -> {
                io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                    circuitBreakerRegistry.circuitBreaker(name,
                        circuitBreakerProperties.createCircuitBreakerConfig(name,
                            circuitBreakerConfig,
                            new CompositeCircuitBreakerCustomizer(Collections.emptyList())));
                if (endpointsConfig.getCircuitbreaker().isEnabled()) {
                    circuitBreaker.getEventPublisher().onEvent(eventConsumerRegistry
                        .createEventConsumer(name,
                            circuitBreakerConfig.getEventConsumerBufferSize() != null
                                ? circuitBreakerConfig.getEventConsumerBufferSize() : 100));
                }
            });

            return circuitBreakerRegistry;
        }
    }

    private static class RateLimiterRegistryProvider implements Provider<RateLimiterRegistry> {

        private Resilience4jConfig resilience4jConfig;
        private EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry;

        @Inject
        public RateLimiterRegistryProvider(Resilience4jConfig resilience4jConfig,
            EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry) {
            this.resilience4jConfig = resilience4jConfig;
            this.eventConsumerRegistry = eventConsumerRegistry;
        }

        @Override
        public RateLimiterRegistry get() {
            // build configs
            RateLimiterConfigurationProperties rateLimiterProperties = resilience4jConfig
                .getRatelimiter();
            Map<String, RateLimiterConfig> configs = rateLimiterProperties.getConfigs()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> rateLimiterProperties.createRateLimiterConfig(entry.getValue())));
            RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(configs);

            // build ratelimiters
            EndpointsConfig endpointsConfig = resilience4jConfig.getEndpoints();
            rateLimiterProperties.getInstances().forEach((name, rateLimiterConfig) -> {
                io.github.resilience4j.ratelimiter.RateLimiter rateLimiter =
                    rateLimiterRegistry.rateLimiter(name,
                        rateLimiterProperties.createRateLimiterConfig(rateLimiterConfig));
                if (endpointsConfig.getRatelimiter().isEnabled()) {
                    rateLimiter.getEventPublisher().onEvent(eventConsumerRegistry
                        .createEventConsumer(name,
                            rateLimiterConfig.getEventConsumerBufferSize() != null
                                ? rateLimiterConfig.getEventConsumerBufferSize() : 100));
                }
            });

            return rateLimiterRegistry;
        }
    }

    private static class RetryRegistryProvider implements Provider<RetryRegistry> {

        private Resilience4jConfig resilience4jConfig;
        private EventConsumerRegistry<RetryEvent> eventConsumerRegistry;

        @Inject
        public RetryRegistryProvider(Resilience4jConfig resilience4jConfig,
            EventConsumerRegistry<RetryEvent> eventConsumerRegistry) {
            this.resilience4jConfig = resilience4jConfig;
            this.eventConsumerRegistry = eventConsumerRegistry;
        }

        @Override
        public RetryRegistry get() {
            // build configs
            RetryConfigurationProperties RetryProperties = resilience4jConfig.getRetry();
            Map<String, RetryConfig> configs = RetryProperties.getConfigs()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> RetryProperties.createRetryConfig(entry.getValue(),
                        new CompositeRetryCustomizer(Collections.emptyList()), entry.getKey())));
            RetryRegistry retryRegistry = RetryRegistry.of(configs);

            // build retries
            EndpointsConfig endpointsConfig = resilience4jConfig.getEndpoints();
            RetryProperties.getInstances().forEach((name, retryConfig) -> {
                io.github.resilience4j.retry.Retry retry =
                    retryRegistry.retry(name, RetryProperties.createRetryConfig(retryConfig,
                        new CompositeRetryCustomizer(Collections.emptyList()), name));
                if (endpointsConfig.getRetry().isEnabled()) {
                    retry.getEventPublisher().onEvent(eventConsumerRegistry
                        .createEventConsumer(name,
                            retryConfig.getEventConsumerBufferSize() != null ? retryConfig
                                .getEventConsumerBufferSize() : 100));
                }
            });

            return retryRegistry;
        }
    }

    private static class BulkheadRegistryProvider implements Provider<BulkheadRegistry> {

        private Resilience4jConfig resilience4jConfig;
        private EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry;

        @Inject
        public BulkheadRegistryProvider(Resilience4jConfig resilience4jConfig,
            EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
            this.resilience4jConfig = resilience4jConfig;
            this.eventConsumerRegistry = eventConsumerRegistry;
        }

        @Override
        public BulkheadRegistry get() {
            // build configs
            BulkheadConfigurationProperties bulkheadProperties = resilience4jConfig.getBulkhead();
            Map<String, BulkheadConfig> configs = bulkheadProperties.getConfigs()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> bulkheadProperties.createBulkheadConfig(entry.getValue())));
            BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(configs);

            // build bulkheads
            EndpointsConfig endpointsConfig = resilience4jConfig.getEndpoints();
            bulkheadProperties.getInstances().forEach((name, bulkheadConfig) -> {
                io.github.resilience4j.bulkhead.Bulkhead bulkhead =
                    bulkheadRegistry
                        .bulkhead(name, bulkheadProperties.createBulkheadConfig(bulkheadConfig));
                if (endpointsConfig.getBulkhead().isEnabled()) {
                    bulkhead.getEventPublisher().onEvent(eventConsumerRegistry
                        .createEventConsumer(name,
                            bulkheadConfig.getEventConsumerBufferSize() != null ? bulkheadConfig
                                .getEventConsumerBufferSize() : 100));
                }
            });

            return bulkheadRegistry;
        }
    }

    private static class ThreadPoolBulkheadRegistryProvider implements
        Provider<ThreadPoolBulkheadRegistry> {

        private Resilience4jConfig resilience4jConfig;
        private EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry;

        @Inject
        public ThreadPoolBulkheadRegistryProvider(Resilience4jConfig resilience4jConfig,
            EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
            this.resilience4jConfig = resilience4jConfig;
            this.eventConsumerRegistry = eventConsumerRegistry;
        }

        @Override
        public ThreadPoolBulkheadRegistry get() {
            // build configs
            ThreadPoolBulkheadConfigurationProperties threadPoolBulkheadProperties = resilience4jConfig
                .getThreadpoolbulkhead();
            Map<String, ThreadPoolBulkheadConfig> configs = threadPoolBulkheadProperties
                .getConfigs()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> threadPoolBulkheadProperties
                        .createThreadPoolBulkheadConfig(entry.getValue())));
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry
                .of(configs);

            // build threadpool bulkheads
            EndpointsConfig endpointsConfig = resilience4jConfig.getEndpoints();
            threadPoolBulkheadProperties.getInstances()
                .forEach((name, threadPoolBulkheadConfig) -> {
                    io.github.resilience4j.bulkhead.ThreadPoolBulkhead threadPoolBulkhead =
                        threadPoolBulkheadRegistry.bulkhead(name, threadPoolBulkheadProperties
                            .createThreadPoolBulkheadConfig(threadPoolBulkheadConfig));
                    if (endpointsConfig.getThreadpoolbulkhead().isEnabled()) {
                        threadPoolBulkhead.getEventPublisher().onEvent(eventConsumerRegistry
                            .createEventConsumer(name,
                                threadPoolBulkheadConfig.getEventConsumerBufferSize() != null
                                    ? threadPoolBulkheadConfig.getEventConsumerBufferSize() : 100));
                    }
                });

            return threadPoolBulkheadRegistry;
        }
    }

    private static class Resilience4jService implements Service {

        @Override
        public void onStart(StartEvent event) {
            Registry registry = event.getRegistry();
            Resilience4jConfig resilience4jConfig = registry.get(Resilience4jConfig.class);
            CircuitBreakerRegistry circuitBreakerRegistry = registry
                .get(CircuitBreakerRegistry.class);
            RateLimiterRegistry rateLimiterRegistry = registry.get(RateLimiterRegistry.class);
            RetryRegistry retryRegistry = registry.get(RetryRegistry.class);
            BulkheadRegistry bulkheadRegistry = registry.get(BulkheadRegistry.class);
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = registry
                .get(ThreadPoolBulkheadRegistry.class);

            // dropwizard metrics
            if (resilience4jConfig.isMetrics() && registry.maybeGet(MetricRegistry.class)
                .isPresent()) {
                MetricRegistry metricRegistry = registry.get(MetricRegistry.class);
                CircuitBreakerMetrics
                    .ofCircuitBreakerRegistry(circuitBreakerRegistry, metricRegistry);
                RateLimiterMetrics.ofRateLimiterRegistry(rateLimiterRegistry, metricRegistry);
                RetryMetrics.ofRetryRegistry(retryRegistry, metricRegistry);
                BulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry, metricRegistry);
                ThreadPoolBulkheadMetrics
                    .ofBulkheadRegistry(threadPoolBulkheadRegistry, metricRegistry);
            }

            // prometheus
            if (resilience4jConfig.isPrometheus() && registry.maybeGet(CollectorRegistry.class)
                .isPresent()) {
                CollectorRegistry collectorRegistry = registry.get(CollectorRegistry.class);
                CircuitBreakerMetricsCollector circuitBreakerMetricsCollector = CircuitBreakerMetricsCollector
                    .ofCircuitBreakerRegistry(circuitBreakerRegistry);
                RetryMetricsCollector retryMetricsCollector = RetryMetricsCollector
                    .ofRetryRegistry(retryRegistry);
                RateLimiterMetricsCollector rateLimiterMetricsCollector = RateLimiterMetricsCollector
                    .ofRateLimiterRegistry(rateLimiterRegistry);
                BulkheadMetricsCollector bulkheadMetricsCollector = BulkheadMetricsCollector
                    .ofBulkheadRegistry(bulkheadRegistry);
                ThreadPoolBulkheadMetricsCollector threadPoolBulkheadMetricsCollector = ThreadPoolBulkheadMetricsCollector
                    .ofBulkheadRegistry(threadPoolBulkheadRegistry);
                circuitBreakerMetricsCollector.register(collectorRegistry);
                retryMetricsCollector.register(collectorRegistry);
                rateLimiterMetricsCollector.register(collectorRegistry);
                bulkheadMetricsCollector.register(collectorRegistry);
                threadPoolBulkheadMetricsCollector.register(collectorRegistry);
            }
        }
    }

}
