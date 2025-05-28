/*
 * Copyright 2017 Robert Winkler,Mahmoud Romeh
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
package io.github.resilience4j.spring6.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties.InstanceProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.fallback.configure.FallbackConfiguration;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.spelresolver.configure.SpelResolverConfiguration;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link org.springframework.context.annotation.Configuration Configuration} for
 * resilience4j-circuitbreaker.
 */
@Configuration
@Import({FallbackConfiguration.class, SpelResolverConfiguration.class})
public class CircuitBreakerConfiguration {

    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;

    public CircuitBreakerConfiguration(
        CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.circuitBreakerProperties = circuitBreakerProperties;
    }

    @Bean
    @Qualifier("compositeCircuitBreakerCustomizer")
    public CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer(
        @Autowired(required = false) List<CircuitBreakerConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer,
        @Qualifier("compositeCircuitBreakerCustomizer") CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer) {
        CircuitBreakerRegistry circuitBreakerRegistry = createCircuitBreakerRegistry(
            circuitBreakerProperties, circuitBreakerRegistryEventConsumer,
            compositeCircuitBreakerCustomizer);
        registerEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);
        // then pass the map here
        initCircuitBreakerRegistry(circuitBreakerRegistry, compositeCircuitBreakerCustomizer);
        return circuitBreakerRegistry;
    }

    @Bean
    @Primary
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<CircuitBreaker>>> optionalRegistryEventConsumers) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new));
    }

    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    public CircuitBreakerAspect circuitBreakerAspect(
        CircuitBreakerRegistry circuitBreakerRegistry,
        @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList,
        FallbackExecutor fallbackExecutor,
        SpelResolver spelResolver
    ) {
        return new CircuitBreakerAspect(circuitBreakerProperties, circuitBreakerRegistry,
            circuitBreakerAspectExtList, fallbackExecutor, spelResolver);
    }


    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public RxJava2CircuitBreakerAspectExt rxJava2CircuitBreakerAspect() {
        return new RxJava2CircuitBreakerAspectExt();
    }

    @Bean
    @Conditional(value = {RxJava3OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public RxJava3CircuitBreakerAspectExt rxJava3CircuitBreakerAspect() {
        return new RxJava3CircuitBreakerAspectExt();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public ReactorCircuitBreakerAspectExt reactorCircuitBreakerAspect() {
        return new ReactorCircuitBreakerAspectExt();
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances. The
     * EventConsumerRegistry is used by the CircuitBreakerHealthIndicator to show the latest
     * CircuitBreakerEvents events for each CircuitBreaker instance.
     *
     * @return a default EventConsumerRegistry {@link io.github.resilience4j.consumer.DefaultEventConsumerRegistry}
     */
    @Bean
    public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    /**
     * Initializes a circuitBreaker registry.
     *
     * @param circuitBreakerProperties The circuit breaker configuration properties.
     * @param compositeCircuitBreakerCustomizer customizers for instances and configs
     * @return a CircuitBreakerRegistry
     */
    CircuitBreakerRegistry createCircuitBreakerRegistry(
        CircuitBreakerConfigurationProperties circuitBreakerProperties,
        RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer,
        CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer) {

        Map<String, CircuitBreakerConfig> configs = circuitBreakerProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> circuitBreakerProperties.createCircuitBreakerConfig(entry.getKey(), entry.getValue(), compositeCircuitBreakerCustomizer)));

        return CircuitBreakerRegistry.of(configs, circuitBreakerRegistryEventConsumer, Map.copyOf(circuitBreakerProperties.getTags()));
    }

    /**
     * Initializes the CircuitBreaker registry with resilience4j instances.
     *
     * @param circuitBreakerRegistry The circuit breaker registry.
     * @param compositeCircuitBreakerCustomizer customizers for instances and configs
     */
    private void initCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry,
        CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer) {
        circuitBreakerProperties.getInstances().forEach((name, properties) -> circuitBreakerRegistry.circuitBreaker(name,
            circuitBreakerProperties.createCircuitBreakerConfig(name, properties, compositeCircuitBreakerCustomizer)));

        compositeCircuitBreakerCustomizer.instanceNames()
            .stream()
            .filter(name -> circuitBreakerRegistry.getConfiguration(name).isEmpty())
            .forEach(name -> circuitBreakerRegistry.circuitBreaker(name,
                circuitBreakerProperties.createCircuitBreakerConfig(name, null, compositeCircuitBreakerCustomizer)));
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the
     * circuit breakers.
     *
     * @param circuitBreakerRegistry The circuit breaker registry.
     * @param eventConsumerRegistry  The event consumer registry.
     */
    public void registerEventConsumer(CircuitBreakerRegistry circuitBreakerRegistry,
                                      EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        circuitBreakerRegistry.getEventPublisher()
            .onEntryAdded(event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry()))
            .onEntryReplaced(event -> registerEventConsumer(eventConsumerRegistry, event.getNewEntry()))
            .onEntryRemoved(event -> unregisterEventConsumer(eventConsumerRegistry, event.getRemovedEntry()));
    }

    private void unregisterEventConsumer(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry, CircuitBreaker circuitBreaker) {
        eventConsumerRegistry.removeEventConsumer(circuitBreaker.getName());
    }

    private void registerEventConsumer(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        CircuitBreaker circuitBreaker) {
        int eventConsumerBufferSize = circuitBreakerProperties
            .findCircuitBreakerProperties(circuitBreaker.getName())
            .map(InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        circuitBreaker.getEventPublisher().onEvent(eventConsumerRegistry
            .createEventConsumer(circuitBreaker.getName(), eventConsumerBufferSize));
    }
}
