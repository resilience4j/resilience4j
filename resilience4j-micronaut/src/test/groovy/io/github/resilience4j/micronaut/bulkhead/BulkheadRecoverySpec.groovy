/*
 * Copyright 2020 Michael Pollind
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.mirconaut.annotation.Bulkhead
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton
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
