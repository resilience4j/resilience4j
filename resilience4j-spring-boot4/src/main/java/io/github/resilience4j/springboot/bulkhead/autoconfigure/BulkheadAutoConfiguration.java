/*
 * Copyright 2025 lespinsideg, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.bulkhead.configure.*;
import io.github.resilience4j.spring6.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import io.github.resilience4j.springboot.bulkhead.monitoring.endpoint.BulkheadEndpoint;
import io.github.resilience4j.springboot.bulkhead.monitoring.endpoint.BulkheadEventsEndpoint;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.springboot.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
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
 * resilience4j-bulkhead.
 */
@AutoConfiguration
@ConditionalOnClass(Bulkhead.class)
@EnableConfigurationProperties({BulkheadProperties.class, ThreadPoolBulkheadProperties.class})
@Import({FallbackConfigurationOnMissingBean.class, SpelResolverConfigurationOnMissingBean.class})
public class BulkheadAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    private final BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
    private final ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();

    @Bean
    @ConditionalOnMissingBean
    public BulkheadRegistry bulkheadRegistry(
            BulkheadConfigurationProperties bulkheadConfigurationProperties,
            EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry,
            RegistryEventConsumer<Bulkhead> bulkheadRegistryEventConsumer,
            @Qualifier("compositeBulkheadCustomizer") CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer) {
        return bulkheadConfiguration
                .bulkheadRegistry(bulkheadConfigurationProperties, bulkheadEventConsumerRegistry,
                        bulkheadRegistryEventConsumer, compositeBulkheadCustomizer);
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances. The
     * EventConsumerRegistry is used by the BulkheadHealthIndicator to show the latest Bulkhead
     * events for each Bulkhead instance.
     *
     * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
     */
    @Bean
    @ConditionalOnMissingBean(value = BulkheadEvent.class, parameterizedContainer = EventConsumerRegistry.class)
    public EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry() {
        return bulkheadConfiguration.bulkheadEventConsumerRegistry();
    }


    @Bean
    @Qualifier("compositeBulkheadCustomizer")
    public CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer(
        @Autowired(required = false) List<BulkheadConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    @Bean
    @Primary
    public RegistryEventConsumer<Bulkhead> bulkheadRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<Bulkhead>>> optionalRegistryEventConsumers) {
        return bulkheadConfiguration.bulkheadRegistryEventConsumer(optionalRegistryEventConsumers);
    }

    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public BulkheadAspect bulkheadAspect(
        BulkheadConfigurationProperties bulkheadConfigurationProperties,
        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
        BulkheadRegistry bulkheadRegistry,
        @Autowired(required = false) List<BulkheadAspectExt> bulkHeadAspectExtList,
        FallbackExecutor fallbackExecutor,
        SpelResolver spelResolver) {
        return bulkheadConfiguration
            .bulkheadAspect(bulkheadConfigurationProperties, threadPoolBulkheadRegistry,
                bulkheadRegistry, bulkHeadAspectExtList, fallbackExecutor, spelResolver);
    }

    @Bean
    @Conditional(value = {RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2BulkheadAspectExt rxJava2BulkHeadAspectExt() {
        return bulkheadConfiguration.rxJava2BulkHeadAspectExt();
    }

    @Bean
    @Conditional(value = {RxJava3OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava3BulkheadAspectExt rxJava3BulkHeadAspectExt() {
        return bulkheadConfiguration.rxJava3BulkHeadAspectExt();
    }

    @Bean
    @Conditional(value = {ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorBulkheadAspectExt reactorBulkHeadAspectExt() {
        return bulkheadConfiguration.reactorBulkHeadAspectExt();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry(
            CommonThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties,
            EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry,
            RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer,
            @Qualifier("compositeThreadPoolBulkheadCustomizer") CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer) {
        return threadPoolBulkheadConfiguration.threadPoolBulkheadRegistry(
                threadPoolBulkheadConfigurationProperties, bulkheadEventConsumerRegistry,
                threadPoolBulkheadRegistryEventConsumer, compositeThreadPoolBulkheadCustomizer);
    }

    @Bean
    @ConditionalOnMissingBean(name = "compositeThreadPoolBulkheadCustomizer")
    @Qualifier("compositeThreadPoolBulkheadCustomizer")
    public CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer(
        @Autowired(required = false) List<ThreadPoolBulkheadConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    @Bean
    @Primary
    public RegistryEventConsumer<ThreadPoolBulkhead> threadPoolBulkheadRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<ThreadPoolBulkhead>>> optionalRegistryEventConsumers) {
        return threadPoolBulkheadConfiguration
            .threadPoolBulkheadRegistryEventConsumer(optionalRegistryEventConsumers);
    }

    @AutoConfiguration
    @ConditionalOnClass(Endpoint.class)
    static class BulkheadEndpointAutoConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public BulkheadEndpoint bulkheadEndpoint(BulkheadRegistry bulkheadRegistry,
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry) {
            return new BulkheadEndpoint(bulkheadRegistry, threadPoolBulkheadRegistry);
        }

        @Bean
        @ConditionalOnAvailableEndpoint
        public BulkheadEventsEndpoint bulkheadEventsEndpoint(
            EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
            return new BulkheadEventsEndpoint(eventConsumerRegistry);
        }
    }
}
