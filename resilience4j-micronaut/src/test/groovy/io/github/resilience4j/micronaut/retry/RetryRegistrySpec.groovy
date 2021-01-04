package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.core.functions.Either
import io.github.resilience4j.retry.RetryRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
@Property(name = "resilience4j.retry.configs.default.maxAttempts", value = "3")
@Property(name = "resilience4j.retry.configs.default.waitDuration", value = "PT1S")
@Property(name = "resilience4j.retry.configs.default.retryExceptions", value = "java.io.IOException")
@Property(name = "resilience4j.retry.instances.backendA.baseConfig", value = "default")
@Property(name = "resilience4j.retry.instances.backendA.maxAttempts", value = "1")
class RetryRegistrySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(RetryRegistry)
        def defaultRetry = registry.retry("default")

        expect:
        defaultRetry != null
        defaultRetry.retryConfig.maxAttempts == 3
        defaultRetry.retryConfig.getIntervalBiFunction().apply(0, Either.right(0)) == 1000
        defaultRetry.retryConfig.getExceptionPredicate().test(new IOException())
        !defaultRetry.retryConfig.getExceptionPredicate().test(new IgnoredException())

    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(RetryRegistry)
        def backendA = registry.retry("backend-a")

        expect:
        backendA != null
        backendA.retryConfig.maxAttempts == 1
        backendA.retryConfig.getIntervalBiFunction().apply(0, Either.right(0)) == 1000
        backendA.retryConfig.getExceptionPredicate().test(new IOException())
        !backendA.retryConfig.getExceptionPredicate().test(new IgnoredException())
    }
}
