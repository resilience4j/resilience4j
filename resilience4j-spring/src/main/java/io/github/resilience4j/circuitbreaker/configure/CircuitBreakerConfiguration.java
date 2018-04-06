/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpoint;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-circuitbreaker.
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties circuitBreakerProperties,
                                                         EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
                                                         ConfigurableBeanFactory beanFactory) {
        CircuitBreakerRegistry circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry();
        circuitBreakerProperties.getBackends().forEach(
            (name, properties) -> {
                CircuitBreakerConfig circuitBreakerConfig = circuitBreakerProperties.createCircuitBreakerConfig(name);
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name, circuitBreakerConfig);
                circuitBreaker.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(name, properties.getEventConsumerBufferSize()));

                if (properties.getRegisterHealthIndicator()) {
                    CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);
                    beanFactory.registerSingleton(
                        name + "CircuitBreakerHealthIndicator",
                        healthIndicator
                    );
                }
            }
        );
        return circuitBreakerRegistry;
    }

    @Bean
    public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerProperties circuitBreakerProperties,
                                                     CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerAspect(circuitBreakerProperties, circuitBreakerRegistry);
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances.
     * The EventConsumerRegistry is used by the CircuitBreakerHealthIndicator to show the latest CircuitBreakerEvents events
     * for each CircuitBreaker instance.
     */
    @Bean
    public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }
}
