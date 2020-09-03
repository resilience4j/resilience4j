package io.github.resilience4j.micronaut.bulkhead


import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.bulkhead.configs.default.maxConcurrentCalls", value = "2")
@Property(name = "resilience4j.bulkhead.configs.default.maxWaitDuration", value = "PT10S")
@Property(name = "resilience4j.bulkhead.instances.backendA.baseConfig", value = "default")
class BulkheadSpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(BulkheadRegistry)
        def defaultBulkhead = registry.bulkhead("default")

        expect:
        defaultBulkhead  != null
        defaultBulkhead.bulkheadConfig.maxWaitDuration.seconds == 10
        defaultBulkhead.bulkheadConfig.maxConcurrentCalls == 2
    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(BulkheadRegistry)
        def backendABulkhead = registry.bulkhead("backend-a")


        backendABulkhead != null
        backendABulkhead.bulkheadConfig.maxWaitDuration.seconds == 10
        backendABulkhead.bulkheadConfig.maxConcurrentCalls == 2
    }
}
