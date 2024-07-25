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
package io.github.resilience4j.retry.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.fallback.FallbackExecutor;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.configure.*;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.spelresolver.SpelResolver;
import io.github.resilience4j.spelresolver.autoconfigure.SpelResolverConfigurationOnMissingBean;
import io.github.resilience4j.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.utils.RxJava3OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * {@link Configuration Configuration} for resilience4j-retry.
 */
@Configuration
@Import({FallbackConfigurationOnMissingBean.class, SpelResolverConfigurationOnMissingBean.class})
public abstract class AbstractRetryConfigurationOnMissingBean {

    protected final RetryConfiguration retryConfiguration;

    public AbstractRetryConfigurationOnMissingBean() {
        this.retryConfiguration = new RetryConfiguration();
    }

    @Bean
    @Qualifier("compositeRetryCustomizer")
    @ConditionalOnMissingBean(name = "compositeRetryCustomizer")
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
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry(RetryConfigurationProperties retryConfigurationProperties,
        EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry,
        RegistryEventConsumer<Retry> retryRegistryEventConsumer,
        @Qualifier("compositeRetryCustomizer") CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer) {
        return retryConfiguration
            .retryRegistry(retryConfigurationProperties, retryEventConsumerRegistry,
                retryRegistryEventConsumer, compositeRetryCustomizer);
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

}
