package io.github.resilience4j.micronaut.bulkhead

import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class BulkheadConfiguration extends Specification {
    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(
        "resilience4j.bulkhead.configs.default.limitForPeriod": 100,
        "resilience4j.bulkhead.configs.default.limitRefreshPeriod": 100,

        "resilience4j.bulkhead.configs.someShared.limitForPeriod": 300,
        "resilience4j.bulkhead.configs.someShared.limitRefreshPeriod": 150,
        "resilience4j.bulkhead.configs.someShared.eventConsumerBufferSize" : 10,

        "resilience4j.bulkhead.instances.backendA.limitForPeriod": 190,

        "resilience4j.bulkhead.instances.backendB.baseConfig": "some-shared",
        "resilience4j.bulkhead.instances.backendB.timeoutDuration": 120,

        "resilience4j.bulkhead.enabled" : true,
    )
}
