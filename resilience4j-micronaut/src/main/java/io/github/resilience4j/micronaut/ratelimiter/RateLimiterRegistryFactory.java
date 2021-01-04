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
package io.github.resilience4j.micronaut.ratelimiter;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
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
@Requires(property = "resilience4j.ratelimiter.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class RateLimiterRegistryFactory {

    @Bean
    @RateLimiterQualifier
    public CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer(
        @Nullable List<RateLimiterConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Singleton
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterProperties rateLimiterProperties,
                                                   @RateLimiterQualifier EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
                                                   @RateLimiterQualifier RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer,
                                                   @RateLimiterQualifier CompositeCustomizer<RateLimiterConfigCustomizer> compositeRateLimiterCustomizer) {
        RateLimiterRegistry rateLimiterRegistry = createRateLimiterRegistry(rateLimiterProperties, rateLimiterRegistryEventConsumer, compositeRateLimiterCustomizer);
        registerEventConsumer(rateLimiterRegistry, rateLimiterEventsConsumerRegistry,
            rateLimiterProperties);
        rateLimiterProperties.getInstances().forEach(
            (name, properites) ->
                rateLimiterRegistry
                    .rateLimiter(name, rateLimiterProperties
                        .createRateLimiterConfig(properites, compositeRateLimiterCustomizer, name))
        );
        return rateLimiterRegistry;
    }

    @Bean
    @RateLimiterQualifier
    public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventEventConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    @Bean
    @Primary
    @RateLimiterQualifier
    public RegistryEventConsumer<RateLimiter> rateLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<RateLimiter>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
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
                                       RateLimiterConfigurationProperties rateLimiterConfigurationProperties) {
        rateLimiterRegistry.getEventPublisher().onEntryAdded(
            event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(),
                rateLimiterConfigurationProperties));
    }

    private void registerEventConsumer(
        EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry, RateLimiter rateLimiter,
        RateLimiterConfigurationProperties rateLimiterConfigurationProperties) {
        final io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties limiterProperties = rateLimiterConfigurationProperties
            .getInstances().get(rateLimiter.getName());
        if (limiterProperties != null && limiterProperties.getSubscribeForEvents() != null
            && limiterProperties.getSubscribeForEvents()) {
            rateLimiter.getEventPublisher().onEvent(eventConsumerRegistry
                .createEventConsumer(rateLimiter.getName(),
                    limiterProperties.getEventConsumerBufferSize() != null
                        && limiterProperties.getEventConsumerBufferSize() != 0 ? limiterProperties
                        .getEventConsumerBufferSize() : 100));
        }
    }


    /**
     * Initializes a rate limiter registry.
     *
     * @param rateLimiterConfigurationProperties The rate limiter configuration properties.
     * @param compositeRateLimiterCustomizer     the composite rate limiter customizer delegate
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

        return RateLimiterRegistry.of(configs, rateLimiterRegistryEventConsumer, rateLimiterConfigurationProperties.getTags());
    }

}
