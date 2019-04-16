/*
 * Copyright 2019 Mahmoud Romeh
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

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspectExt;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties.BackendProperties;
import io.github.resilience4j.circuitbreaker.configure.ReactorCircuitBreakerAspectExt;
import io.github.resilience4j.circuitbreaker.configure.RxJava2CircuitBreakerAspectExt;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;

@Configuration
public class CircuitBreakerConfigurationOnMissingBean implements ApplicationContextAware {

	private final CircuitBreakerConfiguration circuitBreakerConfiguration;
	private final ConfigurableBeanFactory beanFactory;
	private final CircuitBreakerConfigurationProperties circuitBreakerProperties;
	private ApplicationContext applicationContext;
	private HealthIndicatorRegistry healthIndicatorRegistry;

	public CircuitBreakerConfigurationOnMissingBean(CircuitBreakerConfigurationProperties circuitBreakerProperties,
	                                                ConfigurableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.circuitBreakerProperties = circuitBreakerProperties;
		this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerProperties);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean
	@ConditionalOnMissingBean
	public CircuitBreakerRegistry circuitBreakerRegistry(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
		CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

		// Register the event consumers
		circuitBreakerConfiguration.registerPostCreationEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);
		// Register a consumer to hook up any health indicators for circuit breakers after creation. This will catch ones that get
		// created beyond initially configured backends.
		circuitBreakerRegistry.registerPostCreationConsumer(this::createHeathIndicatorForCircuitBreaker);

		// Initialize backends that were initially configured.
		circuitBreakerConfiguration.initializeBackends(circuitBreakerRegistry);

		return circuitBreakerRegistry;
	}

	@Bean
	@ConditionalOnMissingBean
	public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry,
	                                                 @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList) {
		return circuitBreakerConfiguration.circuitBreakerAspect(circuitBreakerRegistry, circuitBreakerAspectExtList);
	}

	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	@ConditionalOnMissingBean
	public RxJava2CircuitBreakerAspectExt rxJava2CircuitBreakerAspect() {
		return circuitBreakerConfiguration.rxJava2CircuitBreakerAspect();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	@ConditionalOnMissingBean
	public ReactorCircuitBreakerAspectExt reactorCircuitBreakerAspect() {
		return circuitBreakerConfiguration.reactorCircuitBreakerAspect();
	}

	@Bean
	@ConditionalOnMissingBean(value = CircuitBreakerEvent.class, parameterizedContainer = EventConsumerRegistry.class)
	public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
		return circuitBreakerConfiguration.eventConsumerRegistry();
	}

	private void createHeathIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker, CircuitBreakerConfig circuitBreakerConfig) {
		BackendProperties backendProperties = circuitBreakerProperties.findCircuitBreakerBackend(circuitBreaker, circuitBreakerConfig);

		if (backendProperties != null && backendProperties.getRegisterHealthIndicator()) {
			CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);
			String circuitBreakerName = circuitBreaker.getName() + "CircuitBreaker";
			beanFactory.registerSingleton(
					circuitBreakerName + "HealthIndicator",
					healthIndicator
			);
			// To support health indicators created after the health registry was created, look up to see if it's in
			// the application context. If it is, save it off so we don't need to search for it again, then register
			// the new health indicator with the registry.
			if (applicationContext != null && healthIndicatorRegistry == null) {
				Map<String, HealthIndicatorRegistry> healthRegistryBeans = applicationContext.getBeansOfType(HealthIndicatorRegistry.class);
				if (healthRegistryBeans.size() > 0) {
					healthIndicatorRegistry = healthRegistryBeans.values().iterator().next();
				}
			}

			if (healthIndicatorRegistry != null && healthIndicatorRegistry.get(circuitBreakerName) == null) {
				healthIndicatorRegistry.register(circuitBreakerName, healthIndicator);
			}
		}
	}
}
