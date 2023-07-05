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

package io.github.resilience4j.spring6.micrometer.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.micrometer.configuration.CommonTimerConfigurationProperties.InstanceProperties;
import io.github.resilience4j.common.micrometer.configuration.TimerConfigCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerConfig;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.fallback.configure.FallbackConfiguration;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.spelresolver.configure.SpelResolverConfiguration;
import io.github.resilience4j.spring6.utils.AspectJOnClasspathCondition;
import io.github.resilience4j.spring6.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.spring6.utils.RxJava2OnClasspathCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link Configuration} for resilience4j-micrometer timer.
 */
@Configuration
@Import({FallbackConfiguration.class, SpelResolverConfiguration.class})
public class TimerConfiguration {

    @Bean
    @Qualifier("compositeTimerCustomizer")
    public CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer(
            @Autowired(required = false) List<TimerConfigCustomizer> customizers) {
        return new CompositeCustomizer<>(customizers);
    }

    @Bean
    public TimerRegistry timerRegistry(
            TimerConfigurationProperties timerConfigurationProperties,
            RegistryEventConsumer<Timer> timerRegistryEventConsumer,
            @Qualifier("compositeTimerCustomizer") CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer) {
        TimerRegistry timerRegistry =
                createTimerRegistry(timerConfigurationProperties, timerRegistryEventConsumer,
                        compositeTimerCustomizer);
        registerEventConsumer(timerRegistry, timerEventConsumerRegistry, timerConfigurationProperties);

        initTimerRegistry(timerRegistry, timerConfigurationProperties, compositeTimerCustomizer);
        return timerRegistry;
    }

    @Bean
    @Primary
    public RegistryEventConsumer<Timer> timerRegistryEventConsumer(
            Optional<List<RegistryEventConsumer<Timer>>> optionalRegistryEventConsumers) {
        return new CompositeRegistryEventConsumer<>(optionalRegistryEventConsumers.orElseGet(ArrayList::new));
    }

    @Bean
    @Conditional(AspectJOnClasspathCondition.class)
    public TimerAspect timerAspect(
            TimerConfigurationProperties timerConfigurationProperties,
            TimerRegistry timerRegistry,
            @Autowired(required = false) List<TimerAspectExt> timerAspectExtList,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver,
            @Autowired(required = false) ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor
    ) {
        return new TimerAspect(timerRegistry, timerConfigurationProperties, timerAspectExtList, fallbackExecutor, spelResolver, contextAwareScheduledThreadPoolExecutor);
    }

    @Bean
    @Conditional({RxJava2OnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public RxJava2TimerAspectExt rxJava2TimerAspectExt() {
        return new RxJava2TimerAspectExt();
    }

    @Bean
    @Conditional({ReactorOnClasspathCondition.class, AspectJOnClasspathCondition.class})
    public ReactorTimerAspectExt reactorTimerAspectExt() {
        return new ReactorTimerAspectExt();
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances.
     * The EventConsumerRegistry is used by the Timer events monitor to show the latest Timer events
     * for each Timer instance.
     *
     * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
     */
    @Bean
    public EventConsumerRegistry<TimerEvent> timerEventsConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }

    /**
     * Initializes a timer registry.
     *
     * @param timerConfigurationProperties The timer configuration properties.
     * @return a timerRegistry
     */
    private static TimerRegistry createTimerRegistry(
            TimerConfigurationProperties timerConfigurationProperties,
            RegistryEventConsumer<Timer> timerRegistryEventConsumer,
            CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer) {

        Map<String, TimerConfig> configs = timerConfigurationProperties.getConfigs()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> timerConfigurationProperties.createTimerConfig(
                                entry.getKey(), entry.getValue(), compositeTimerCustomizer)));

        return TimerRegistry.of(configs, timerRegistryEventConsumer, Map.copyOf(timerConfigurationProperties.getTags()));
    }

    /**
     * Initializes the Timer registry.
     *
     * @param timerRegistry            The time limiter registry.
     * @param compositeTimerCustomizer The Composite time limiter customizer
     */
    void initTimerRegistry(
            TimerRegistry timerRegistry,
            TimerConfigurationProperties timerConfigurationProperties,
            CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer) {

        timerConfigurationProperties.getInstances().forEach(
                (name, properties) -> timerRegistry.timer(name,
                        timerConfigurationProperties
                                .createTimerConfig(name, properties, compositeTimerCustomizer))
        );
    }

    /**
     * Registers the post creation consumer function that registers the consumer events to the timers.
     *
     * @param timerRegistry         The timer registry.
     * @param properties            timer configuration properties
     */
    private static void registerEventConsumer(TimerRegistry timerRegistry, TimerConfigurationProperties properties) {
        timerRegistry.getEventPublisher()
                .onEntryAdded(event -> registerEventConsumer(eventConsumerRegistry, event.getAddedEntry(), properties))
                .onEntryReplaced(event -> registerEventConsumer(eventConsumerRegistry, event.getNewEntry(), properties));
    }

    private static void registerEventConsumer(EventConsumerRegistry<TimerEvent> eventConsumerRegistry, Timer timer,
                                              TimerConfigurationProperties timerConfigurationProperties) {
        int eventConsumerBufferSize = Optional.ofNullable(timerConfigurationProperties.getInstanceProperties(timer.getName()))
                .map(InstanceProperties::getEventConsumerBufferSize)
                .orElse(100);
        timer.getEventPublisher().onEvent(
                eventConsumerRegistry.createEventConsumer(timer.getName(), eventConsumerBufferSize));
    }

}
