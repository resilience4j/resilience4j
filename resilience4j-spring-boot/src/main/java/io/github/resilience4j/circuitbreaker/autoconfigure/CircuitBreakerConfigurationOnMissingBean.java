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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.*;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties.BackendProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import io.github.resilience4j.consumer.EventConsumerRegistry;

import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CircuitBreakerConfigurationOnMissingBean {

    private final CircuitBreakerConfiguration circuitBreakerConfiguration;
    private final ConfigurableBeanFactory beanFactory;
    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;

    public CircuitBreakerConfigurationOnMissingBean(CircuitBreakerConfigurationProperties circuitBreakerProperties,
                                                    ConfigurableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        // Register the event consumers
        circuitBreakerConfiguration.registerPostCreationEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);
        // Register a consumer to hook up any health indicators for circuit breakers after creation. This will catch ones that get
        // created beyond initially configured backends.
        circuitBreakerRegistry.registerPostCreationConsumer(this::createHeathIndicatorForCircuitBreaker);

        // Initialize backends that were initially configured.
        circuitBreakerConfiguration.initializeBackends(circuitBreakerRegistry);

        return circuitBreakerRegistry;
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry,
                                                     @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList) {
        return circuitBreakerConfiguration.circuitBreakerAspect(circuitBreakerRegistry, circuitBreakerAspectExtList);
    }

    @Bean
    public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
        return circuitBreakerConfiguration.eventConsumerRegistry();
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2CircuitBreakerAspectExt rxJava2CircuitBreakerAspect() {
        return circuitBreakerConfiguration.rxJava2CircuitBreakerAspect();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorCircuitBreakerAspectExt reactorCircuitBreakerAspect() {
        return circuitBreakerConfiguration.reactorCircuitBreakerAspect();
    }

    private void createHeathIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker, CircuitBreakerConfig circuitBreakerConfig) {
        BackendProperties backendProperties = circuitBreakerProperties.findCircuitBreakerBackend(circuitBreaker, circuitBreakerConfig);

        if(backendProperties != null && backendProperties.getRegisterHealthIndicator()) {
            CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);
            beanFactory.registerSingleton(
                    circuitBreaker.getName() + "CircuitBreakerHealthIndicator",
                    healthIndicator
            );
        }
    }
}
