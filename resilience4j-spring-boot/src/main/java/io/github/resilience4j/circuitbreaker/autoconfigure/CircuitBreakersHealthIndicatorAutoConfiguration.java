package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakersHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({CircuitBreaker.class, HealthIndicator.class})
@AutoConfigureAfter(CircuitBreakerAutoConfiguration.class)
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
public class CircuitBreakersHealthIndicatorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "circuitBreakersHealthIndicator")
    @ConditionalOnProperty(prefix = "management.health.circuitbreakers", name = "enabled")
    public CircuitBreakersHealthIndicator circuitBreakersHealthIndicator(
        CircuitBreakerRegistry circuitBreakerRegistry,
        CircuitBreakerConfigurationProperties circuitBreakerProperties,
        HealthAggregator healthAggregator) {
        return new CircuitBreakersHealthIndicator(circuitBreakerRegistry, circuitBreakerProperties,
            healthAggregator);
    }

}
