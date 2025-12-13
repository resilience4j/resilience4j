/*
 * Copyright 2025 Vijay Ram, Artur Havliukovskyi
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

package io.github.resilience4j.springboot.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.circuitbreaker.monitoring.endpoint.CircuitBreakerHystrixServerSideEvent;
import io.github.resilience4j.springboot.circuitbreaker.monitoring.endpoint.CircuitBreakerServerSideEvent;
import io.github.resilience4j.reactor.adapter.ReactorAdapter;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@AutoConfiguration(after = CircuitBreakerAutoConfiguration.class)
@ConditionalOnClass({CircuitBreaker.class, Endpoint.class, Flux.class, ReactorAdapter.class})
public class CircuitBreakerStreamEventsAutoConfiguration {

    @Bean
    @ConditionalOnAvailableEndpoint
    public CircuitBreakerServerSideEvent circuitBreakerServerSideEventEndpoint(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerServerSideEvent(circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnAvailableEndpoint
    public CircuitBreakerHystrixServerSideEvent circuitBreakerHystrixServerSideEventEndpoint(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerHystrixServerSideEvent(circuitBreakerRegistry);
    }
}
