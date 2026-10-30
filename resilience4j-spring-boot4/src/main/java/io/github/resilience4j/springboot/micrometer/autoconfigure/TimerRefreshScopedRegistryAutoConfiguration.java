/*
 * Copyright 2026 the original author or authors.
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
package io.github.resilience4j.springboot.micrometer.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.micrometer.configuration.TimerConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.micrometer.event.TimerEvent;
import io.github.resilience4j.spring6.micrometer.configure.TimerConfiguration;
import io.github.resilience4j.spring6.micrometer.configure.TimerConfigurationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = TimerAutoConfiguration.class, after = RefreshAutoConfiguration.class)
@ConditionalOnClass({Timer.class, RefreshScope.class})
@ConditionalOnBean(org.springframework.cloud.context.scope.refresh.RefreshScope.class)
public class TimerRefreshScopedRegistryAutoConfiguration {

    // delegate conditional auto-configurations to regular spring configuration
    protected final TimerConfiguration timerConfiguration = new TimerConfiguration();

    /**
     * Overriding {@link TimerAutoConfiguration#timerRegistry} to be refreshable.
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public TimerRegistry timerRegistry(
            TimerConfigurationProperties timerProperties,
            EventConsumerRegistry<TimerEvent> timerEventsConsumerRegistry,
            RegistryEventConsumer<Timer> timerRegistryEventConsumer,
            @Qualifier("compositeTimerCustomizer") CompositeCustomizer<TimerConfigCustomizer> compositeTimerCustomizer,
            @Autowired(required = false) MeterRegistry registry) {
        return timerConfiguration.timerRegistry(
                timerProperties, timerEventsConsumerRegistry,
                timerRegistryEventConsumer, compositeTimerCustomizer, registry);
    }
}
