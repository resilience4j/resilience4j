package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.annotation.Retry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties
import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.micronaut.circuitbreaker.RecordFailurePredicate
import io.github.resilience4j.retry.RetryRegistry
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
@Property(name = "resilience4j.retry.enabled", value = "true")
@Property(name = "resilience4j.retry.configs.default.maxRetryAttempts", value = "3")
@Property(name = "resilience4j.retry.configs.default.waitDuration", value = "PT1S")
@Property(name = "resilience4j.retry.configs.default.retryExceptions", value = "java.io.IOException")
@Property(name = "resilience4j.retry.instances.backendA.baseConfig", value = "default")
@Property(name = "resilience4j.retry.instances.backendA.maxRetryAttempts", value = "1")
class RetySpec extends Specification{
   @Inject ApplicationContext applicationContext

    @Inject
    @Client("/retry")
    HttpClient client;

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(RetryRegistry)
        def defaultRetry = registry.retry("default")
        def backendA = registry.retry("backend-a")

        expect:
        defaultRetry != null
        defaultRetry.retryConfig.maxAttempts == 3
        defaultRetry.retryConfig.intervalFunction.apply(0) == 1000

        backendA != null
        backendA.retryConfig.maxAttempts == 1
        backendA.retryConfig.intervalFunction.apply(0) == 1000
    }

    void "test recovery retry"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/async-recoverable", String.class);

        then:
        response.body() == "recovered"

        when:
        response = client.toBlocking().exchange("/sync-recoverable", String.class);

        then:
        response.body() == "recovered"
    }

    @Controller("/retry")
    static class RetryService extends TestDummyService{

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
