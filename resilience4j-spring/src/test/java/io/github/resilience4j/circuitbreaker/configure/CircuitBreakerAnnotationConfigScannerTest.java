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
	public void testGetInstanceProperties() {
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

	public static class AnnotatedBean {

		@CircuitBreaker(name = "mybreaker", ignoreExceptions = { IllegalArgumentException.class })
		public String foo() {
			return null;
		}
	}
}
