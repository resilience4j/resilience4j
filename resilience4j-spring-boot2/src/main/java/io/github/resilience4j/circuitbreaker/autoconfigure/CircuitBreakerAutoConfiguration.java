/*
 * Copyright 2019 Robert Winkler
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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.internal.StringToThrowableClassConverter;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpoint;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpoint;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.env.Environment;

import java.util.Map;


/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j-circuitbreaker.
 */
@Configuration
@ConditionalOnClass(CircuitBreaker.class)
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@Import({CircuitBreakerConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})
public class CircuitBreakerAutoConfiguration {

    private final String CIRCUIT_BREAKER_PREFIX = "resilience4j.circuitbreaker.configs.";

    private final Environment environment;
    private final CircuitBreakerProperties circuitBreakerProperties;

    public CircuitBreakerAutoConfiguration(Environment environment, CircuitBreakerProperties circuitBreakerProperties) {
        this.environment = environment;
        this.circuitBreakerProperties = circuitBreakerProperties;
    }

    @Bean
    @ConfigurationPropertiesBinding
    public StringToThrowableClassConverter stringToThrowableClassConverter() {
        return new StringToThrowableClassConverter(environment, CIRCUIT_BREAKER_PREFIX + "default");
    }

    @Bean
    public ConverterRegistry converterRegistry(StringToThrowableClassConverter stringToThrowableClassConverter, ConverterRegistry registry) {
        registry.addConverter(stringToThrowableClassConverter);
        for (Map.Entry<String, CircuitBreakerProperties.InstanceProperties> entrySet : circuitBreakerProperties.getConfigs().entrySet()) {
            String configPrefix = CIRCUIT_BREAKER_PREFIX + entrySet.getKey();
            registry.addConverter(new StringToThrowableClassConverter(environment, configPrefix));
        }

        return registry;
    }

    @Configuration
    @ConditionalOnClass(Endpoint.class)
    static class CircuitBreakerEndpointAutoConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public CircuitBreakerEndpoint circuitBreakerEndpoint(
            CircuitBreakerRegistry circuitBreakerRegistry) {
            return new CircuitBreakerEndpoint(circuitBreakerRegistry);
        }

        @Bean
        @ConditionalOnAvailableEndpoint
        public CircuitBreakerEventsEndpoint circuitBreakerEventsEndpoint(
            EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
            return new CircuitBreakerEventsEndpoint(eventConsumerRegistry);
        }
    }
}
