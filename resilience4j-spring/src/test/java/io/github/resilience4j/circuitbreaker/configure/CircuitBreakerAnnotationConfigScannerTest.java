/*
 * Copyright 2019 Olov Andersson
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;


public class CircuitBreakerAnnotationConfigScannerTest {

	@Test
	public void testMergeConfigurationPropertiesAnnotationConfigUsed() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClassName(AnnotatedBean.class.getName());
		beanFactory.registerBeanDefinition("foo", beanDefinition);
		CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
		scanner.postProcessBeanFactory(beanFactory);

		CircuitBreakerConfigurationProperties circuitBreakerProperties = new CircuitBreakerConfigurationProperties();
		scanner.mergeConfigurationProperties(circuitBreakerProperties);
		InstanceProperties instanceProperties = circuitBreakerProperties.getInstances().get("mybreaker");
		assertNotNull(instanceProperties);
		assertArrayEquals(new Class[] { IllegalArgumentException.class }, instanceProperties.getIgnoreExceptions());
	}
	
	@Test
	public void testMergeConfigurationPropertiesPropertiesTakePrecedence() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClassName(AnnotatedBean.class.getName());
		beanFactory.registerBeanDefinition("foo", beanDefinition);
		CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
		scanner.postProcessBeanFactory(beanFactory);

		CircuitBreakerConfigurationProperties circuitBreakerProperties = new CircuitBreakerConfigurationProperties();
		InstanceProperties existingInstanceProperties = new InstanceProperties();
		existingInstanceProperties.setIgnoreExceptions(new Class[] {NullPointerException.class});
		circuitBreakerProperties.getInstances().put("mybreaker", existingInstanceProperties);
		scanner.mergeConfigurationProperties(circuitBreakerProperties);
		InstanceProperties instanceProperties = circuitBreakerProperties.getInstances().get("mybreaker");
		assertNotNull(instanceProperties);
		assertArrayEquals(new Class[] { NullPointerException.class }, instanceProperties.getIgnoreExceptions());
	}

	public static class AnnotatedBean {

		@CircuitBreaker(name = "mybreaker", ignoreExceptions = { IllegalArgumentException.class })
		public String foo() {
			return null;
		}
	}
}
