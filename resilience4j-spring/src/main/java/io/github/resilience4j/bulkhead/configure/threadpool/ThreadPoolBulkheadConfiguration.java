/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.configure.threadpool;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link Configuration Configuration} for {@link io.github.resilience4j.bulkhead.ThreadPoolBulkhead}
 */
@Configuration
public class ThreadPoolBulkheadConfiguration {

    /**
     * @param bulkheadConfigurationProperties bulk head spring configuration properties
     * @param bulkheadEventConsumerRegistry   the bulk head event consumer registry
     * @return the ThreadPoolBulkheadRegistry with all needed setup in place
     */
    @Bean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry(
        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties,
        EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry,
        RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer) {
        ThreadPoolBulkheadRegistry bulkheadRegistry = createBulkheadRegistry(
            bulkheadConfigurationProperties, threadPoolBulkheadRegistryEventConsumer);
        registerEventConsumer(bulkheadRegistry, bulkheadEventConsumerRegistry,
            bulkheadConfigurationProperties);
        bulkheadConfigurationProperties.getBackends().forEach((name, properties) -> bulkheadRegistry
            .bulkhead(name, bulkheadConfigurationProperties.createThreadPoolBulkheadConfig(name)));
        return bulkheadRegistry;
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
     * @param bulkheadConfigurationProperties The bulkhead configuration properties.
     * @return a ThreadPoolBulkheadRegistry
     */
    private ThreadPoolBulkheadRegistry createBulkheadRegistry(
        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties,
        RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer) {
        Map<String, ThreadPoolBulkheadConfig> configs = bulkheadConfigurationProperties.getConfigs()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> bulkheadConfigurationProperties
                .createThreadPoolBulkheadConfig(entry.getValue())));

        return ThreadPoolBulkheadRegistry.of(configs, threadPoolBulkheadRegistryEventConsumer);
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
        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties) {
        bulkheadRegistry.getEventPublisher().onEntryAdded(
            event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(),
                bulkheadConfigurationProperties));
    }

    private void registerEventConsumer(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry,
        ThreadPoolBulkhead bulkHead,
        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties) {
        int eventConsumerBufferSize = Optional
            .ofNullable(bulkheadConfigurationProperties.getBackendProperties(bulkHead.getName()))
            .map(
                ThreadPoolBulkheadConfigurationProperties.InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        bulkHead.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(
            String.join("-", ThreadPoolBulkhead.class.getSimpleName(), bulkHead.getName()),
            eventConsumerBufferSize));
    }
}
