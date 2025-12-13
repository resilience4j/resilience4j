/*
 * Copyright 2025 Robert Winkler, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.circuitbreaker.configure.*;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import io.github.resilience4j.springboot.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpoint;
import io.github.resilience4j.springboot.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpoint;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.springboot.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.springboot.spelresolver.autoconfigure.SpelResolverConfigurationOnMissingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j-circuitbreaker.
 */
@AutoConfiguration
@ConditionalOnClass(CircuitBreaker.class)
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@Import({FallbackConfigurationOnMissingBean.class, SpelResolverConfigurationOnMissingBean.class})
public class CircuitBreakerAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    private final CircuitBreakerConfiguration circuitBreakerConfiguration;

    public CircuitBreakerAutoConfiguration(
            CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerProperties);
    }

    @Bean
    @ConditionalOnMissingBean(value = CircuitBreakerEvent.class, parameterizedContainer = EventConsumerRegistry.class)
    public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
        return circuitBreakerConfiguration.eventConsumerRegistry();
    }

    @Bean
    @Qualifier("compositeCircuitBreakerCustomizer")
    public CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer(
        @Autowired(required = false) List<CircuitBreakerConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer,
        @Qualifier("compositeCircuitBreakerCustomizer") CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer) {
        return circuitBreakerConfiguration
            .circuitBreakerRegistry(eventConsumerRegistry, circuitBreakerRegistryEventConsumer,
                compositeCircuitBreakerCustomizer);
    }

    @Bean
    @Primary
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<CircuitBreaker>>> optionalRegistryEventConsumers) {
        return circuitBreakerConfiguration
            .circuitBreakerRegistryEventConsumer(optionalRegistryEventConsumers);
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    public CircuitBreakerAspect circuitBreakerAspect(
        CircuitBreakerRegistry circuitBreakerRegistry,
        @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList,
        FallbackExecutor fallbackExecutor,
        SpelResolver spelResolver
    ) {
        return circuitBreakerConfiguration
            .circuitBreakerAspect(circuitBreakerRegistry, circuitBreakerAspectExtList,
                fallbackExecutor, spelResolver);
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2CircuitBreakerAspectExt rxJava2CircuitBreakerAspect() {
        return circuitBreakerConfiguration.rxJava2CircuitBreakerAspect();
    }

    @Bean
    @Conditional(value = {RxJava3OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava3CircuitBreakerAspectExt rxJava3CircuitBreakerAspect() {
        return circuitBreakerConfiguration.rxJava3CircuitBreakerAspect();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorCircuitBreakerAspectExt reactorCircuitBreakerAspect() {
        return circuitBreakerConfiguration.reactorCircuitBreakerAspect();
    }

    @AutoConfiguration
    @ConditionalOnClass(Endpoint.class)
    static class CircuitBreakerEndpointAutoConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public CircuitBreakerEndpoint circuitBreakerEndpoint(
            CircuitBreakerRegistry circuitBreakerRegistry) {
            return new CircuitBreakerEndpoint(circuitBreakerRegistry);
        }

        @Bean
        @ConditionalOnAvailableEndpoint
        public CircuitBreakerEventsEndpoint circuitBreakerEventsEndpoint(
            EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
            return new CircuitBreakerEventsEndpoint(eventConsumerRegistry);
        }
    }
}
