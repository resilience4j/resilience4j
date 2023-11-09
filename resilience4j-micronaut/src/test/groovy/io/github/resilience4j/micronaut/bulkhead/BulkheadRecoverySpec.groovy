/*
 * Copyright 2020 Michael Pollind, Mahmoud Romeh
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

import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
class BulkheadRecoverySpec extends Specification{

    @Inject
    ThreadpoolBulkheadService service;


    void "test sync recovery bulkhead"() {
        when:
        String result = service.sync();

        then:
        result == "recovered"
    }

    void "test flowable recovery bulkhead"() {
        when:
        Flowable<String> result = service.flowable();

        then:
        result.blockingFirst() == "recovered"
    }

    void "test completable recovery bulkhead"() {
        when:
        CompletableFuture<String> result = service.completable();

        then:
        result.get() == "recovered"
    }

    void "test maybe recovery bulkhead"() {
        when:
        Maybe<String> result = service.doSomethingMaybe();

        then:
        result.blockingGet() == "testMaybe"
    }

    void "test single recovery bulkhead"() {
        when:
        Single<String> result = service.doSomethingSingle();

        then:
        result.blockingGet() == "testSingle"
    }

    void "test single recovery bulkhead null"() {
        setup:
        Single<String> result = service.doSomethingSingleNull();

        when:
        result.blockingGet();

        then:
        thrown NoSuchElementException
    }

    void "test async recovery threadPoolBulkhead"() {
        when:
        CompletableFuture<String> result = service.asyncRecoverablePool()

        then:
        result.get() == "recovered"
    }

    void "test sync recovery threadPoolBulkhead"() {
        when:
        service.syncRecoverablePool()

        then:
        IllegalStateException ex = thrown()
        ex.getMessage() == "ThreadPool bulkhead is only applicable for completable futures"
    }

    @Singleton
    static class ThreadpoolBulkheadService extends TestDummyService {

        @Bulkhead(name = "backend-a", fallbackMethod = 'syncRecovery')
        @Override
        String sync() {
            return syncError()
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'syncRecoveryParam')
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'completableRecovery')
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'completableRecoveryParam')
        @Override
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'flowableRecovery')
        @Override
        Flowable<String> flowable() {
            return flowableError()
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'doSomethingMaybeRecovery')
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @Override
        @Bulkhead(name = "backend-a", fallbackMethod = 'doSomethingSingleRecovery')
        Single<String> doSomethingSingleNull() {
            return null
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'doSomethingCompletableRecovery')
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @Bulkhead(name = "backend-a", fallbackMethod = 'doSomethingObservableRecovery')
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }

        @Bulkhead(name = "default", fallbackMethod = 'completableRecovery', type = Bulkhead.Type.THREADPOOL)
        CompletableFuture<String> asyncRecoverablePool() {
            return completableFutureError();
        }

        @Bulkhead(name = "default", fallbackMethod = 'syncRecovery', type = Bulkhead.Type.THREADPOOL)
        String syncRecoverablePool() {
            return syncError();
        }
    }
}
