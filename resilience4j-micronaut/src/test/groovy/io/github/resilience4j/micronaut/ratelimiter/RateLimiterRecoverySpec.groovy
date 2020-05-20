package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.annotation.RateLimiter
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
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
class RateLimiterRecoverySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    @Client("/ratelimiter")
    HttpClient client;

    void "test async recovery ratelimiter"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/async-recoverable", String.class);

        then:
        response.body() == "recovered"
    }

    void "test sync recovery ratelimiter"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/sync-recoverable", String.class);

        then:
        response.body() == "recovered"
    }

    @Controller("/ratelimiter")
    static class RatelimiterService extends TestDummyService {
        @RateLimiter(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        @Get("/async-recoverable")
        CompletableFuture<String> recoverable() {
            return asyncError();
        }

        @RateLimiter(name = "backend-a", fallbackMethod = 'syncRecovery')
        @Get("/sync-recoverable")
        String syncRecovertable() {
            return syncError();
        }
    }
}
