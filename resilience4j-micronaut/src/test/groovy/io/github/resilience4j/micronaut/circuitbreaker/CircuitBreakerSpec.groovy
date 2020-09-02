package io.github.resilience4j.micronaut.circuitbreaker


import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
@Property(name = "resilience4j.circuitbreaker.configs.default.slidingWindowType", value = "COUNT_BASED")
@Property(name = "resilience4j.circuitbreaker.configs.default.slidingWindowSize", value = "100")
@Property(name = "resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState", value = "10")
@Property(name = "resilience4j.circuitbreaker.configs.default.failureRateThreshold", value = "60")
@Property(name = "resilience4j.circuitbreaker.configs.default.eventConsumerBufferSize", value = "10")
@Property(name = "resilience4j.circuitbreaker.configs.default.registerHealthIndicator", value = "true")
@Property(name = "resilience4j.circuitbreaker.instances.backendA.baseConfig", value = "default")
class CircuitBreakerSpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    @Client("/circuitbreaker")
    HttpClient client;

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(CircuitBreakerRegistry)
        def circuitBreaker = registry.circuitBreaker("default")

        expect:
        circuitBreaker != null

        circuitBreaker.circuitBreakerConfig.slidingWindowSize == 100
        circuitBreaker.circuitBreakerConfig.slidingWindowType == CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
        circuitBreaker.circuitBreakerConfig.permittedNumberOfCallsInHalfOpenState == 10
        circuitBreaker.circuitBreakerConfig.failureRateThreshold == 60

    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(CircuitBreakerRegistry)
        def backendA = registry.circuitBreaker("backend-a")

        expect:
        backendA != null

        backendA.circuitBreakerConfig.slidingWindowSize == 100
        backendA.circuitBreakerConfig.slidingWindowType == CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
        backendA.circuitBreakerConfig.permittedNumberOfCallsInHalfOpenState == 10
        backendA.circuitBreakerConfig.failureRateThreshold == 60

    }

}
