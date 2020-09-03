package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.annotation.RateLimiter
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
class RateLimiterRecoverySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    RatelimiterService service;

    void "test async recovery ratelimiter"() {
        when:
        CompletableFuture<String> body = service.recoverable();

        then:
        body.get() == "recovered"
    }

    void "test sync recovery ratelimiter"() {
        when:
        String body = service.syncRecovertable();

        then:
        body == "recovered"
    }

    @Singleton
    static class RatelimiterService extends TestDummyService {
        @RateLimiter(name = "default", fallbackMethod = 'completionStageRecovery')
        CompletableFuture<String> recoverable() {
            return asyncError();
        }

        @RateLimiter(name = "default", fallbackMethod = 'syncRecovery')
        String syncRecovertable() {
            return syncError();
        }
    }
}
