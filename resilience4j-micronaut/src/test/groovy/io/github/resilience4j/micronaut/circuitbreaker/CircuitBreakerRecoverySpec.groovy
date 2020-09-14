package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.annotation.CircuitBreaker
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
class CircuitBreakerRecoverySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    CircuitBreakerService service;

    void "test async recovery circuit breaker"() {
        when:
        CompletableFuture<String> response = service.asynRecoverable();

        then:
        response.get() == "recovered"
    }


    void "test sync recovery circuit breaker"() {
        when:
        String response = service.syncRecoverable()

        then:
        response == "recovered"
    }

    @Singleton
    static class CircuitBreakerService extends TestDummyService {
        @CircuitBreaker(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        CompletableFuture<String> asynRecoverable() {
            return asyncError();
        }

        @CircuitBreaker(name = "backend-a", fallbackMethod = 'syncRecovery')
        String syncRecoverable() {
            return syncError();
        }
    }
}
