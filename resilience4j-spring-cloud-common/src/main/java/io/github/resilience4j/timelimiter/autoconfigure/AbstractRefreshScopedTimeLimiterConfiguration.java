/*
 * Copyright 2020 Ingyu Hwang
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
package io.github.resilience4j.timelimiter.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.configure.TimeLimiterConfiguration;
import io.github.resilience4j.timelimiter.configure.TimeLimiterConfigurationProperties;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

public abstract class AbstractRefreshScopedTimeLimiterConfiguration {

    protected final TimeLimiterConfiguration timeLimiterConfiguration;

    protected AbstractRefreshScopedTimeLimiterConfiguration() {
        this.timeLimiterConfiguration = new TimeLimiterConfiguration();
    }

    /**
     * @return the RefreshScoped TimeLimiterRegistry
     */
    @Bean
    @RefreshScope
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

}
