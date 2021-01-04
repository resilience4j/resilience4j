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
package io.github.resilience4j.micronaut.bulkhead;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties;
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
@Requires(property = "resilience4j.bulkhead.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class BulkHeadRegistryFactory {

    @Bean
    @BulkheadQualifier
    public CompositeCustomizer<BulkheadConfigCustomizer> composeBulkheadCustomizer(
        @Nullable List<BulkheadConfigCustomizer> configCustomizers
    ) {
        return new CompositeCustomizer<>(configCustomizers);
    }


    @Singleton
    @Requires(beans = BulkheadProperties.class)
    public BulkheadRegistry bulkheadRegistry(
        BulkheadConfigurationProperties bulkheadConfigurationProperties,
        @BulkheadQualifier EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry,
        @BulkheadQualifier RegistryEventConsumer<Bulkhead> bulkheadRegistryEventConsumer,
        @BulkheadQualifier CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer) {
        BulkheadRegistry bulkheadRegistry = createBulkheadRegistry(bulkheadConfigurationProperties,
            bulkheadRegistryEventConsumer, compositeBulkheadCustomizer);
        registerEventConsumer(bulkheadRegistry, bulkheadEventConsumerRegistry,
            bulkheadConfigurationProperties);
        bulkheadConfigurationProperties.getInstances().forEach((name, properties) ->
            bulkheadRegistry
                .bulkhead(name, bulkheadConfigurationProperties
                    .createBulkheadConfig(properties, compositeBulkheadCustomizer,
                        name)));
        return bulkheadRegistry;
    }

    @Bean
    @Primary
    @BulkheadQualifier
    public RegistryEventConsumer<Bulkhead> bulkheadRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<Bulkhead>>> optionalRegistryEventConsumers
    ) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new)
        );
    }

    @Bean
    @BulkheadQualifier
    public EventConsumerRegistry<BulkheadEvent> bulkheadEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }


    /**
     * Registers the post creation consumer function that registers the consumer events to the
     * bulkheads.
     *
     * @param bulkheadRegistry      The BulkHead registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    private void registerEventConsumer(BulkheadRegistry bulkheadRegistry,
                                       EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry,
                                       BulkheadConfigurationProperties bulkheadConfigurationProperties) {
        bulkheadRegistry.getEventPublisher().onEntryAdded(
            event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(),
                bulkheadConfigurationProperties));
    }

    private void registerEventConsumer(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry,
                                       Bulkhead bulkHead, BulkheadConfigurationProperties bulkheadConfigurationProperties) {
        int eventConsumerBufferSize = Optional
            .ofNullable(bulkheadConfigurationProperties.getBackendProperties(bulkHead.getName()))
            .map(
                io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties.InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        bulkHead.getEventPublisher().onEvent(
            eventConsumerRegistry.createEventConsumer(bulkHead.getName(), eventConsumerBufferSize));
    }


    /**
     * Initializes a bulkhead registry.
     *
     * @param bulkheadConfigurationProperties The bulkhead configuration properties.
     * @param compositeBulkheadCustomizer
     * @return a BulkheadRegistry
     */
    private BulkheadRegistry createBulkheadRegistry(
        BulkheadConfigurationProperties bulkheadConfigurationProperties,
        RegistryEventConsumer<Bulkhead> bulkheadRegistryEventConsumer,
        CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer) {
        Map<String, BulkheadConfig> configs = bulkheadConfigurationProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> bulkheadConfigurationProperties.createBulkheadConfig(entry.getValue(),
                    compositeBulkheadCustomizer, entry.getKey())));
        return BulkheadRegistry.of(configs, bulkheadRegistryEventConsumer, bulkheadConfigurationProperties.getTags());
    }
}
