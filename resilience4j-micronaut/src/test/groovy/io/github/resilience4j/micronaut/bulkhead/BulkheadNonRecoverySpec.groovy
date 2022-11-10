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

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.micronaut.annotation.Bulkhead
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
class BulkheadNonRecoverySpec extends Specification{

    @Inject
    ThreadpoolBulkheadService service;


    void "test sync non recovery circuitbreaker"() {
        when:
        service.sync();

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"

    }

    void "test flowable non recovery circuitbreaker"() {
        setup:
        Flowable<String> result = service.flowable()

        when:
        result.blockingFirst()

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"
    }

    void "test completable non recovery circuitbreaker"() {
        setup:
        CompletableFuture<String> result = service.completable()

        when:
        result.get()

        then:
        ExecutionException ex = thrown()
        RuntimeException inner = ex.getCause()
        inner.getMessage() == "Test"

    }

    @Singleton
    static class ThreadpoolBulkheadService extends TestDummyService {

        @Bulkhead(name = "backend-a")
        @Override
        String sync() {
            return syncError()
        }

        @Bulkhead(name = "backend-a")
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @Bulkhead(name = "backend-a")
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @Bulkhead(name = "backend-a")
        @Override
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @Bulkhead(name = "backend-a")
        @Override
        Flowable<String> flowable() {
            return flowableError()
        }

        @Bulkhead(name = "backend-a")
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @Bulkhead(name = "backend-a")
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @Override
        @Bulkhead(name = "backend-a")
        Single<String> doSomethingSingleNull() {
            return null
        }

        @Bulkhead(name = "backend-a")
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @Bulkhead(name = "backend-a")
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }

        @Bulkhead(name = "default", type = Bulkhead.Type.THREADPOOL)
        CompletableFuture<String> asyncRecoverablePool() {
            return completableFutureError();
        }

        @Bulkhead(name = "default", type = Bulkhead.Type.THREADPOOL)
        String syncRecoverablePool() {
            return syncError();
        }
    }
}
