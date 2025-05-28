/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.spring6.retry.configure;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties.InstanceProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.fallback.configure.FallbackConfiguration;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
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
 * {@link Configuration Configuration} for resilience4j-retry.
 */
@Configuration
@Import({FallbackConfiguration.class, SpelResolverConfiguration.class})
public class RetryConfiguration {


    @Bean
    @Qualifier("compositeRetryCustomizer")
    public CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer(
        @Autowired(required = false) List<RetryConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    /**
     * @param retryConfigurationProperties retryConfigurationProperties retry configuration spring
     *                                     properties
     * @param retryEventConsumerRegistry   the event retry registry
     * @return the retry definition registry
     */
    @Bean
    public RetryRegistry retryRegistry(RetryConfigurationProperties retryConfigurationProperties,
        EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry,
        RegistryEventConsumer<Retry> retryRegistryEventConsumer,
        @Qualifier("compositeRetryCustomizer") CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        RetryRegistry retryRegistry = createRetryRegistry(retryConfigurationProperties,
            retryRegistryEventConsumer, compositeRetryCustomizer);
        registerEventConsumer(retryRegistry, retryEventConsumerRegistry,
            retryConfigurationProperties);
        initRetryRegistry(retryConfigurationProperties, compositeRetryCustomizer, retryRegistry);
        return retryRegistry;
    }
    /**
     * Initializes the Retry registry with resilience4j instances.
     *
     * @param retryRegistry The retry registry.
     * @param compositeRetryCustomizer customizers for instances and configs
     */
    private void initRetryRegistry(RetryConfigurationProperties retryConfigurationProperties,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer, RetryRegistry retryRegistry) {
        retryConfigurationProperties.getInstances().forEach((name, properties) ->
            retryRegistry.retry(name, retryConfigurationProperties.createRetryConfig(name, compositeRetryCustomizer)));

        compositeRetryCustomizer.instanceNames()
            .stream()
            .filter(name -> retryRegistry.getConfiguration(name).isEmpty())
            .forEach(name ->
                retryRegistry.retry(name, retryConfigurationProperties.createRetryConfig(name, compositeRetryCustomizer)));
    }

    @Bean
    @Primary
    public RegistryEventConsumer<Retry> retryRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<Retry>>> optionalRegistryEventConsumers) {
        return new CompositeRegistryEventConsumer<>(
            optionalRegistryEventConsumers.orElseGet(ArrayList::new));
    }

    /**
     * Initializes a retry registry.
     *
     * @param retryConfigurationProperties The retry configuration properties.
     * @return a RetryRegistry
     */
    private RetryRegistry createRetryRegistry(
        RetryConfigurationProperties retryConfigurationProperties,
        RegistryEventConsumer<Retry> retryRegistryEventConsumer,
        CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        Map<String, RetryConfig> configs = retryConfigurationProperties.getConfigs()
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> retryConfigurationProperties
                    .createRetryConfig(entry.getValue(), compositeRetryCustomizer,
                        entry.getKey())));

        return RetryRegistry.of(configs, retryRegistryEventConsumer, Map.copyOf(retryConfigurationProperties.getTags()));
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the
     * retries.
     *
     * @param retryRegistry         The retry registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    private void registerEventConsumer(RetryRegistry retryRegistry,
        EventConsumerRegistry<RetryEvent> eventConsumerRegistry,
        RetryConfigurationProperties properties) {
        retryRegistry.getEventPublisher()
            .onEntryAdded(event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(), properties))
            .onEntryReplaced(event -> registerEventConsumer(eventConsumerRegistry, event.getNewEntry(), properties))
            .onEntryRemoved(event -> unregisterEventConsumer(eventConsumerRegistry, event.getRemovedEntry()));
    }

    private void unregisterEventConsumer(EventConsumerRegistry<RetryEvent> eventConsumerRegistry, Retry retry) {
        eventConsumerRegistry.removeEventConsumer(retry.getName());
    }

    private void registerEventConsumer(EventConsumerRegistry<RetryEvent> eventConsumerRegistry,
        Retry retry, RetryConfigurationProperties retryConfigurationProperties) {
        int eventConsumerBufferSize = Optional
            .ofNullable(retryConfigurationProperties.getBackendProperties(retry.getName()))
            .map(InstanceProperties::getEventConsumerBufferSize)
            .orElse(100);
        retry.getEventPublisher().onEvent(
            eventConsumerRegistry.createEventConsumer(retry.getName(), eventConsumerBufferSize));
    }

    /**
     * @param retryConfigurationProperties retry configuration spring properties
     * @param retryRegistry                retry in memory registry
     * @return the spring retry AOP aspect
     */
    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    public RetryAspect retryAspect(
        RetryConfigurationProperties retryConfigurationProperties,
        RetryRegistry retryRegistry,
        @Autowired(required = false) List<RetryAspectExt> retryAspectExtList,
        FallbackExecutor fallbackExecutor,
        SpelResolver spelResolver,
        @Autowired(required = false) ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor
    ) {
        return new RetryAspect(retryConfigurationProperties, retryRegistry, retryAspectExtList,
            fallbackExecutor, spelResolver, contextAwareScheduledThreadPoolExecutor);
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public RxJava2RetryAspectExt rxJava2RetryAspectExt() {
        return new RxJava2RetryAspectExt();
    }

    @Bean
    @Conditional(value = {RxJava3OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public RxJava3RetryAspectExt rxJava3RetryAspectExt() {
        return new RxJava3RetryAspectExt();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public ReactorRetryAspectExt reactorRetryAspectExt() {
        return new ReactorRetryAspectExt();
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances. The
     * EventConsumerRegistry is used by the Retry events monitor to show the latest RetryEvent
     * events for each Retry instance.
     *
     * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
     */
    @Bean
    public EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

}
