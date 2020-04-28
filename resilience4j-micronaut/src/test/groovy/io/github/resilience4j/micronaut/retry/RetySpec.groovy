package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties
import io.github.resilience4j.micronaut.circuitbreaker.RecordFailurePredicate
import io.github.resilience4j.retry.RetryRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.annotation.Controller
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
class RetySpec extends Specification{
   @Inject ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(RetryRegistry)
        def retry = registry.retry("default")

        expect:
        retry != null

        retry.retryConfig.maxAttempts == 3

    }

}
