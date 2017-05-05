package io.github.resilience4j.service.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;

/**
 * @author bstorozhuk
 */
@SpringBootApplication(scanBasePackages = "io.github.resilience4j")
@EnableSpringBootMetricsCollector
@EnablePrometheusEndpoint
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    public HealthIndicator backendA(CircuitBreakerRegistry circuitBreakerRegistry,
                                    EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
                                    CircuitBreakerProperties circuitBreakerProperties) {
        return new CircuitBreakerHealthIndicator(circuitBreakerRegistry,
            eventConsumerRegistry,
            circuitBreakerProperties,
            "backendA");
    }

    @Bean
    public HealthIndicator backendB(CircuitBreakerRegistry circuitBreakerRegistry,
                                    EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
                                    CircuitBreakerProperties circuitBreakerProperties) {
        return new CircuitBreakerHealthIndicator(circuitBreakerRegistry,
            eventConsumerRegistry,
            circuitBreakerProperties,
            "backendB");
    }
}
