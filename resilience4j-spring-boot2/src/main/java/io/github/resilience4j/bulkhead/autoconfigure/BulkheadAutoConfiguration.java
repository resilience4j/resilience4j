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
package io.github.resilience4j.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.BulkheadConfiguration;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.monitoring.endpoint.BulkheadEndpoint;
import io.github.resilience4j.bulkhead.monitoring.endpoint.BulkheadEventsEndpoint;
import io.github.resilience4j.bulkhead.monitoring.health.BulkheadHealthIndicator;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-bulkhead.
 */
@Configuration
@ConditionalOnClass(Bulkhead.class)
@EnableConfigurationProperties(BulkheadProperties.class)
@Import(BulkheadConfiguration.class)
@AutoConfigureBefore(EndpointAutoConfiguration.class)
public class BulkheadAutoConfiguration {

    private final BulkheadProperties bulkheadProperties;
    private final BulkheadRegistry bulkheadRegistry;
    private final ConfigurableBeanFactory beanFactory;

    public BulkheadAutoConfiguration(BulkheadProperties bulkheadProperties, BulkheadRegistry bulkheadRegistry, ConfigurableBeanFactory beanFactory) {
        this.bulkheadProperties = bulkheadProperties;
        this.bulkheadRegistry = bulkheadRegistry;
        this.beanFactory = beanFactory;
    }

    @Bean
    @ConditionalOnEnabledEndpoint
    public BulkheadEndpoint bulkheadEndpoint(BulkheadRegistry bulkheadRegistry) {
        return new BulkheadEndpoint(bulkheadRegistry);
    }

    @Bean
    @ConditionalOnEnabledEndpoint
    public BulkheadEventsEndpoint bulkheadEventsEndpoint(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
        return new BulkheadEventsEndpoint(eventConsumerRegistry);
    }

    @PostConstruct
    public void configureRegistryWithHealthEndpoint(){
        bulkheadProperties.getBackends().forEach(
                (name, properties) -> {
                    if (properties.getRegisterHealthIndicator()) {
                        Bulkhead bu = bulkheadRegistry.bulkhead(name);
                        BulkheadHealthIndicator healthIndicator = new BulkheadHealthIndicator(bu);
                        beanFactory.registerSingleton(
                                name + "BulkheadHealthIndicator",
                                healthIndicator
                        );
                    }
                }
        );
    }

 }
