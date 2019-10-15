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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;

/**
 * Scans all bean definitions for CircuitBreaker annotations and collects annotated configuration so
 * it can be added as a PropertySource to the environment. This is done as a
 * BeanFactoryPostProcessor so we can ensure that we have found these before actual bean
 * instantiation begins.
 * <p>
 * Unless resilience4j-spring-boot/resilience4j-spring-boot2/
 * resilience4j-spring-cloud/resilience4j-spring-cloud2 is used this class must be explicitly added
 * to the spring context as a bean for CircuitBreaker annotation exception config to be considered.
 */
public class CircuitBreakerAnnotationConfigScanner implements BeanFactoryPostProcessor, Ordered {
	private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerAnnotationConfigScanner.class);

    private ConfigurableEnvironment configurableEnvironment;

    private int order = LOWEST_PRECEDENCE;
    
	public CircuitBreakerAnnotationConfigScanner(ConfigurableEnvironment configurableEnvironment) {
        this.configurableEnvironment = configurableEnvironment;
	}
	
	public void setOrder(int order) {
        this.order = order;
    }
	
    @Override
    public int getOrder() {
        return order;
    }
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	    final Map<String, InstanceProperties> instanceProperties = new HashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (beanDefinition != null) {
				String beanClassName = beanDefinition.getBeanClassName();
				if (beanClassName != null) {
					try {
						Class<?> beanClass = Class.forName(beanClassName);
						findAnnotationConfig(instanceProperties, beanClass);
					} catch (ClassNotFoundException e) {
						LOG.error("Failed to load beanClass {} while searching for CircuitBreaker annotations", e);
					}
				}
			}
		}
		final Map<String, Object> annotationProperties = new LinkedHashMap<>();
		//resilience4j.circuitbreaker.configs.foo.recordExceptions[0]=org.springframework.web.client.HttpServerErrorException
		for(Map.Entry<String, InstanceProperties> entry : instanceProperties.entrySet()) {
		    addAnnotationExceptionPropertyToMapPropertySource(annotationProperties, entry.getKey(), "ignoreExceptions", entry.getValue().getIgnoreExceptions());
		    addAnnotationExceptionPropertyToMapPropertySource(annotationProperties, entry.getKey(), "recordExceptions", entry.getValue().getRecordExceptions());
		}
		MapPropertySource mapPropertySource = new MapPropertySource("circuitBreakerAnnotationConfig", annotationProperties);
		configurableEnvironment.getPropertySources().addLast(mapPropertySource);
	}

    private void addAnnotationExceptionPropertyToMapPropertySource(Map<String, Object> annotationProperties, String instanceName, String exceptionPropertyName,
            Class<? extends Throwable>[] exceptions) {
        if (exceptions != null) {
            for (int i = 0; i < exceptions.length; i++) {
                String key = String.format("resilience4j.circuitbreaker.instances.%s.%s[%d]", instanceName, exceptionPropertyName, i);
                String value = exceptions[i].getName();
                annotationProperties.put(key, value);
            }
        }
    }
	
	private void findAnnotationConfig(Map<String, InstanceProperties> instanceProperties, Class<?> beanClass) {
		CircuitBreaker circuitBreakerAnnotation = AnnotationUtils.findAnnotation(beanClass, CircuitBreaker.class);
		updateInstanceProperties(instanceProperties, circuitBreakerAnnotation, beanClass);
	    for (Method m : beanClass.getMethods()) {
			circuitBreakerAnnotation = AnnotationUtils.findAnnotation(m, CircuitBreaker.class);
			updateInstanceProperties(instanceProperties, circuitBreakerAnnotation, beanClass);
		}
	}
	
	private void updateInstanceProperties(Map<String, InstanceProperties> instanceProperties, CircuitBreaker circuitBreakerAnnotation, Class<?> beanClass) {
	    if (circuitBreakerAnnotation != null) {
            String name = circuitBreakerAnnotation.name();
            if (circuitBreakerAnnotation.ignoreExceptions().length > 0) {
                InstanceProperties properties = instanceProperties.computeIfAbsent(name,
                        (key) -> new InstanceProperties());
                checkConflictingConfigurations(name, properties.getIgnoreExceptions(), beanClass, "ignoreExceptions", circuitBreakerAnnotation.ignoreExceptions());
                properties.setIgnoreExceptions(circuitBreakerAnnotation.ignoreExceptions());
            }
            if (circuitBreakerAnnotation.recordExceptions().length > 0) {
                InstanceProperties properties = instanceProperties.computeIfAbsent(name,
                        (key) -> new InstanceProperties());
                checkConflictingConfigurations(name, properties.getRecordExceptions(), beanClass, "recordExceptions", circuitBreakerAnnotation.recordExceptions());
                properties.setRecordExceptions(circuitBreakerAnnotation.recordExceptions());
            }
        }
	}
	
    private void checkConflictingConfigurations(String breakerName, Class<? extends Throwable>[] existingExceptions, Class<?> beanClass, String configName,
            Class<? extends Throwable>[] annotationExceptions) {
        if (existingExceptions != null && existingExceptions.length > 0) {
            if (!Arrays.deepEquals(existingExceptions, annotationExceptions)) {
                String msg = String.format("Can't set %s for @CircuitBreaker %s in class %s, it has already been defined elsewhere with different exceptions",
                        configName, breakerName,
                        beanClass.getName());
                throw new IllegalArgumentException(msg);
            }
        }
    }

}
