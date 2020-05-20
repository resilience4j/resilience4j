package io.github.resilience4j.micronaut.ratelimiter


import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
@Property(name = "resilience4j.ratelimiter.configs.default.slidingWindowType", value = "COUNT_BASED")
@Property(name = "resilience4j.ratelimiter.configs.default.limitForPeriod", value = "10")
@Property(name = "resilience4j.ratelimiter.configs.default.limitRefreshPeriod", value = "PT1s")
@Property(name = "resilience4j.ratelimiter.configs.default.timeoutDuration", value = "PT3s")
@Property(name = "resilience4j.ratelimiter.configs.default.slidingWindowSize", value = "100")
@Property(name = "resilience4j.ratelimiter.configs.default.permittedNumberOfCallsInHalfOpenState", value = "10")
@Property(name = "resilience4j.ratelimiter.configs.default.failureRateThreshold", value = "60")
@Property(name = "resilience4j.ratelimiter.configs.default.eventConsumerBufferSize", value = "10")
@Property(name = "resilience4j.ratelimiter.configs.default.registerHealthIndicator", value = "true")
@Property(name = "resilience4j.ratelimiter.instances.backendA.baseConfig", value = "default")
class RateLimitSpec extends Specification {
    @Inject ApplicationContext applicationContext


    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)
        def defaultRateLimiter = registry.rateLimiter("default")

        expect:
        defaultRateLimiter != null

        defaultRateLimiter.rateLimiterConfig.limitForPeriod == 10
        defaultRateLimiter.rateLimiterConfig.limitRefreshPeriod.seconds == 1
        defaultRateLimiter.rateLimiterConfig.timeoutDuration.seconds == 3
        defaultRateLimiter.getName() == "default"
    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)
        def backendARateLimiter = registry.rateLimiter("backend-a")

        expect:
        backendARateLimiter != null

        backendARateLimiter.rateLimiterConfig.limitForPeriod == 10
        backendARateLimiter.rateLimiterConfig.limitRefreshPeriod.seconds == 1
        backendARateLimiter.rateLimiterConfig.timeoutDuration.seconds == 3
        backendARateLimiter.getName() == "backend-a"

    }

}
