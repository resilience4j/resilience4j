package io.github.resilience4j.circuitbreaker.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAnnotationConfigScanner;
import io.github.resilience4j.utils.AspectJOnClasspathCondition;

@Configuration
public class CircuitBreakerAnnotationConfigScannerAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	@Conditional(value = {AspectJOnClasspathCondition.class})
	public CircuitBreakerAnnotationConfigScanner circuitBreakerAnnotationConfigScanner() {
	    return new CircuitBreakerAnnotationConfigScanner();
	}
}
