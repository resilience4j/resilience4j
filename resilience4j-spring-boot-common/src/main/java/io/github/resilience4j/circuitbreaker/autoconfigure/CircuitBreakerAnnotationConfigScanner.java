package io.github.resilience4j.circuitbreaker.autoconfigure;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;

/**
 * Scans all beans for CircuitBreaker annotations and collects annotated configuration so it can
 * be merged with the properties configuration.
 * 
 */
public class CircuitBreakerAnnotationConfigScanner implements BeanPostProcessor {
    private Map<String, InstanceProperties> instanceProperties = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = AopUtils.getTargetClass(bean);
        for (Method m : beanClass.getMethods()) {
            CircuitBreaker circuitBreakerAnnotation = m.getAnnotation(CircuitBreaker.class);
            if (circuitBreakerAnnotation != null) {
                String name = circuitBreakerAnnotation.name();
                if (circuitBreakerAnnotation.ignoreExceptions().length > 0) {
                    InstanceProperties properties = instanceProperties.computeIfAbsent(name, (key) -> new InstanceProperties());
                    properties.setIgnoreExceptions(circuitBreakerAnnotation.ignoreExceptions());
                }
                if (circuitBreakerAnnotation.recordExceptions().length > 0) {
                    InstanceProperties properties = instanceProperties.computeIfAbsent(name, (key) -> new InstanceProperties());
                    properties.setIgnoreExceptions(circuitBreakerAnnotation.recordExceptions());
                }
            }
        }
        return bean;
    }
    
    public Map<String, InstanceProperties> getInstanceProperties() {
        return instanceProperties;
    }
}
