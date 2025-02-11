package io.github.resilience4j.micronaut.retry


import io.github.resilience4j.micronaut.annotation.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.micronaut.context.annotation.Executable
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
class RetryInitializationViaInterceptorSpec extends Specification {

    @Inject
    RetryService service

    void "test succeeds retry"() {
        when:
        String result = service.sync()

        then:
        result == "ok"
    }

    @Singleton
    static class RetryService {

        AtomicInteger attemptCounter = new AtomicInteger(0)

        @Retry(name = "BACKEND", fallbackMethod = "syncRecovery")
        String sync() {
            int attempt = attemptCounter.getAndIncrement()
            if (attempt < 3) {
                throw new RuntimeException("Test exception")
            }
            return "ok"
        }

        @Executable
        String syncRecovery() {
            return "recovered"
        }
    }

    @Primary
    @Singleton
    RetryRegistry retryRegistry() {

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(4)     // more than the default
            .failAfterMaxAttempts(true)
            .build()

        return RetryRegistry.custom()
            .withRetryConfig(RetryConfig.ofDefaults())
            .addRetryConfig("BACKEND", retryConfig)
            .build()
    }
}
