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
package io.github.resilience4j.circuitbreaker.configure;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
		CircuitBreakerConfigurationSpringTest.ConfigWithOverrides.class
})
public class CircuitBreakerConfigurationSpringTest {

	@Autowired
	private ConfigWithOverrides configWithOverrides;


	@Test
	public void testAllCircuitBreakerConfigurationBeansOverridden() {
		assertNotNull(configWithOverrides.circuitBreakerRegistry);
		assertNotNull(configWithOverrides.circuitBreakerAspect);
		assertNotNull(configWithOverrides.circuitEventConsumerBreakerRegistry);
		assertNotNull(configWithOverrides.circuitBreakerConfigurationProperties);
		assertTrue(configWithOverrides.circuitBreakerConfigurationProperties.getSharedConfigs().size() == 1);
		final CircuitBreakerConfigurationProperties.BackendProperties circuitBreakerBackend = configWithOverrides.circuitBreakerConfigurationProperties
				.findCircuitBreakerBackend(CircuitBreaker.ofDefaults("testBackEndForShared"), CircuitBreakerConfig.custom().configurationName("sharedBackend").build());
		assertTrue(circuitBreakerBackend.getSharedConfigName().equals("sharedConfig"));
		assertTrue(circuitBreakerBackend.getFailureRateThreshold() == 3);

	}

	@Configuration
	@ComponentScan("io.github.resilience4j.circuitbreaker")
	public static class ConfigWithOverrides {

		private CircuitBreakerRegistry circuitBreakerRegistry;

		private CircuitBreakerAspect circuitBreakerAspect;

		private EventConsumerRegistry<CircuitBreakerEvent> circuitEventConsumerBreakerRegistry;

		private CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties;

		@Bean
		public CircuitBreakerRegistry circuitBreakerRegistry() {
			circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
			return circuitBreakerRegistry;
		}

		@Bean
		public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry,
		                                                 @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList) {
			circuitBreakerAspect = new CircuitBreakerAspect(circuitBreakerConfigurationProperties(), circuitBreakerRegistry, circuitBreakerAspectExtList);
			return circuitBreakerAspect;
		}

		@Bean
		public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
			circuitEventConsumerBreakerRegistry = new DefaultEventConsumerRegistry<>();
			return circuitEventConsumerBreakerRegistry;
		}

		@Bean
		public CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties() {
			circuitBreakerConfigurationProperties = new CircuitBreakerConfigurationPropertiesTest();
			return circuitBreakerConfigurationProperties;
		}

		private class CircuitBreakerConfigurationPropertiesTest extends CircuitBreakerConfigurationProperties {

			CircuitBreakerConfigurationPropertiesTest() {
				BackendProperties backendProperties = new BackendProperties();
				backendProperties.setSharedConfigName("sharedConfig");
				backendProperties.setFailureRateThreshold(3);
				getSharedConfigs().put("sharedBackend", backendProperties);
			}

		}
	}


}