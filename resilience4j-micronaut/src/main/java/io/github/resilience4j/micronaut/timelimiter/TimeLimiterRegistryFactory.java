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
package io.github.resilience4j.micronaut.timelimiter;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
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
@Requires(property = "resilience4j.timelimiter.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class TimeLimiterRegistryFactory {

    @Bean
    @TimeLimiterQualifier
    public CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer(@Nullable List<TimeLimiterConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Singleton
    @Requires(beans = TimeLimiterConfigurationProperties.class)
    public TimeLimiterRegistry timeLimiterRegistry(
        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties,
        @TimeLimiterQualifier EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventConsumerRegistry,
        @TimeLimiterQualifier RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
        @TimeLimiterQualifier CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        TimeLimiterRegistry timeLimiterRegistry =
            createTimeLimiterRegistry(timeLimiterConfigurationProperties, timeLimiterRegistryEventConsumer,
                compositeTimeLimiterCustomizer);
        registerEventConsumer(timeLimiterRegistry, timeLimiterEventConsumerRegistry, timeLimiterConfigurationProperties);

        initTimeLimiterRegistry(timeLimiterRegistry, timeLimiterConfigurationProperties, compositeTimeLimiterCustomizer);
        return timeLimiterRegistry;
    }


    @Bean
    @Primary
    @TimeLimiterQualifier
    public RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<TimeLimiter>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
    }

    @Bean
    @TimeLimiterQualifier
    public EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    void initTimeLimiterRegistry(TimeLimiterRegistry timeLimiterRegistry, TimeLimiterConfigurationProperties timeLimiterConfigurationProperties,
                                 CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        timeLimiterConfigurationProperties.getInstances().forEach(
            (name, properties) -> timeLimiterRegistry.timeLimiter(name,
                timeLimiterConfigurationProperties.createTimeLimiterConfig(name, properties, compositeTimeLimiterCustomizer))
        );
    }

    /**
     * Initializes a timeLimiter registry.
     *
     * @param timeLimiterConfigurationProperties The timeLimiter configuration properties.
     * @return a timeLimiterRegistry
     */
    private static TimeLimiterRegistry createTimeLimiterRegistry(
        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties,
        RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
        CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {

        Map<String, TimeLimiterConfig> configs = timeLimiterConfigurationProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> timeLimiterConfigurationProperties.createTimeLimiterConfig(
                    entry.getKey(), entry.getValue(), compositeTimeLimiterCustomizer)));

        return TimeLimiterRegistry.of(configs, timeLimiterRegistryEventConsumer, timeLimiterConfigurationProperties.getTags());
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the timeLimiters.
     *
     * @param timeLimiterRegistry                The timeLimiter registry.
     * @param eventConsumerRegistry              The event consumer registry.
     * @param timeLimiterConfigurationProperties timeLimiter configuration properties
     */
    private static void registerEventConsumer(TimeLimiterRegistry timeLimiterRegistry,
                                              EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry,
                                              TimeLimiterConfigurationProperties timeLimiterConfigurationProperties) {
        timeLimiterRegistry.getEventPublisher().onEntryAdded(event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(), timeLimiterConfigurationProperties));
    }

    private static void registerEventConsumer(EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry, TimeLimiter timeLimiter,
                                              TimeLimiterConfigurationProperties timeLimiterConfigurationProperties) {
        int eventConsumerBufferSize = Optional.ofNullable(timeLimiterConfigurationProperties.getInstanceProperties(timeLimiter.getName()))
            .map(io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        timeLimiter.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(timeLimiter.getName(), eventConsumerBufferSize));
    }

}
