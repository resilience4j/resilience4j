package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import spock.lang.Specification

class  RateLimiterConfiguration extends Specification {
    void "test basic configuration functionality"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
            "test",
            ["resilience4j.ratelimiter.configs.default.limitForPeriod": 100,
             "resilience4j.ratelimiter.enabled" : true]
        ))
        applicationContext.start()

        expect:
        def registry = applicationContext.getBean(RateLimiterRegistry)
        registry.rateLimiter("default").rateLimiterConfig.limitForPeriod == 100

        cleanup:
        applicationContext.stop()
    }
}
