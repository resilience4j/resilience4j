package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerHystrixServerSideEvent;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerServerSideEvent;
import io.github.resilience4j.reactor.adapter.ReactorAdapter;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnClass({CircuitBreaker.class, Endpoint.class})
@AutoConfigureAfter(CircuitBreakerAutoConfiguration.class)
@ConditionalOnProperty(value = "resilience4j.circuitBreaker.sse.enabled")
public class CircuitBreakerStreamEventsAutoConfiguration {

    @Bean
    @ConditionalOnAvailableEndpoint
    @ConditionalOnClass({Flux.class, ReactorAdapter.class})
    public CircuitBreakerServerSideEvent circuitBreakerServerSideEventEndpoint(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerServerSideEvent(circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnAvailableEndpoint
    @ConditionalOnClass({Flux.class, ReactorAdapter.class})
    public CircuitBreakerHystrixServerSideEvent circuitBreakerHystrixServerSideEventEndpoint(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerHystrixServerSideEvent(circuitBreakerRegistry);
    }
}
