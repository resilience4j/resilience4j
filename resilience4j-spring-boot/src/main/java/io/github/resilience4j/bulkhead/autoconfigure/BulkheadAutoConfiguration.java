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
import io.github.resilience4j.bulkhead.monitoring.endpoint.BulkheadEndpoint;
import io.github.resilience4j.bulkhead.monitoring.health.BulkheadHealthIndicator;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
public class BulkheadAutoConfiguration {

    private final BulkheadProperties bulkheadProperties;
    private final BulkheadRegistry bulkheadRegistry;
    private final ConfigurableBeanFactory beanFactory;

    public BulkheadAutoConfiguration(BulkheadProperties circuitBreakerProperties, BulkheadRegistry bulkheadRegistry, ConfigurableBeanFactory beanFactory) {
        this.bulkheadProperties = circuitBreakerProperties;
        this.bulkheadRegistry = bulkheadRegistry;
        this.beanFactory = beanFactory;
    }

    @Bean
    public BulkheadEndpoint bulkheadEndpoint(BulkheadRegistry bulkheadRegistry) {
        return new BulkheadEndpoint(bulkheadRegistry);
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
