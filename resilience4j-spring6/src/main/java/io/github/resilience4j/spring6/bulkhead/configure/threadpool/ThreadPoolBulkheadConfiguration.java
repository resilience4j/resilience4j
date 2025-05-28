/*
 * Copyright 2019 lespinsideg , Mahmoud Romeh
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
package io.github.resilience4j.spring6.bulkhead.configure.threadpool;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * {@link Configuration Configuration} for {@link io.github.resilience4j.bulkhead.ThreadPoolBulkhead}
 */
@Configuration
public class ThreadPoolBulkheadConfiguration {

    @Bean
    @Qualifier("compositeThreadPoolBulkheadCustomizer")
    public CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer(
        @Autowired(required = false) List<ThreadPoolBulkheadConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    /**
     * @param bulkheadConfigurationProperties bulk head spring configuration properties
     * @param bulkheadEventConsumerRegistry   the bulk head event consumer registry
     * @return the ThreadPoolBulkheadRegistry with all needed setup in place
     */
    @Bean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry(
        CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties,
        EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry,
        RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer,
        @Qualifier("compositeThreadPoolBulkheadCustomizer") CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> bulkheadCustomizer) {
        ThreadPoolBulkheadRegistry bulkheadRegistry = createBulkheadRegistry(
            bulkheadConfigurationProperties, threadPoolBulkheadRegistryEventConsumer,
            bulkheadCustomizer);
        registerEventConsumer(bulkheadRegistry, bulkheadEventConsumerRegistry,
            bulkheadConfigurationProperties);
        initBulkheadRegistry(bulkheadConfigurationProperties, bulkheadCustomizer, bulkheadRegistry);
        return bulkheadRegistry;
    }

    /**
     * Initializes the Bulkhead registry with resilience4j instances.
     *
     * @param bulkheadRegistry The bulkhead registry.
     * @param bulkheadCustomizer customizers for instances and configs
     */
    private void initBulkheadRegistry(CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties,
        CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> bulkheadCustomizer,
        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        bulkheadConfigurationProperties.getInstances().forEach((name, properties) -> bulkheadRegistry
            .bulkhead(name, bulkheadConfigurationProperties
                .createThreadPoolBulkheadConfig(name, bulkheadCustomizer)));

        bulkheadCustomizer.instanceNames()
            .stream()
            .filter(name -> bulkheadRegistry.getConfiguration(name).isEmpty())
            .forEach(name -> bulkheadRegistry.bulkhead(name,
                bulkheadConfigurationProperties.createThreadPoolBulkheadConfig(null, bulkheadCustomizer, name)));
    }

    @Bean
    @Primary
    public RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<ThreadPoolBulkhead>>> optionalRegistryEventConsumers) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new));
    }

    /**
     * Initializes a bulkhead registry.
     *
     * @param threadPoolBulkheadConfigurationProperties The bulkhead configuration properties.
     * @param compositeThreadPoolBulkheadCustomizer the delegate of customizers
     * @return a ThreadPoolBulkheadRegistry
     */
    private ThreadPoolBulkheadRegistry createBulkheadRegistry(
        CommonThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties,
        RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer,
        CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer) {
        Map<String, ThreadPoolBulkheadConfig> configs = threadPoolBulkheadConfigurationProperties
            .getConfigs()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                entry -> threadPoolBulkheadConfigurationProperties
                    .createThreadPoolBulkheadConfig(entry.getValue(),
                        compositeThreadPoolBulkheadCustomizer, entry.getKey())));
        return ThreadPoolBulkheadRegistry.of(configs, threadPoolBulkheadRegistryEventConsumer, Map.copyOf(threadPoolBulkheadConfigurationProperties.getTags()));
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the
     * bulkheads.
     *
     * @param bulkheadRegistry      The BulkHead registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    private void registerEventConsumer(ThreadPoolBulkheadRegistry bulkheadRegistry,
        EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry,
        CommonThreadPoolBulkheadConfigurationProperties properties) {
        bulkheadRegistry.getEventPublisher()
            .onEntryAdded(event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(), properties))
            .onEntryReplaced(event -> registerEventConsumer(eventConsumerRegistry, event.getNewEntry(), properties))
            .onEntryRemoved(event -> unregisterEventConsumer(eventConsumerRegistry, event.getRemovedEntry()));
    }

    private void unregisterEventConsumer(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry, ThreadPoolBulkhead bulkHead) {
        eventConsumerRegistry.removeEventConsumer(bulkHead.getName());
    }

    private void registerEventConsumer(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry,
        ThreadPoolBulkhead bulkHead,
        CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties) {
        int eventConsumerBufferSize = ofNullable(bulkheadConfigurationProperties.getBackendProperties(bulkHead.getName()))
            .map(InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        bulkHead.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(
            String.join("-", ThreadPoolBulkhead.class.getSimpleName(), bulkHead.getName()),
            eventConsumerBufferSize));
    }
}
