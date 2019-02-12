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
package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfigurationOnMissingBean {

    private final CircuitBreakerConfiguration circuitBreakerConfiguration;

    CircuitBreakerConfigurationOnMissingBean() {
        circuitBreakerConfiguration = new CircuitBreakerConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfigurationProperties circuitBreakerProperties,
                                                         EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        return circuitBreakerConfiguration.circuitBreakerRegistry(circuitBreakerProperties, eventConsumerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerConfigurationProperties circuitBreakerProperties,
                                                     CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerConfiguration.circuitBreakerAspect(circuitBreakerProperties, circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
        return circuitBreakerConfiguration.eventConsumerRegistry();
    }
}
