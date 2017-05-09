/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.prometheus.CircuitBreakerExports;
import io.prometheus.client.CollectorRegistry;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-metrics.
 */
@Configuration
@ConditionalOnClass(CollectorRegistry.class)
public class CircuitBreakerPrometheusAutoConfiguration {
    @Bean
    public CircuitBreakerExports circuitBreakerPrometheusCollector(CircuitBreakerRegistry circuitBreakerRegistry){
        CircuitBreakerExports collector = CircuitBreakerExports.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        collector.register();
        return collector;
    }
}
