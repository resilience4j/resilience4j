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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;

/**
 * {@link org.springframework.context.annotation.Configuration
 * Configuration} for resilience4j-circuitbreaker.
 */
@Configuration
public class CircuitBreakerConfiguration {

	@Bean
	public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfigurationProperties circuitBreakerProperties,
	                                                     EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
		circuitBreakerProperties.getBackends().forEach(
				(name, properties) -> {
					CircuitBreakerConfig circuitBreakerConfig = circuitBreakerProperties.createCircuitBreakerConfig(name);
					CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name, circuitBreakerConfig);
					circuitBreaker.getEventPublisher().onEvent(eventConsumerRegistry.createEventConsumer(name, properties.getEventConsumerBufferSize()));
				}
		);
		return circuitBreakerRegistry;
	}

	@Bean
	public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerConfigurationProperties circuitBreakerProperties,
	                                                 CircuitBreakerRegistry circuitBreakerRegistry, @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList) {
		return new CircuitBreakerAspect(circuitBreakerProperties, circuitBreakerRegistry, circuitBreakerAspectExtList);
	}


	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	public RxJava2CircuitBreakerAspectExt rxJava2CircuitBreakerAspect() {
		return new RxJava2CircuitBreakerAspectExt();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	public ReactorCircuitBreakerAspectExt reactorCircuitBreakerAspect() {
		return new ReactorCircuitBreakerAspectExt();
	}

	/**
	 * The EventConsumerRegistry is used to manage EventConsumer instances.
	 * The EventConsumerRegistry is used by the CircuitBreakerHealthIndicator to show the latest CircuitBreakerEvents events
	 * for each CircuitBreaker instance.
	 *
	 * @return a default EventConsumerRegistry {@link io.github.resilience4j.consumer.DefaultEventConsumerRegistry}
	 */
	@Bean
	public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
		return new DefaultEventConsumerRegistry<>();
	}
}
