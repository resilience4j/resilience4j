package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.micronaut.circuitbreaker.CircuitBreakerSpec
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.annotation.Controller
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Inject
import java.time.Duration

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
class RateLimitSpec extends Specification {
    @Inject ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)

        expect:
        def ratelimiter = registry.rateLimiter("default")
        ratelimiter != null

        ratelimiter.rateLimiterConfig.limitForPeriod == 10
        ratelimiter.rateLimiterConfig.limitRefreshPeriod.seconds == 1
        ratelimiter.rateLimiterConfig.timeoutDuration.seconds == 0
        ratelimiter.getName() == "default"
    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)

        expect:
        def ratelimiter = registry.rateLimiter("backend-a")
        ratelimiter != null

        ratelimiter.rateLimiterConfig.limitForPeriod == 10
        ratelimiter.rateLimiterConfig.limitRefreshPeriod.seconds == 1
        ratelimiter.rateLimiterConfig.timeoutDuration.seconds == 0

        ratelimiter.getName() == "backend-a"
    }

    void "backend-b configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)

        expect:
        def ratelimiter = registry.rateLimiter("backend-b")
        ratelimiter != null

        ratelimiter.rateLimiterConfig.limitForPeriod == 100
        ratelimiter.rateLimiterConfig.limitRefreshPeriod == Duration.ofMillis(500)
        ratelimiter.rateLimiterConfig.timeoutDuration.seconds == 3

        ratelimiter.getName() == "backend-b"
    }
}
