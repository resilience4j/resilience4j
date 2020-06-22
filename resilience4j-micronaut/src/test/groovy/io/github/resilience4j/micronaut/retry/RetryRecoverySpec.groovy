package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.annotation.Retry
import io.github.resilience4j.micronaut.TestDummyService
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
@Property(name = "resilience4j.retry.enabled", value = "true")
class RetryRecoverySpec extends Specification {
    @Inject
    @Client("/retry")
    HttpClient client;

    void "test async recovery retry"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/async-recoverable", String.class);

        then:
        response.body() == "recovered"

    }

    void "test sync recovery retry"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/sync-recoverable", String.class);

        then:
        response.body() == "recovered"
    }

    @Controller("/retry")
    static class RetryService extends TestDummyService {

        @Retry(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        @Get("/async-recoverable")
        CompletableFuture<String> recoverable() {
            return asyncError();
        }

        @Retry(name = "backend-a", fallbackMethod = 'syncRecovery')
        @Get("/sync-recoverable")
        String syncRecovertable() {
            return syncError();
        }
    }
}
