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
package io.github.resilience4j.retry.autoconfigure;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.monitoring.endpoint.RetryEndpoint;
import io.github.resilience4j.retry.monitoring.endpoint.RetryEventsEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j-retry.
 */
@Configuration
@ConditionalOnClass(Retry.class)
@EnableConfigurationProperties(RetryProperties.class)
@Import({RetryConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})
public class RetryAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Endpoint.class)
    static class RetryAutoEndpointConfiguration {

        @Bean
        @ConditionalOnEnabledEndpoint
        public RetryEndpoint retryEndpoint(RetryRegistry retryRegistry) {
            return new RetryEndpoint(retryRegistry);
        }

        @Bean
        @ConditionalOnEnabledEndpoint
        public RetryEventsEndpoint retryEventsEndpoint(
            EventConsumerRegistry<RetryEvent> eventConsumerRegistry) {
            return new RetryEventsEndpoint(eventConsumerRegistry);
        }
    }
}
