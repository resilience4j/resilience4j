/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.autoconfigure;

import static io.github.resilience4j.ratelimiter.autoconfigure.RateLimiterProperties.createRateLimiterConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.internal.InMemoryRateLimiterRegistry;
import io.github.resilience4j.ratelimiter.monitoring.endpoint.RateLimiterEndpoint;
import io.github.resilience4j.ratelimiter.monitoring.endpoint.RateLimiterEventsEndpoint;
import io.github.resilience4j.ratelimiter.monitoring.health.RateLimiterHealthIndicator;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j ratelimiter.
 */
@Configuration
@ConditionalOnClass(RateLimiter.class)
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterAutoConfiguration.class);

    @Bean
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterProperties rateLimiterProperties,
                                                   EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
                                                   ConfigurableBeanFactory beanFactory) {
        RateLimiterRegistry rateLimiterRegistry = new InMemoryRateLimiterRegistry(RateLimiterConfig.ofDefaults());
        rateLimiterProperties.getLimiters().forEach(
            (name, properties) -> {
                RateLimiter rateLimiter = createRateLimiter(rateLimiterRegistry, name, properties);
                if (properties.getSubscribeForEvents()) {
                    subscribeToLimiterEvents(rateLimiterEventsConsumerRegistry, name, properties, rateLimiter);
                }
                if (properties.getRegisterHealthIndicator()) {
                    createHealthIndicatorForLimiter(beanFactory, name, rateLimiter);
                }
            }
        );
        return rateLimiterRegistry;
    }

    @Bean
    public RateLimiterAspect rateLimiterAspect(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterAspect(rateLimiterRegistry);
    }

    @Bean
    public RateLimiterEndpoint rateLimiterEndpoint(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterEndpoint(rateLimiterRegistry);
    }

    @Bean
    public RateLimiterEventsEndpoint rateLimiterEventsEndpoint(EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
                                                               RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterEventsEndpoint(rateLimiterEventsConsumerRegistry, rateLimiterRegistry);
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances.
     * The EventConsumerRegistry is used by the RateLimiterHealthIndicator to show the latest RateLimiterEvents events
     * for each RateLimiter instance.
     */
    @Bean
    public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    private void createHealthIndicatorForLimiter(ConfigurableBeanFactory beanFactory, String name, RateLimiter rateLimiter) {
        beanFactory.registerSingleton(
            name + "HealthIndicator",
            new RateLimiterHealthIndicator(rateLimiter)
        );
    }

    private void subscribeToLimiterEvents(EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry, String name, RateLimiterProperties.LimiterProperties properties, RateLimiter rateLimiter) {
        EventConsumer<RateLimiterEvent> eventConsumer = rateLimiterEventsConsumerRegistry
            .createEventConsumer(name, properties.getEventConsumerBufferSize());
        rateLimiter.getEventStream().subscribe(eventConsumer);

        logger.debug("Autoconfigure subscription for Rate Limiter {}", rateLimiter);
    }

    private RateLimiter createRateLimiter(RateLimiterRegistry rateLimiterRegistry, String name, RateLimiterProperties.LimiterProperties properties) {
        RateLimiter rateLimiter =
            rateLimiterRegistry.rateLimiter(name, createRateLimiterConfig(properties));
        logger.debug("Autoconfigure Rate Limiter registered. {}", rateLimiter);
        return rateLimiter;
    }
}
