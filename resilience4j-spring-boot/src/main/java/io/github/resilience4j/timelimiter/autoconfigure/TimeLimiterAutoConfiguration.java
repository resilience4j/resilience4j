/*
 * Copyright 2020 Ingyu Hwang
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

package io.github.resilience4j.timelimiter.autoconfigure;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.monitoring.endpoint.TimeLimiterEndpoint;
import io.github.resilience4j.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpoint;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(TimeLimiter.class)
@EnableConfigurationProperties(TimeLimiterProperties.class)
@Import(TimeLimiterConfigurationOnMissingBean.class)
@AutoConfigureBefore(EndpointAutoConfiguration.class)
public class TimeLimiterAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Endpoint.class)
    public static class TimeLimiterEndpointConfiguration {

        @Bean
        public TimeLimiterEndpoint timeLimiterEndpoint(TimeLimiterRegistry timeLimiterRegistry) {
            return new TimeLimiterEndpoint(timeLimiterRegistry);
        }

        @Bean
        public TimeLimiterEventsEndpoint timeLimiterEventsEndpoint(EventConsumerRegistry<TimeLimiterEvent> eventsConsumerRegistry) {
            return new TimeLimiterEventsEndpoint(eventsConsumerRegistry);
        }

    }
}
