/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.monitoring.endpoint.BulkheadEndpoint;
import io.github.resilience4j.bulkhead.monitoring.endpoint.BulkheadEventsEndpoint;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j-bulkhead.
 */
@Configuration
@ConditionalOnClass(Bulkhead.class)
@EnableConfigurationProperties({BulkheadProperties.class, ThreadPoolBulkheadProperties.class})
@Import({BulkheadConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})
public class BulkheadAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Endpoint.class)
    static class BulkheadEndpointAutoConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public BulkheadEndpoint bulkheadEndpoint(BulkheadRegistry bulkheadRegistry,
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry) {
            return new BulkheadEndpoint(bulkheadRegistry, threadPoolBulkheadRegistry);
        }

        @Bean
        @ConditionalOnAvailableEndpoint
        public BulkheadEventsEndpoint bulkheadEventsEndpoint(
            EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
            return new BulkheadEventsEndpoint(eventConsumerRegistry);
        }
    }
}
