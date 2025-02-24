package io.github.resilience4j.micronaut.timelimiter

import io.github.resilience4j.micronaut.annotation.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.micronaut.context.annotation.Executable
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@MicronautTest
@Property(name = "resilience4j.timelimiter.enabled", value = "true")
class TimeLimiterInitializationViaInterceptorSpec extends Specification {

    @Inject
    TimeLimiterService service

    void "test timeLimiter succeeds"() {
        when:
        CompletionStage<String> result = service.completable()

        then:
        result.get() == "ok"
    }

    @Singleton
    static class TimeLimiterService {

        @TimeLimiter(name = "BACKEND", fallbackMethod = "completionStageRecovery")
        CompletionStage<String> completable() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(2000)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt()
                }
                return "ok"
            })
        }

        @Executable
        CompletionStage<String> completionStageRecovery() {
            return CompletableFuture.supplyAsync(() -> "recovered");
        }
    }

    @Primary
    @Singleton
    TimeLimiterRegistry timeLimiterRegistry() {

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))
            .build()

        return TimeLimiterRegistry.of(
            Map.of("BACKEND", timeLimiterConfig)
        )
    }
}
