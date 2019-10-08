package io.github.resilience4j.circuitbreaker.autoconfigure;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;

public class CircuitBreakerAnnotationConfigScannerTest {

    @Test
    public void testGetInstanceProperties() {
        CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
        scanner.postProcessBeforeInitialization(new AnnotatedBean(), "foo");
        Map<String, InstanceProperties> instancePropertiesMap = scanner.getInstanceProperties();
        InstanceProperties instanceProperties = instancePropertiesMap.get("mybreaker");
        assertNotNull(instanceProperties);
        assertArrayEquals(new Class[] {IllegalArgumentException.class}, instanceProperties.getIgnoreExceptions());
    }

    public static class AnnotatedBean {

        @CircuitBreaker(name = "mybreaker", ignoreExceptions = {IllegalArgumentException.class})
        public String foo() {
            return null;
        }
    }
}
