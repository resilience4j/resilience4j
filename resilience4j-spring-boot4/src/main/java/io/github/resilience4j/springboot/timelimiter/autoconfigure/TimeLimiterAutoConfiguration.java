/*
 * Copyright 2025 Ingyu Hwang, Artur Havliukovskyi
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

package io.github.resilience4j.springboot.timelimiter.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.timelimiter.configure.*;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import io.github.resilience4j.springboot.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.springboot.spelresolver.autoconfigure.SpelResolverConfigurationOnMissingBean;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.springboot.timelimiter.monitoring.endpoint.TimeLimiterEndpoint;
import io.github.resilience4j.springboot.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpoint;
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

@AutoConfiguration
@ConditionalOnClass(TimeLimiter.class)
@EnableConfigurationProperties(TimeLimiterProperties.class)
@Import({FallbackConfigurationOnMissingBean.class, SpelResolverConfigurationOnMissingBean.class})
public class TimeLimiterAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    protected final TimeLimiterConfiguration timeLimiterConfiguration = new TimeLimiterConfiguration();

    @Bean
    @ConditionalOnMissingBean
    public TimeLimiterRegistry timeLimiterRegistry(
            TimeLimiterConfigurationProperties timeLimiterProperties,
            EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry,
            RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer,
            @Qualifier("compositeTimeLimiterCustomizer") CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer) {
        return timeLimiterConfiguration.timeLimiterRegistry(
                timeLimiterProperties, timeLimiterEventsConsumerRegistry,
                timeLimiterRegistryEventConsumer, compositeTimeLimiterCustomizer);
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances.
     * The EventConsumerRegistry is used by the TimeLimiter events monitor to show the latest TimeLimiterEvent events
     * for each TimeLimiter instance.
     *
     * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
     */
    @Bean
    @ConditionalOnMissingBean(value = TimeLimiterEvent.class, parameterizedContainer = EventConsumerRegistry.class)
    public EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry() {
        return timeLimiterConfiguration.timeLimiterEventsConsumerRegistry();
    }

    @Bean
    @Qualifier("compositeTimeLimiterCustomizer")
    public CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterCustomizer(
        @Autowired(required = false) List<TimeLimiterConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    @Bean
    @Primary
    public RegistryEventConsumer<TimeLimiter> timeLimiterRegistryEventConsumer(
        Optional<List<RegistryEventConsumer<TimeLimiter>>> optionalRegistryEventConsumers) {
        return timeLimiterConfiguration.timeLimiterRegistryEventConsumer(optionalRegistryEventConsumers);
    }

    @Bean
    @Conditional(AspectJOnClasspathCondition.class)
    @ConditionalOnMissingBean
    public TimeLimiterAspect timeLimiterAspect(
        TimeLimiterConfigurationProperties timeLimiterProperties,
        TimeLimiterRegistry timeLimiterRegistry,
        @Autowired(required = false) List<TimeLimiterAspectExt> timeLimiterAspectExtList,
        FallbackExecutor fallbackExecutor,
        SpelResolver spelResolver,
        @Autowired(required = false) ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor
    ) {
        return timeLimiterConfiguration.timeLimiterAspect(
            timeLimiterProperties, timeLimiterRegistry, timeLimiterAspectExtList, fallbackExecutor, spelResolver, contextAwareScheduledThreadPoolExecutor);
    }

    @Bean
    @Conditional({RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2TimeLimiterAspectExt rxJava2TimeLimiterAspectExt() {
        return timeLimiterConfiguration.rxJava2TimeLimiterAspectExt();
    }

    @Bean
    @Conditional({RxJava3OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava3TimeLimiterAspectExt rxJava3TimeLimiterAspectExt() {
        return timeLimiterConfiguration.rxJava3TimeLimiterAspectExt();
    }

    @Bean
    @Conditional({ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorTimeLimiterAspectExt reactorTimeLimiterAspectExt() {
        return timeLimiterConfiguration.reactorTimeLimiterAspectExt();
    }

    @AutoConfiguration
    @ConditionalOnClass(Endpoint.class)
    static class TimeLimiterAutoEndpointConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public TimeLimiterEndpoint timeLimiterEndpoint(TimeLimiterRegistry timeLimiterRegistry) {
            return new TimeLimiterEndpoint(timeLimiterRegistry);
        }

        @Bean
        @ConditionalOnAvailableEndpoint
        public TimeLimiterEventsEndpoint timeLimiterEventsEndpoint(EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry) {
            return new TimeLimiterEventsEndpoint(eventConsumerRegistry);
        }
    }

}
