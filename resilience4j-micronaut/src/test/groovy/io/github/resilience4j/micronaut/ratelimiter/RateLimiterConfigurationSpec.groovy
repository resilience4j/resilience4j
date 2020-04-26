package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties
import io.github.resilience4j.ratelimiter.RateLimiterProperties
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class RateLimiterConfigurationSpec extends Specification {
    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(
        "resilience4j.ratelimiter.configs.default.limitForPeriod": 100,
        "resilience4j.ratelimiter.configs.default.limitRefreshPeriod": 100,

        "resilience4j.ratelimiter.configs.someShared.limitForPeriod": 300,
        "resilience4j.ratelimiter.configs.someShared.limitRefreshPeriod": 150,
        "resilience4j.ratelimiter.configs.someShared.eventConsumerBufferSize" : 10,

        "resilience4j.ratelimiter.instances.backendA.limitForPeriod": 190,

        "resilience4j.ratelimiter.instances.backendB.baseConfig": "some-shared",
        "resilience4j.ratelimiter.instances.backendB.timeoutDuration": 120,

        "resilience4j.ratelimiter.enabled": true)

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)
        def config = applicationContext.getBean(RateLimiterConfigurationProperties)

        expect:
        def defaultConfig = config.configs['default']
        defaultConfig.limitRefreshPeriod.seconds == 100
        defaultConfig.limitForPeriod == 100

        def defaultRate = registry.rateLimiter("default")
        defaultRate.rateLimiterConfig.limitForPeriod == 100
        defaultRate.rateLimiterConfig.limitRefreshPeriod.seconds == 100
    }

    void "backed-a configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)
        def config = applicationContext.getBean(RateLimiterConfigurationProperties)

        expect:
        def backendAConfig = config.instances["backend-a"]
        backendAConfig.limitForPeriod == 190

        def backendA = registry.rateLimiter("backend-a")
        backendA.rateLimiterConfig.limitForPeriod == 190
    }

    void "backed-b configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)
        def config = applicationContext.getBean(RateLimiterConfigurationProperties)
        expect:
        def backendBConfig = config.instances["backend-b"]
        backendBConfig.timeoutDuration.seconds == 120
        backendBConfig.baseConfig == 'some-shared'

        def backendB = registry.rateLimiter("backend-b")
        backendB.rateLimiterConfig.timeoutDuration.seconds == 120
        backendB.rateLimiterConfig.limitForPeriod == 300
        backendB.rateLimiterConfig.limitRefreshPeriod.seconds == 150
    }

    void "some-shared configuration"() {
        given:
        def registry = applicationContext.getBean(RateLimiterRegistry)
        def config = applicationContext.getBean(RateLimiterConfigurationProperties)

        expect:
        def someSharedConfig = config.configs["some-shared"]
        someSharedConfig.limitRefreshPeriod.seconds == 150
        someSharedConfig.limitForPeriod == 300
        someSharedConfig.eventConsumerBufferSize == 10

        def someShared = registry.rateLimiter("some-shared")
        someShared.rateLimiterConfig.limitForPeriod == 100
        someShared.rateLimiterConfig.limitRefreshPeriod.seconds == 100
    }
}
