package io.github.resilience4j.micronaut.circuitbreaker

import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class CircuitBreakerConfiguration extends Specification {
    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(
        "resilience4j.circuitbreaker.configs.default.limitForPeriod": 100,
        "resilience4j.circuitbreaker.configs.default.limitRefreshPeriod": 100,

        "resilience4j.circuitbreaker.configs.someShared.limitForPeriod": 300,
        "resilience4j.circuitbreaker.configs.someShared.limitRefreshPeriod": 150,
        "resilience4j.circuitbreaker.configs.someShared.eventConsumerBufferSize" : 10,

        "resilience4j.circuitbreaker.instances.backendA.limitForPeriod": 190,

        "resilience4j.circuitbreaker.instances.backendB.baseConfig": "some-shared",
        "resilience4j.circuitbreaker.instances.backendB.timeoutDuration": 120,

        "resilience4j.circuitbreaker.enabled" : true,
    )

}
