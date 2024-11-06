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
import io.github.resilience4j.circuitbreaker.configure.IgnoreClassBindingExceptionsConverter;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEndpoint;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpoint;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.autoconfigure.FallbackConfigurationOnMissingBean;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.Arrays;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j-circuitbreaker.
 */
@Configuration
@ConditionalOnClass(CircuitBreaker.class)
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@Import({CircuitBreakerConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})
public class CircuitBreakerAutoConfiguration {

    private final AbstractEnvironment environment;

    public CircuitBreakerAutoConfiguration(final AbstractEnvironment environment) {
        this.environment = environment;
    }

    @Bean
    @ConfigurationPropertiesBinding
    public IgnoreClassBindingExceptionsConverter ignoreClassBindingExceptionsConverter() {
        boolean ignoreClassBindingExceptions = isIgnoreClassBindingExceptionsEnabled();
        return new IgnoreClassBindingExceptionsConverter(ignoreClassBindingExceptions);
    }

    @Bean
    public GenericConversionService genericConversionService() {
        GenericConversionService genericConversionService = new GenericConversionService();
        genericConversionService.addConverter(ignoreClassBindingExceptionsConverter());
        return genericConversionService;
    }

    private boolean isIgnoreClassBindingExceptionsEnabled() {
        return environment
                .getPropertySources()
                .stream()
                .filter(EnumerablePropertySource.class::isInstance)
                .map(EnumerablePropertySource.class::cast)
                .flatMap(ps -> Arrays.stream(ps.getPropertyNames()))
                .filter(name -> name.contains(".configs.") && name.endsWith(".ignoreUnknownExceptions"))
                .findFirst()
                .map(name -> environment.getProperty(name, Boolean.class, false))
                .orElse(false);
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
