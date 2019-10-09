package io.github.resilience4j.circuitbreaker.configure;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;

/**
 * Scans all bean definitions for CircuitBreaker annotations and collects
 * annotated configuration so it can be merged with the properties
 * configuration. This is done as a BeanFactoryPostProcessor so we can ensure
 * that we have found these before actual bean instantiation begins.
 * <p>
 * Unless resilience4j-spring-boot/resilience4j-spring-boot2 is used this bean
 * must be explicitly added to the spring configuration in order and the
 * mergeConfigurationProperties need to called for it to have effect.
 */
public class CircuitBreakerAnnotationConfigScanner implements BeanFactoryPostProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerAnnotationConfigScanner.class);

	private Map<String, InstanceProperties> instanceProperties = new HashMap<>();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (beanDefinition != null) {
				String beanClassName = beanDefinition.getBeanClassName();
				if (beanClassName != null) {
					try {
						Class<?> beanClass = Class.forName(beanClassName);
						findAnnotationConfig(beanClass);
					} catch (ClassNotFoundException e) {
						LOG.error("Failed to load beanClass {} while searching for CircuitBreaker annotations", e);
					}
				}
			}

		}
	}

	private void findAnnotationConfig(Class<?> beanClass) {
		for (Method m : beanClass.getMethods()) {
			CircuitBreaker circuitBreakerAnnotation = m.getAnnotation(CircuitBreaker.class);
			if (circuitBreakerAnnotation != null) {
				String name = circuitBreakerAnnotation.name();
				if (circuitBreakerAnnotation.ignoreExceptions().length > 0) {
					InstanceProperties properties = instanceProperties.computeIfAbsent(name,
							(key) -> new InstanceProperties());
					properties.setIgnoreExceptions(circuitBreakerAnnotation.ignoreExceptions());
				}
				if (circuitBreakerAnnotation.recordExceptions().length > 0) {
					InstanceProperties properties = instanceProperties.computeIfAbsent(name,
							(key) -> new InstanceProperties());
					properties.setIgnoreExceptions(circuitBreakerAnnotation.recordExceptions());
				}
			}
		}
	}

	/**
	 * Merges any configuration found on annotations with the provided properties,
	 * which typically would come from a property file.
	 * <p>
	 * The provided properties will always have precedence over any configuration
	 * found on annotations.
	 * 
	 * @param circuitBreakerProperties
	 */
	public void mergeConfigurationProperties(CircuitBreakerConfigurationProperties circuitBreakerProperties) {
		for (Map.Entry<String, InstanceProperties> annotationInstanceConfig : instanceProperties.entrySet()) {
			InstanceProperties annotationInstanceProperties = annotationInstanceConfig.getValue();
			InstanceProperties instanceProperties = circuitBreakerProperties.getInstances()
					.get(annotationInstanceConfig.getKey());
			if (instanceProperties == null) {
				// No properties config, just add the annotation config:
				circuitBreakerProperties.getInstances().put(annotationInstanceConfig.getKey(),
						annotationInstanceProperties);
				continue;
			}
			// Configuration file always takes precedence:
			if (instanceProperties.getIgnoreExceptions() == null
					&& annotationInstanceProperties.getIgnoreExceptions() != null) {
				instanceProperties.setIgnoreExceptions(annotationInstanceProperties.getIgnoreExceptions());
			}
			if (instanceProperties.getRecordExceptions() == null
					&& annotationInstanceProperties.getRecordExceptions() != null) {
				instanceProperties.setRecordExceptions(annotationInstanceProperties.getRecordExceptions());
			}
		}
	}

}
