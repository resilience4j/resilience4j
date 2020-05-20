package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.annotation.CircuitBreaker
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
class CircuitBreakerRecoverySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    @Client("/circuitbreaker")
    HttpClient client;

    void "test asyc recovery circuitbreaker"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/async-recoverable", String.class);

        then:
        response.getStatus().code == 200
        response.body() == "recovered"
    }


    void "test sync recovery circuitbreaker"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/sync-recoverable", String.class);

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
