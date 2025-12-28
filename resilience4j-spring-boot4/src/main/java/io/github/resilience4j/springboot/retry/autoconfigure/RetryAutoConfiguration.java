/*
 * Copyright 2025 Mahmoud Romeh, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.retry.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.retry.configure.*;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import io.github.resilience4j.springboot.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.springboot.retry.monitoring.endpoint.RetryEndpoint;
import io.github.resilience4j.springboot.retry.monitoring.endpoint.RetryEventsEndpoint;
import io.github.resilience4j.springboot.spelresolver.autoconfigure.SpelResolverConfigurationOnMissingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;


/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j-retry.
 */
@AutoConfiguration
@ConditionalOnClass(Retry.class)
@EnableConfigurationProperties(RetryProperties.class)
@Import({FallbackConfigurationOnMissingBean.class, SpelResolverConfigurationOnMissingBean.class})
public class RetryAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    private final RetryConfiguration retryConfiguration = new RetryConfiguration();

    /**
     * @param retryConfigurationProperties retryConfigurationProperties retry configuration spring
     *                                     properties
     * @param retryEventConsumerRegistry   the event retry registry
     * @return the retry definition registry
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry(RetryConfigurationProperties retryConfigurationProperties,
                                       EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry,
                                       RegistryEventConsumer<Retry> retryRegistryEventConsumer,
                                       @Qualifier("compositeRetryCustomizer") CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        return retryConfiguration
                .retryRegistry(retryConfigurationProperties, retryEventConsumerRegistry,
                        retryRegistryEventConsumer, compositeRetryCustomizer);
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances. The
     * EventConsumerRegistry is used by the Retry events monitor to show the latest RetryEvent
     * events for each Retry instance.
     *
     * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
     */
    @Bean
    @ConditionalOnMissingBean(value = RetryEvent.class, parameterizedContainer = EventConsumerRegistry.class)
    public EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry() {
        return retryConfiguration.retryEventConsumerRegistry();
    }

    @Bean
    @Qualifier("compositeRetryCustomizer")
    public CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer(
        @Autowired(required = false) List<RetryConfigCustomizer> configCustomizers) {
        return new CompositeCustomizer<>(configCustomizers);
    }

    @Bean
    @Primary
    public RegistryEventConsumer<Retry> retryRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<Retry>>> optionalRegistryEventConsumers) {
        return retryConfiguration.retryRegistryEventConsumer(optionalRegistryEventConsumers);
    }

    /**
     * @param retryConfigurationProperties retry configuration spring properties
     * @param retryRegistry                retry in memory registry
     * @return the spring retry AOP aspect
     */
    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RetryAspect retryAspect(
        RetryConfigurationProperties retryConfigurationProperties,
        RetryRegistry retryRegistry,
        @Autowired(required = false) List<RetryAspectExt> retryAspectExtList,
        FallbackExecutor fallbackExecutor,
        SpelResolver spelResolver,
        @Autowired(required = false) ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor
    ) {
        return retryConfiguration
            .retryAspect(retryConfigurationProperties, retryRegistry, retryAspectExtList,
                fallbackExecutor, spelResolver, contextAwareScheduledThreadPoolExecutor);
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2RetryAspectExt rxJava2RetryAspectExt() {
        return retryConfiguration.rxJava2RetryAspectExt();
    }

    @Bean
    @Conditional(value = {RxJava3OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava3RetryAspectExt rxJava3RetryAspectExt() {
        return retryConfiguration.rxJava3RetryAspectExt();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorRetryAspectExt reactorRetryAspectExt() {
        return retryConfiguration.reactorRetryAspectExt();
    }

    @AutoConfiguration
    @ConditionalOnClass(Endpoint.class)
    static class RetryAutoEndpointConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public RetryEndpoint retryEndpoint(RetryRegistry retryRegistry) {
            return new RetryEndpoint(retryRegistry);
        }

        @Bean
        @ConditionalOnAvailableEndpoint
        public RetryEventsEndpoint retryEventsEndpoint(
            EventConsumerRegistry<RetryEvent> eventConsumerRegistry) {
            return new RetryEventsEndpoint(eventConsumerRegistry);
        }
    }
}
