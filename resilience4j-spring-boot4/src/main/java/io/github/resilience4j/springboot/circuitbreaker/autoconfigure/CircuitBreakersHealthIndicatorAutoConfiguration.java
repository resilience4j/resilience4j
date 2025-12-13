package io.github.resilience4j.springboot.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.springboot.circuitbreaker.monitoring.health.CircuitBreakersHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = HealthContributorAutoConfiguration.class, after = CircuitBreakerAutoConfiguration.class)
@ConditionalOnClass({CircuitBreaker.class, HealthIndicator.class, HealthContributorAutoConfiguration.class, StatusAggregator.class})
public class CircuitBreakersHealthIndicatorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "circuitBreakersHealthIndicator")
    @ConditionalOnProperty(prefix = "management.health.circuitbreakers", name = "enabled")
    public CircuitBreakersHealthIndicator circuitBreakersHealthIndicator(
        CircuitBreakerRegistry circuitBreakerRegistry,
        CircuitBreakerConfigurationProperties circuitBreakerProperties,
        StatusAggregator statusAggregator) {
        return new CircuitBreakersHealthIndicator(circuitBreakerRegistry, circuitBreakerProperties, statusAggregator);
    }

}
