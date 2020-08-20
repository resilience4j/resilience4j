/*
 * Copyright 2017 Bohdan Storozhuk, Mahmoud Romeh
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
package io.github.resilience4j.ratelimiter.configure;


import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.spelresolver.SpelResolver;
import io.github.resilience4j.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link org.springframework.context.annotation.Configuration Configuration} for resilience4j
 * ratelimiter.
 */
@Configuration
public class RateLimiterConfiguration {

    @Bean
    @Qualifier("compositeRateLimiterCustomizer")
    public CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer(
        @Nullable List<RateLimiterConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry(
        RateLimiterConfigurationProperties rateLimiterProperties,
        EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
        RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer,
        @Qualifier("compositeRateLimiterCustomizer") CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer) {
        RateLimiterRegistry rateLimiterRegistry = createRateLimiterRegistry(rateLimiterProperties,
            rateLimiterRegistryEventConsumer, compositeRateLimiterCustomizer);
        registerEventConsumer(rateLimiterRegistry, rateLimiterEventsConsumerRegistry,
            rateLimiterProperties);
        rateLimiterProperties.getInstances().forEach(
            (name, properties) ->
                rateLimiterRegistry
                    .rateLimiter(name, rateLimiterProperties
                        .createRateLimiterConfig(properties, compositeRateLimiterCustomizer, name))
        );
        return rateLimiterRegistry;
    }

    @Bean
    @Primary
    public RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<RateLimiter>>> optionalRegistryEventConsumers) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new));
    }

    /**
     * Initializes a rate limiter registry.
     *
     * @param rateLimiterConfigurationProperties The rate limiter configuration properties.
     * @param compositeRateLimiterCustomizer the composite rate limiter customizer delegate
     * @return a RateLimiterRegistry
     */
    private RateLimiterRegistry createRateLimiterRegistry(
        RateLimiterConfigurationProperties rateLimiterConfigurationProperties,
        RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer,
        CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer) {
        Map<String, RateLimiterConfig> configs = rateLimiterConfigurationProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> rateLimiterConfigurationProperties
                    .createRateLimiterConfig(entry.getValue(), compositeRateLimiterCustomizer,
                        entry.getKey())));

        return RateLimiterRegistry.of(configs, rateLimiterRegistryEventConsumer,
            Map.copyOf(rateLimiterConfigurationProperties.getTags()));
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the rate
     * limiters.
     *
     * @param rateLimiterRegistry   The rate limiter registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    private void registerEventConsumer(RateLimiterRegistry rateLimiterRegistry,
        EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry,
        RateLimiterConfigurationProperties properties) {
        rateLimiterRegistry.getEventPublisher()
            .onEntryAdded(event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(), properties))
            .onEntryReplaced(event -> registerEventConsumer(eventConsumerRegistry, event.getNewEntry(), properties));
    }

    private void registerEventConsumer(
        EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry, RateLimiter rateLimiter,
        RateLimiterConfigurationProperties rateLimiterConfigurationProperties) {
        InstanceProperties limiterProperties = rateLimiterConfigurationProperties.getInstances()
            .get(rateLimiter.getName());
        if (limiterProperties != null && limiterProperties.getSubscribeForEvents() != null
            && limiterProperties.getSubscribeForEvents()) {
            rateLimiter.getEventPublisher().onEvent(
                eventConsumerRegistry.createEventConsumer(rateLimiter.getName(),
                    limiterProperties.getEventConsumerBufferSize() != null
                        && limiterProperties.getEventConsumerBufferSize() != 0 ? limiterProperties
                        .getEventConsumerBufferSize() : 100));
        }
    }

    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    public RateLimiterAspect rateLimiterAspect(
        RateLimiterConfigurationProperties rateLimiterProperties,
        RateLimiterRegistry rateLimiterRegistry,
        @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList,
        FallbackDecorators fallbackDecorators,
        SpelResolver spelResolver
    ) {
        return new RateLimiterAspect(rateLimiterRegistry, rateLimiterProperties,
            rateLimiterAspectExtList, fallbackDecorators, spelResolver);
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public RxJava2RateLimiterAspectExt rxJava2RateLimiterAspectExt() {
        return new RxJava2RateLimiterAspectExt();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public ReactorRateLimiterAspectExt reactorRateLimiterAspectExt() {
        return new ReactorRateLimiterAspectExt();
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances. The
     * EventConsumerRegistry is used by the RateLimiterHealthIndicator to show the latest
     * RateLimiterEvents events for each RateLimiter instance.
     *
     * @return The EventConsumerRegistry of RateLimiterEvent bean.
     */
    @Bean
    public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

}
