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

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.monitoring.endpoint.RateLimiterEndpoint;
import io.github.resilience4j.ratelimiter.monitoring.endpoint.RateLimiterEventsEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j ratelimiter.
 */
@Configuration
@ConditionalOnClass(RateLimiter.class)
@EnableConfigurationProperties(RateLimiterProperties.class)
@Import({RateLimiterConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})
@AutoConfigureBefore(EndpointAutoConfiguration.class)
public class RateLimiterAutoConfiguration {

    @Bean
    @ConditionalOnEnabledEndpoint
    @ConditionalOnClass(value = {Endpoint.class})
    public RateLimiterEndpoint rateLimiterEndpoint(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimiterEndpoint(rateLimiterRegistry);
    }

    @Bean
    @ConditionalOnEnabledEndpoint
    @ConditionalOnClass(value = {Endpoint.class})
    public RateLimiterEventsEndpoint rateLimiterEventsEndpoint(
        EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry) {
        return new RateLimiterEventsEndpoint(eventConsumerRegistry);
    }

}
