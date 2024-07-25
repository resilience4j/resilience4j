/*
 * Copyright 2023 Mariusz Kopylec
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

package io.github.resilience4j.springboot3.micrometer.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.micrometer.configuration.TimerConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.micrometer.event.TimerEvent;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.micrometer.configure.*;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava3OnClasspathCondition;
import io.github.resilience4j.springboot3.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import io.github.resilience4j.springboot3.spelresolver.autoconfigure.SpelResolverConfigurationOnMissingBean;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

import java.util.List;
import java.util.Optional;

@Configuration
@Import({FallbackConfigurationOnMissingBean.class, SpelResolverConfigurationOnMissingBean.class})
public abstract class AbstractTimerConfigurationOnMissingBean {

    protected final TimerConfiguration timerConfiguration;

    protected AbstractTimerConfigurationOnMissingBean() {
        this.timerConfiguration = new TimerConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean(name = "compositeTimerCustomizer")
    @Qualifier("compositeTimerCustomizer")
    public CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer(@Autowired(required = false) List<TimerConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    @Bean
    @ConditionalOnMissingBean
    public TimerRegistry timerRegistry(
            TimerConfigurationProperties timerProperties,
            EventConsumerRegistry<TimerEvent> timerEventsConsumerRegistry,
            RegistryEventConsumer<Timer> timerRegistryEventConsumer,
            @Qualifier("compositeTimerCustomizer") CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer,
            @Autowired(required = false) MeterRegistry registry
    ) {
        return timerConfiguration.timerRegistry(timerProperties, timerEventsConsumerRegistry, timerRegistryEventConsumer, compositeTimerCustomizer, registry);
    }

    @Bean
    @Primary
    public RegistryEventConsumer<Timer> timerRegistryEventConsumer(Optional<List<RegistryEventConsumer<Timer>>> optionalRegistryEventConsumers) {
        return timerConfiguration.timerRegistryEventConsumer(optionalRegistryEventConsumers);
    }

    @Bean
    @Conditional(AspectJOnClasspathCondition.class)
    @ConditionalOnMissingBean
    public TimerAspect timerAspect(
            TimerConfigurationProperties timerProperties,
            TimerRegistry timerRegistry,
            @Autowired(required = false) List<TimerAspectExt> timerAspectExtList,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver
    ) {
        return timerConfiguration.timerAspect(timerProperties, timerRegistry, timerAspectExtList, fallbackExecutor, spelResolver);
    }

    @Bean
    @Conditional({RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava2TimerAspectExt rxJava2TimerAspectExt() {
        return timerConfiguration.rxJava2TimerAspectExt();
    }

    @Bean
    @Conditional({RxJava3OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public RxJava3TimerAspectExt rxJava3TimerAspectExt() {
        return timerConfiguration.rxJava3TimerAspectExt();
    }

    @Bean
    @Conditional({ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    @ConditionalOnMissingBean
    public ReactorTimerAspectExt reactorTimerAspectExt() {
        return timerConfiguration.reactorTimerAspectExt();
    }
}
