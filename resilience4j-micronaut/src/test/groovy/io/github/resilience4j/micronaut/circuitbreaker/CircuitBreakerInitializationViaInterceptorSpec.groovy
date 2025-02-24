package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micronaut.annotation.CircuitBreaker
import io.micronaut.context.annotation.Executable
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
class CircuitBreakerInitializationViaInterceptorSpec extends Specification {

    @Inject
    CircuitBreakerService service

    void "test sync recovery circuitbreaker"() {
        when:
        String result = service.sync()

        then:
        result == "recovered"
    }

    @Singleton
    static class CircuitBreakerService {

        @CircuitBreaker(name = "BACKEND", fallbackMethod = "syncRecovery")
        String sync() {
            return "ok"
        }

        @Executable
        String syncRecovery() {
            return "recovered"
        }
    }

    @Primary
    @Singleton
    CircuitBreakerRegistry circuitBreakerRegistry() {

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .initialState(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN)
            .build()

        return CircuitBreakerRegistry.custom()
            .withCircuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .addCircuitBreakerConfig("BACKEND", circuitBreakerConfig)
            .build()
    }
}
