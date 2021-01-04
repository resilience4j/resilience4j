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
package io.github.resilience4j.micronaut.retry;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Factory
@Requires(property = "resilience4j.retry.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class RetryRegistryFactory {

    @Bean
    @RetryQualifier
    public CompositeCustomizer<RetryConfigCustomizer> compositeTimeLimiterCustomizer(@Nullable List<RetryConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Singleton
    @Requires(beans = RetryConfigurationProperties.class)
    public RetryRegistry createRetryRegistry(
        RetryConfigurationProperties retryConfigurationProperties,
        @RetryQualifier EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry,
        @RetryQualifier RegistryEventConsumer<Retry> retryRegistryEventConsumer,
        @RetryQualifier CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        RetryRegistry retryRegistry = createRetryRegistry(retryConfigurationProperties,
            retryRegistryEventConsumer, compositeRetryCustomizer);
        registerEventConsumer(retryRegistry, retryEventConsumerRegistry,
            retryConfigurationProperties);
        retryConfigurationProperties.getInstances()
            .forEach((name, properties) ->
                retryRegistry.retry(name, retryConfigurationProperties
                    .createRetryConfig(name, compositeRetryCustomizer)));
        return retryRegistry;
    }


    @Bean
    @RetryQualifier
    public EventConsumerRegistry<RetryEvent> retryEventEventConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    @Bean
    @Primary
    @RetryQualifier
    public RegistryEventConsumer<Retry> retryRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<Retry>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the rate
     * limiters.
     *
     * @param retryRegistry         The rate limiter registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    private void registerEventConsumer(RetryRegistry retryRegistry,
                                       EventConsumerRegistry<RetryEvent> eventConsumerRegistry,
                                       RetryConfigurationProperties rateLimiterConfigurationProperties) {
        retryRegistry.getEventPublisher().onEntryAdded(
            event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(),
                rateLimiterConfigurationProperties));
    }

    private void registerEventConsumer(
        EventConsumerRegistry<RetryEvent> eventConsumerRegistry, Retry retry,
        RetryConfigurationProperties retryConfigurationProperties) {
        int eventConsumerBufferSize = Optional.ofNullable(retryConfigurationProperties.getBackendProperties(retry.getName()))
            .map(RetryConfigurationProperties.InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        retry.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(retry.getName(), eventConsumerBufferSize));
    }


    /**
     * Initializes a rate limiter registry.
     *
     * @param retryProperties                The rate limiter configuration properties.
     * @param compositeRateLimiterCustomizer the composite rate limiter customizer delegate
     * @return a RateLimiterRegistry
     */
    private RetryRegistry createRetryRegistry(
        RetryConfigurationProperties retryProperties,
        RegistryEventConsumer<Retry> rateLimiterRegistryEventConsumer,
        CompositeCustomizer<RetryConfigCustomizer> compositeRateLimiterCustomizer) {
        Map<String, RetryConfig> configs = retryProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> retryProperties
                    .createRetryConfig(entry.getValue(), compositeRateLimiterCustomizer,
                        entry.getKey())));

        return RetryRegistry.of(configs, rateLimiterRegistryEventConsumer, retryProperties.getTags());
    }
}
