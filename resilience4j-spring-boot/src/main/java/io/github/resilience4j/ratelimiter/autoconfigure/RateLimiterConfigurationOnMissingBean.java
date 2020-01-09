/*
 * Copyright 2019 Mahmoud Romeh
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

import io.github.resilience4j.common.IntegerToDurationConverter;
import io.github.resilience4j.common.StringToDurationConverter;
import io.github.resilience4j.common.ratelimiter.configuration.CompositeRateLimiterCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@Import({IntegerToDurationConverter.class, StringToDurationConverter.class})
public class RateLimiterConfigurationOnMissingBean extends
    AbstractRateLimiterConfigurationOnMissingBean {

    @Bean
    public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
        return rateLimiterConfiguration.rateLimiterEventsConsumerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public CompositeRateLimiterCustomizer compositeRateLimiterCustomizer(
        @Autowired(required = false)
            List<RateLimiterConfigCustomizer> configCustomizers) {
        return new CompositeRateLimiterCustomizer(configCustomizers);
    }
}
