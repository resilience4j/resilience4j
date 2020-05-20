package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.annotation.Bulkhead
import io.github.resilience4j.micronaut.TestDummyService
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
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
class BulkheadRecoverySpec extends Specification{

    @Inject
    @Client("/thread-pool-bulkhead")
    HttpClient client;


    void "test async recovery bulkhead"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("semaphore/async-recoverable", String.class);

        then:
        response.body() == "recovered"
    }

    void "test async recovery bulkhead parameter"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("semaphore/async-recoverable/test", String.class);

        then:
        response.body() == "test"
    }


    void "test sync recovery bulkhead"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("semaphore/sync-recoverable", String.class);

        then:
        response.body() == "recovered"
    }


    void "test async recovery threadPoolBulkhead"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("threadpool/async-recoverable", String.class);

        then:
        response.body() == "recovered"
    }

    void "test sync recovery threadPoolBulkhead"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("threadpool/sync-recoverable", String.class);

        then:
        response.body() == "recovered"
    }


    @Controller("/thread-pool-bulkhead")
    static class ThreadpoolBulkheadService extends TestDummyService {
        @Bulkhead(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        @Get("semaphore/async-recoverable")
        public CompletableFuture<String> asynRecovertableSemaphore() {
            return asyncError();
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'completionStageRecoveryParam')
        @Get("semaphore/async-recoverable/{parameter}")
        public CompletableFuture<String> asynRecovertableSemaphoreProperty(String parameter) {
            return asyncError();
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'syncRecovery')
        @Get("semaphore/sync-recoverable")
        public String syncRecovertableSemaphore() {
            return syncError();
        }

        @Bulkhead(name = "default", fallbackMethod = 'completionStageRecovery', type = Bulkhead.Type.THREADPOOL)
        @Get("threadpool/async-recoverable")
        public CompletableFuture<String> asynRecovertablePool() {
            return asyncError();
        }

        @Bulkhead(name = "default", fallbackMethod = 'syncRecovery', type = Bulkhead.Type.THREADPOOL)
        @Get("threadpool/sync-recoverable")
        public String syncRecovertablePool() {
            return syncError();
        }
    }
}
