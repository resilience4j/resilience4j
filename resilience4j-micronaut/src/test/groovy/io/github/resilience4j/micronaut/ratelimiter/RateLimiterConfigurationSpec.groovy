package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import spock.lang.Specification

class RateLimiterConfigurationSpec extends Specification {
    void "test rate limiter configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
            "test",
            ["resilience4j.ratelimiter.configs.default.limitForPeriod": 100,
             "resilience4j.ratelimiter.configs.default.limitRefreshPeriod": 100,
             "resilience4j.ratelimiter.configs.someShared.limitForPeriod": 10,
             "resilience4j.ratelimiter.configs.someShared.limitRefreshPeriod": 150,
             "resilience4j.ratelimiter.configs.someShared.eventConsumerBufferSize": 100,
             "resilience4j.ratelimiter.instances.backendA.limitForPeriod": 190,
             "resilience4j.ratelimiter.instances.backendA.baseConfig": "default",
             "resilience4j.ratelimiter.instances.backendB.baseConfig": "someShared",
             "resilience4j.ratelimiter.instances.backendB.timeoutDuration": 120,
             "resilience4j.ratelimiter.enabled" : true]
        ))
        applicationContext.start()
        def registry = applicationContext.getBean(RateLimiterRegistry)

        expect:
        registry.rateLimiter("default").rateLimiterConfig.limitForPeriod == 100

        cleanup:
        applicationContext.stop()
    }
    
}
