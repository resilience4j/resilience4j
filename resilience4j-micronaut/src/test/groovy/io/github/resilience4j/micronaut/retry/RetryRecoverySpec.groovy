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
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
class RetryRecoverySpec extends Specification {

    @Inject
    RetryService service;

    void "test async recovery retry"() {
        when:
        CompletableFuture<String>  result = service.recoverable();

        then:
        result.get() == "recovered"

    }

    void "test sync recovery retry"() {
        when:
        String result = service.syncRecoverable()

        then:
        result == "recovered"
    }

    @Singleton
    static class RetryService extends TestDummyService {

        @Retry(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        CompletableFuture<String> recoverable() {
            return asyncError();
        }

        @Retry(name = "backend-a", fallbackMethod = 'syncRecovery')
        String syncRecoverable() {
            return syncError();
        }
    }
}
