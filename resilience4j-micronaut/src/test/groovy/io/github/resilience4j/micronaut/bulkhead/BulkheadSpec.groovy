package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.annotation.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadRegistry
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
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.bulkhead.configs.default.maxConcurrentCalls", value = "2")
@Property(name = "resilience4j.bulkhead.configs.default.maxWaitDuration", value = "PT10S")
@Property(name = "resilience4j.bulkhead.instances.backendA.baseConfig", value = "default")
class BulkheadSpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    @Client("/bulkhead")
    HttpClient client;

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(BulkheadRegistry)
        def defaultBulkhead = registry.bulkhead("default")
        def backendABulkhead = registry.bulkhead("backend-a")

        expect:
        defaultBulkhead  != null
        defaultBulkhead.bulkheadConfig.maxWaitDuration.seconds == 10
        defaultBulkhead.bulkheadConfig.maxConcurrentCalls == 2

        backendABulkhead != null
        backendABulkhead.bulkheadConfig.maxWaitDuration.seconds == 10
        backendABulkhead.bulkheadConfig.maxConcurrentCalls == 2
    }

    void "test recovery bulkhead"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/async-recoverable", String.class);

        then:
        response.body() == "recovered"

        when:
        response = client.toBlocking().exchange("/sync-recoverable", String.class);

        then:
        response.body() == "recovered"
    }

    @Controller("/bulkhead")
    static class BulkheadService extends TestDummyService {
        @Bulkhead(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        @Get("/async-recoverable")
        public CompletableFuture<String> asynRecovertable() {
            return asyncError();
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'syncRecovery')
        @Get("/sync-recoverable")
        public String syncRecovertable() {
            return syncError();
        }
    }
}
