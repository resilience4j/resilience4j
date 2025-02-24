package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.micronaut.annotation.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.time.Duration

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
class RateLimiterInitializationViaInterceptorSpec extends Specification {

    @Inject
    RateLimitedService service

    def "test service with rate limited service cap"() {
        when:
        String result = service.limited()

        then:
        noExceptionThrown()
        result == "ok"

        when:
        service.limited()

        then:
        def ex = thrown(RequestNotPermitted)
        ex.message == "RateLimiter 'low' does not permit further calls"
    }

    @Singleton
    static class RateLimitedService {

        @RateLimiter(name = "low")
        String limited() {
            return "ok";
        }
    }

    @Primary
    @Singleton
    RateLimiterRegistry rateLimiterRegistry() {

        RateLimiterConfig backendRateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofSeconds(10))
            .timeoutDuration(Duration.ofMillis(1))
            .build()

        return RateLimiterRegistry.custom()
            .withRateLimiterConfig(RateLimiterConfig.ofDefaults())
            .addRateLimiterConfig("low", backendRateLimiterConfig)
            .build()
    }
}
