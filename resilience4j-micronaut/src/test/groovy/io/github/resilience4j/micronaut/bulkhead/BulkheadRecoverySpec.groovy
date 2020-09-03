package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.annotation.Bulkhead
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Singleton
import javax.inject.Inject
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
class BulkheadRecoverySpec extends Specification{

    @Inject
    ThreadpoolBulkheadService service;


    void "test async recovery bulkhead"() {
        when:
        CompletableFuture<String> result = service.asynRecoverableSemaphore()

        then:
        result.get() == "recovered"
    }

    void "test async recovery bulkhead parameter"() {
        when:
        CompletableFuture<String> result = service.asynRecoverableSemaphoreProperty("test")

        then:
        result.get() == "test"
    }


    void "test sync recovery bulkhead"() {
        when:
        String result = service.syncRecoverableSemaphore()

        then:
        result == "recovered"
    }


    void "test async recovery threadPoolBulkhead"() {
        when:
        CompletableFuture<String> result = service.asyncRecoverablePool()
        then:
        result.get() == "recovered"
    }

    void "test sync recovery threadPoolBulkhead"() {
        when:
        String result = service.syncRecoverablePool()

        then:
        result == "recovered"
    }


    @Singleton
    static class ThreadpoolBulkheadService extends TestDummyService {
        @Bulkhead(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        @Get("semaphore/async-recoverable")
        CompletableFuture<String> asynRecoverableSemaphore() {
            return asyncError();
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'completionStageRecoveryParam')
        CompletableFuture<String> asynRecoverableSemaphoreProperty(String parameter) {
            return asyncError();
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'syncRecovery')
        String syncRecoverableSemaphore() {
            return syncError();
        }

        @Bulkhead(name = "default", fallbackMethod = 'completionStageRecovery', type = Bulkhead.Type.THREADPOOL)
        CompletableFuture<String> asyncRecoverablePool() {
            return asyncError();
        }

        @Bulkhead(name = "default", fallbackMethod = 'syncRecovery', type = Bulkhead.Type.THREADPOOL)
        String syncRecoverablePool() {
            return syncError();
        }
    }
}
