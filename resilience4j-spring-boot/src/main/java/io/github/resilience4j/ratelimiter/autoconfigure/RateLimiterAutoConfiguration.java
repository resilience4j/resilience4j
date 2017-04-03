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

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterProperties rateLimiterProperties) {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        rateLimiterProperties.getLimiters().forEach(
            (name, properties) -> {
                io.github.resilience4j.ratelimiter.RateLimiter rateLimiter =
                    rateLimiterRegistry.rateLimiter(name, rateLimiterProperties.createRateLimiterConfig(name));

                logger.debug("Autoconfigure Rate Limiter registered. {}", rateLimiter);
            }
        );
        return rateLimiterRegistry;
    }

    @Bean
    public RateLimiterAspect rateLimiterAspect(RateLimiterProperties rateLimiterProperties,
                                               RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterAspect(rateLimiterProperties, rateLimiterRegistry);
    }

    @Bean
    public RateLimiterEndpoint circuitBreakerEndpoint(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterEndpoint(rateLimiterRegistry);
    }

    @Bean
    public RateLimiterEventsEndpoint circuitBreakerEventsEndpoint(RateLimiterEndpoint rateLimiterEndpoint,
                                                                  EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
                                                                  RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterEventsEndpoint(rateLimiterEndpoint, rateLimiterEventsConsumerRegistry, rateLimiterRegistry);
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances.
     * The EventConsumerRegistry is used by the CircuitBreakerHealthIndicator to show the latest CircuitBreakerEvents events
     * for each CircuitBreaker instance.
     */
    @Bean
    public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }
}
