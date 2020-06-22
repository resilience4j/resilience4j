package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.annotation.Bulkhead
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.maxThreadPoolSize", value = "10")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.coreThreadPoolSize", value = "5")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.queueCapacity", value = "4")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.keepAliveDuration", value = "PT10S")
@Property(name = "resilience4j.thread-pool-bulkhead.instances.backendA.baseConfig", value = "default")
class ThreadPoolBulkheadSpec extends Specification {
    @Inject
    ApplicationContext applicationContext;

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(ThreadPoolBulkheadRegistry)
        def defaultBulkhead = registry.bulkhead("default")
        def backendABulkhead = registry.bulkhead("backend-a")

        expect:
        defaultBulkhead != null
        defaultBulkhead.bulkheadConfig.getMaxThreadPoolSize() == 10
        defaultBulkhead.bulkheadConfig.getCoreThreadPoolSize() == 5
        defaultBulkhead.bulkheadConfig.getQueueCapacity() == 4
        defaultBulkhead.bulkheadConfig.keepAliveDuration.seconds == 10

        backendABulkhead != null
        backendABulkhead.bulkheadConfig.getMaxThreadPoolSize() == 10
        backendABulkhead.bulkheadConfig.getCoreThreadPoolSize() == 5
        backendABulkhead.bulkheadConfig.getQueueCapacity() == 4
        backendABulkhead.bulkheadConfig.keepAliveDuration.seconds == 10
    }

}
