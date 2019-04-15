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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpoint;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpoint;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-circuitbreaker.
 */
@Configuration
@ConditionalOnClass(CircuitBreaker.class)
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@Import(CircuitBreakerConfiguration.class)
@AutoConfigureBefore(EndpointAutoConfiguration.class)
public class CircuitBreakerAutoConfiguration {

    private final CircuitBreakerProperties circuitBreakerProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ConfigurableBeanFactory beanFactory;

    public CircuitBreakerAutoConfiguration(CircuitBreakerProperties circuitBreakerProperties, CircuitBreakerRegistry circuitBreakerRegistry, ConfigurableBeanFactory beanFactory) {
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.beanFactory = beanFactory;
    }

    @Bean
    public CircuitBreakerEndpoint circuitBreakerEndpoint(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerEndpoint(circuitBreakerRegistry);
    }

    @Bean
    public CircuitBreakerEventsEndpoint circuitBreakerEventsEndpoint(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        return new CircuitBreakerEventsEndpoint(eventConsumerRegistry);
    }

    @PostConstruct
    public void configureRegistryWithHealthEndpoint(){
        circuitBreakerProperties.getBackends().forEach(
                (name, properties) -> {
                    if (properties.getRegisterHealthIndicator()) {
                        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
                        CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);
                        beanFactory.registerSingleton(
                                name + "CircuitBreakerHealthIndicator",
                                healthIndicator
                        );
                    }
                }
        );
    }

 }
