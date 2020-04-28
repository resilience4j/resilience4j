package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
class BulkheadSpec extends Specification {
    @Inject ApplicationContext applicationContext


    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(BulkheadRegistry)
        def bulkhead = registry.bulkhead("default")

        expect:
        bulkhead != null

        bulkhead.bulkheadConfig.maxWaitDuration.seconds == 10
        bulkhead.bulkheadConfig.maxConcurrentCalls == 2
    }
}
