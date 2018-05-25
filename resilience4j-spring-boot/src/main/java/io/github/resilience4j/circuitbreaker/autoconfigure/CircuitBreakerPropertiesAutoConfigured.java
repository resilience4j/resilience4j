package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "resilience4j.circuitbreaker")
class CircuitBreakerPropertiesAutoConfigured extends CircuitBreakerProperties {

}
