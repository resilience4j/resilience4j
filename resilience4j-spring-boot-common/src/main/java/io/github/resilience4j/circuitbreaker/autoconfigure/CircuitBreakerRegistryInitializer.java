package io.github.resilience4j.circuitbreaker.autoconfigure;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;

/**
 * Initializes the registry with configuration that may come both from annotations and properties.
 */
public class CircuitBreakerRegistryInitializer implements InitializingBean {

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreakerConfigurationProperties circuitBreakerProperties;
    private CircuitBreakerConfiguration circuitBreakerConfiguration;
    private CircuitBreakerAnnotationConfigScanner circuitBreakerAnnotationConfigScanner;

    public CircuitBreakerRegistryInitializer(CircuitBreakerRegistry circuitBreakerRegistry,
	    CircuitBreakerConfiguration circuitBreakerConfiguration,
	    CircuitBreakerConfigurationProperties circuitBreakerProperties,
	    CircuitBreakerAnnotationConfigScanner circuitBreakerAnnotationConfigScanner) {
	this.circuitBreakerRegistry = circuitBreakerRegistry;
	this.circuitBreakerProperties = circuitBreakerProperties;
	this.circuitBreakerConfiguration = circuitBreakerConfiguration;
	this.circuitBreakerAnnotationConfigScanner = circuitBreakerAnnotationConfigScanner;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
	// Merge the properties with any found by the
	// circuitBreakerAnnotationConfigScanner
	if (circuitBreakerAnnotationConfigScanner != null) {
	    mergeAnnotationConfig(circuitBreakerAnnotationConfigScanner);
	}
	// Initialize backends that were initially configured.
	circuitBreakerConfiguration.initCircuitBreakerRegistry(circuitBreakerRegistry);
    }

    private void mergeAnnotationConfig(CircuitBreakerAnnotationConfigScanner circuitBreakerAnnotationConfigScanner) {
	for (Map.Entry<String, InstanceProperties> annotationInstanceConfig : circuitBreakerAnnotationConfigScanner
		.getInstanceProperties().entrySet()) {
	    // TODO: Here we take advantage of the fact that circuitBreakerProperties is
	    // mutable, is that ok?
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
