/*
 * Copyright 2023 Mariusz Kopylec
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

package io.github.resilience4j.springboot3.micrometer.autoconfigure;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.micrometer.event.TimerEvent;
import io.github.resilience4j.springboot3.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.springboot3.micrometer.monitoring.endpoint.TimerEndpoint;
import io.github.resilience4j.springboot3.micrometer.monitoring.endpoint.TimerEventsEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(Timer.class)
@EnableConfigurationProperties(TimerProperties.class)
@Import({TimerConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})
public class TimerAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Endpoint.class)
    static class TimerAutoEndpointConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public TimerEndpoint timerEndpoint(TimerRegistry timerRegistry) {
            return new TimerEndpoint(timerRegistry);
        }

        @Bean
        @ConditionalOnAvailableEndpoint
        public TimerEventsEndpoint timerEventsEndpoint(EventConsumerRegistry<TimerEvent> eventConsumerRegistry) {
            return new TimerEventsEndpoint(eventConsumerRegistry);
        }
    }
}
