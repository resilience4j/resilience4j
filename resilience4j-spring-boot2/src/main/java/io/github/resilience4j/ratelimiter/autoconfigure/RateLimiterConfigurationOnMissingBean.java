/*
 * Copyright 2018 Slawomir Kowalski
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

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspect;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfiguration;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfigurationOnMissingBean {
    private final RateLimiterConfiguration rateLimiterConfiguration;

    RateLimiterConfigurationOnMissingBean() {
        this.rateLimiterConfiguration = new RateLimiterConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterConfigurationProperties rateLimiterProperties,
                                                   EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
                                                   ConfigurableBeanFactory beanFactory) {
        return rateLimiterConfiguration.rateLimiterRegistry(rateLimiterProperties, rateLimiterEventsConsumerRegistry, beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterAspect rateLimiterAspect(RateLimiterConfigurationProperties rateLimiterProperties, RateLimiterRegistry rateLimiterRegistry) {
        return rateLimiterConfiguration.rateLimiterAspect(rateLimiterProperties, rateLimiterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
        return rateLimiterConfiguration.rateLimiterEventsConsumerRegistry();
    }
}
