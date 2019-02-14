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
package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties.BackendProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.context.annotation.Configuration
 * Configuration} for resilience4j-circuitbreaker.
 */
@Configuration
public class CircuitBreakerConfiguration {

	private final CircuitBreakerConfigurationProperties circuitBreakerProperties;
	private EventConsumerRegister eventConsumerRegister;
	
	public CircuitBreakerConfiguration(CircuitBreakerConfigurationProperties circuitBreakerProperties) {
		this.circuitBreakerProperties = circuitBreakerProperties;
	}
	
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        registerPostCreationEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);
        initializeBackends(circuitBreakerRegistry, eventConsumerRegistry);
        return circuitBreakerRegistry;
    }

    @Bean
    public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerAspect(circuitBreakerProperties, circuitBreakerRegistry);
    }

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances.
     * The EventConsumerRegistry is used by the CircuitBreakerHealthIndicator to show the latest CircuitBreakerEvents events
     * for each CircuitBreaker instance.
     * @return a default EventConsumerRegistry {@link io.github.resilience4j.consumer.DefaultEventConsumerRegistry}
     */
    @Bean
    public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
        return new DefaultEventConsumerRegistry<>();
    }
    
    /**
     * Initializes the backends configured in the properties.
     * 
     * @param circuitBreakerRegistry The circuit breaker registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    public void initializeBackends(CircuitBreakerRegistry circuitBreakerRegistry,
    							   EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
    	
    	circuitBreakerProperties.getBackends().forEach(
                (name, properties) -> {
                    CircuitBreakerConfig circuitBreakerConfig = circuitBreakerProperties.createCircuitBreakerConfig(name);
                    circuitBreakerRegistry.circuitBreaker(name, circuitBreakerConfig);
                }
        );
    	
    }
    
    /**
     * Registers the post creation consumer function that registers the consumer events to the circuit breakers.
     * 
     * @param circuitBreakerRegistry The circuit breaker registry.
     * @param eventConsumerRegistry The event consumer registry.
     */
    public void registerPostCreationEventConsumer(CircuitBreakerRegistry circuitBreakerRegistry,
    		EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
    	eventConsumerRegister = new EventConsumerRegister(eventConsumerRegistry);
		circuitBreakerRegistry.registerPostCreationConsumer(
				(circuitBreaker, config) -> eventConsumerRegister.registerEventConsumer(circuitBreaker, config));
    }
    
    /**
     * Holds onto the event consumer registry for the post creation consumer function.
     */
    private final class EventConsumerRegister {
    	
    	private final EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry;
    	
    	public EventConsumerRegister(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
    		this.eventConsumerRegistry = eventConsumerRegistry;
    	}
    	
    	private void registerEventConsumer(CircuitBreaker circuitBreaker, CircuitBreakerConfig circuitBreakerConfig) {
        	BackendProperties backendProperties = circuitBreakerProperties.findCircuitBreakerBackend(circuitBreaker, circuitBreakerConfig);
        	
        	if(backendProperties != null) {
        		circuitBreaker.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(circuitBreaker.getName(), backendProperties.getEventConsumerBufferSize()));
        	}
        }
    }
}
