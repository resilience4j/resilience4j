package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.annotation.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject
import java.util.concurrent.CompletableFuture

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
        def circuitbreaker = registry.circuitBreaker("default")

        expect:
        circuitbreaker != null

        circuitbreaker.circuitBreakerConfig.slidingWindowSize == 100
        circuitbreaker.circuitBreakerConfig.slidingWindowType == CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
        circuitbreaker.circuitBreakerConfig.permittedNumberOfCallsInHalfOpenState == 10
        circuitbreaker.circuitBreakerConfig.failureRateThreshold == 60

    }

    void "test recovery circuitbreaker"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/async-recoverable", String.class);

        then:
        response.getStatus().code == 200
        response.body() == "recovered"

        when:
        response = client.toBlocking().exchange("/sync-recoverable", String.class);

        then:
        response.getStatus().code == 200
        response.body() == "recovered"
    }

    @Controller("/circuitbreaker")
    static class CircuitbreakerService extends TestDummyService {
        @CircuitBreaker(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        @Get("/async-recoverable")
        public CompletableFuture<String> asynRecovertable() {
            return asyncError();
        }

        @CircuitBreaker(name = "backend-a", fallbackMethod = 'syncRecovery')
        @Get("/sync-recoverable")
        public String syncRecovertable() {
            return syncError();
        }

    }
}
