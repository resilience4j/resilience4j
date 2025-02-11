package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.micronaut.annotation.Bulkhead
import io.micronaut.context.annotation.Executable
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
class BulkheadInitializationViaInterceptorSpec extends Specification {

    @Inject
    BulkheadService service

    void "test sync recovery bulkhead"() {
        when:
        String result = service.sync()

        then:
        result == "recovered"
    }

    @Singleton
    static class BulkheadService {

        @Bulkhead(name = "BACKEND", fallbackMethod = "syncRecovery")
        String sync() {
            return "ok"
        }

        @Executable
        String syncRecovery() {
            return "recovered"
        }
    }

    @Primary
    @Singleton
    BulkheadRegistry bulkheadRegistry() {

        BulkheadConfig backendBulkHeadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(0)
            .build()

        return BulkheadRegistry.custom()
            .withBulkheadConfig(BulkheadConfig.ofDefaults())
            .addBulkheadConfig("BACKEND", backendBulkHeadConfig)
            .build()
    }
}
