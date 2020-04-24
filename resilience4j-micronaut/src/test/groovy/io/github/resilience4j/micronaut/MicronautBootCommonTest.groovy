package io.github.resilience4j.micronaut

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import spock.lang.Specification

class MicronautBootCommonTest extends Specification {
    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(RateLimiterRegistry)
        !applicationContext.containsBean(TimeLimiterRegistry)
        !applicationContext.containsBean(CircuitBreakerRegistry)
        !applicationContext.containsBean(BulkheadRegistry)


        cleanup:
        applicationContext.close()
    }

}
