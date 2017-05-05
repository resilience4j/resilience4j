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
package io.github.resilience4j.circuitbreaker.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpoint;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpoint;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-circuitbreaker.
 */
@Configuration
@ConditionalOnClass(CircuitBreaker.class)
@EnableConfigurationProperties(CircuitBreakerProperties.class)
public class CircuitBreakerAutoConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties circuitBreakerProperties){
        CircuitBreakerRegistry circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry();
        circuitBreakerProperties.getBackends().forEach(
                (name, properties) -> circuitBreakerRegistry.circuitBreaker(name, circuitBreakerProperties
                        .createCircuitBreakerConfig(name))
        );
        return circuitBreakerRegistry;
    }

    @Bean
    public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerProperties circuitBreakerProperties,
                                                     CircuitBreakerRegistry circuitBreakerRegistry){
        return new CircuitBreakerAspect(circuitBreakerProperties, circuitBreakerRegistry);
    }

    @Bean
    public CircuitBreakerEndpoint circuitBreakerEndpoint(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerEndpoint(circuitBreakerRegistry);
    }

    @Bean
    public CircuitBreakerEventsEndpoint circuitBreakerEventsEndpoint(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
                                                                     CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerEventsEndpoint(eventConsumerRegistry, circuitBreakerRegistry);
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
