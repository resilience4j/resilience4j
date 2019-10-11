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
 * Unless resilience4j-spring-boot/resilience4j-spring-boot2/
 * resilience4j-spring-cloud/resilience4j-spring-cloud2 is used this class
 * must be explicitly added to the spring context as a bean for CircuitBreaker
 * annotation exception config to be considered.
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
		CircuitBreaker circuitBreakerAnnotation = beanClass.getAnnotation(CircuitBreaker.class);
		updateInstanceProperties(circuitBreakerAnnotation, beanClass);
	    for (Method m : beanClass.getMethods()) {
			circuitBreakerAnnotation = m.getAnnotation(CircuitBreaker.class);
			updateInstanceProperties(circuitBreakerAnnotation, beanClass);
		}
	}
	
	private void updateInstanceProperties(CircuitBreaker circuitBreakerAnnotation, Class<?> beanClass) {
	    if (circuitBreakerAnnotation != null) {
            String name = circuitBreakerAnnotation.name();
            if (circuitBreakerAnnotation.ignoreExceptions().length > 0) {
                InstanceProperties properties = instanceProperties.computeIfAbsent(name,
                        (key) -> new InstanceProperties());
                checkConflictingConfigurations(name, properties.getIgnoreExceptions(), beanClass, "ignoreExceptions");
                properties.setIgnoreExceptions(circuitBreakerAnnotation.ignoreExceptions());
            }
            if (circuitBreakerAnnotation.recordExceptions().length > 0) {
                InstanceProperties properties = instanceProperties.computeIfAbsent(name,
                        (key) -> new InstanceProperties());
                checkConflictingConfigurations(name, properties.getRecordExceptions(), beanClass, "recordExceptions");
                properties.setRecordExceptions(circuitBreakerAnnotation.recordExceptions());
            }
        }
	}
	
	private void checkConflictingConfigurations(String breakerName, Class<? extends Throwable>[] existingExceptions, Class<?> beanClass, String configName) {
	    if (existingExceptions != null && existingExceptions.length > 0) {
	        String msg = String.format("Can't set %s for @CircuitBreaker %s in class %s, it has already been set elsewhere", configName, breakerName, beanClass.getName());
            throw new IllegalArgumentException(msg);
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
				// No properties file config, just add the annotation config:
				circuitBreakerProperties.getInstances().put(annotationInstanceConfig.getKey(),
						annotationInstanceProperties);
			}
			else {
    			// Configuration file always takes precedence, check for config file values
			    // before setting the annotation value:
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

}
