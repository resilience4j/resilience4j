/*
 * Copyright 2019 Michael Pollind
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
package io.github.resilience4j.micronaut.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Factory
@Requires(property = "resilience4j.circuitbreaker.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class CircuitBreakerRegistryFactory {
    @Bean
    @CircuitBreakerQualifier
    public CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer(
        @Nullable List<CircuitBreakerConfigCustomizer> configCustomizer ) {
        return new CompositeCustomizer<>(configCustomizer);
    }

    @Singleton
    @Requires(beans = CircuitBreakerProperties.class)
    public CircuitBreakerRegistry circuitBreakerRegistry(
        CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties,
        @CircuitBreakerQualifier EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        @CircuitBreakerQualifier RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer,
        @CircuitBreakerQualifier CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer) {
        CircuitBreakerRegistry circuitBreakerRegistry = createCircuitBreakerRegistry(
            circuitBreakerConfigurationProperties, circuitBreakerRegistryEventConsumer,
            compositeCircuitBreakerCustomizer);
        registerEventConsumer(circuitBreakerConfigurationProperties, circuitBreakerRegistry, eventConsumerRegistry);
        initCircuitBreakerRegistry(circuitBreakerConfigurationProperties, circuitBreakerRegistry, compositeCircuitBreakerCustomizer);
        return circuitBreakerRegistry;
    }


    @Bean
    @Primary
    @CircuitBreakerQualifier
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<CircuitBreaker>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
    }

    @Bean
    @CircuitBreakerQualifier
    public EventConsumerRegistry<CircuitBreakerEvent> circuitBreakerEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }


    /**
     * Initializes the CircuitBreaker registry.
     *
     * @param circuitBreakerRegistry The circuit breaker registry.
     * @param customizerMap
     */
    private void initCircuitBreakerRegistry(CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties,
                                    CircuitBreakerRegistry circuitBreakerRegistry,
                                    CompositeCustomizer<CircuitBreakerConfigCustomizer> customizerMap) {
        circuitBreakerConfigurationProperties.getInstances().forEach(
            (name, properties) -> circuitBreakerRegistry.circuitBreaker(name,
                circuitBreakerConfigurationProperties
                    .createCircuitBreakerConfig(name, properties, customizerMap))
        );
    }

    /**
     * Initializes a circuitBreaker registry.
     *
     * @param circuitBreakerProperties The circuit breaker configuration properties.
     * @param customizerMap
     * @return a CircuitBreakerRegistry
     */
    CircuitBreakerRegistry createCircuitBreakerRegistry(
        CircuitBreakerConfigurationProperties circuitBreakerProperties,
        RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer,
        CompositeCustomizer<CircuitBreakerConfigCustomizer> customizerMap) {

        Map<String, CircuitBreakerConfig> configs = circuitBreakerProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> circuitBreakerProperties
                    .createCircuitBreakerConfig(entry.getKey(), entry.getValue(),
                        customizerMap)));

        return CircuitBreakerRegistry.of(configs, circuitBreakerRegistryEventConsumer, circuitBreakerProperties.getTags());
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the
     * circuit breakers.
     *
     * @param circuitBreakerRegistry The circuit breaker registry.
     * @param eventConsumerRegistry  The event consumer registry.
     */
    public void registerEventConsumer(CircuitBreakerConfigurationProperties circuitBreakerProperties,CircuitBreakerRegistry circuitBreakerRegistry,
                                      EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(
            event -> registerEventConsumer(circuitBreakerProperties, eventConsumerRegistry, event.getAddedEntry()));
    }


    private void registerEventConsumer( CircuitBreakerConfigurationProperties circuitBreakerRegistry,
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        CircuitBreaker circuitBreaker) {
        int eventConsumerBufferSize = circuitBreakerRegistry
            .findCircuitBreakerProperties(circuitBreaker.getName())
            .map(
                io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        circuitBreaker.getEventPublisher().onEvent(eventConsumerRegistry
            .createEventConsumer(circuitBreaker.getName(), eventConsumerBufferSize));
    }
}
