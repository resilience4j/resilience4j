package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
class CircuitBreakerSpec extends Specification {
    @Inject ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(CircuitBreakerRegistry)
        def circuitbreaker = registry.circuitBreaker("default")

        expect:
        circuitbreaker != null

        circuitbreaker.circuitBreakerConfig.slidingWindowSize == 100
        circuitbreaker.circuitBreakerConfig.slidingWindowType == CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
        circuitbreaker.circuitBreakerConfig.permittedNumberOfCallsInHalfOpenState == 10
        circuitbreaker.circuitBreakerConfig.failureRateThreshold == 60

    }

}
