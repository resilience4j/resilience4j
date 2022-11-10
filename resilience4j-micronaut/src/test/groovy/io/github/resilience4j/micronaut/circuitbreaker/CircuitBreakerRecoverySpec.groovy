/*
 * Copyright 2020 Michael Pollind , Mahmoud Romeh
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
package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.micronaut.annotation.CircuitBreaker
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
class CircuitBreakerRecoverySpec extends Specification {
    @Inject
    CircuitBreakerService service;

    void "test sync recovery circuitbreaker"() {
        when:
        String result = service.sync();

        then:
        result == "recovered"
    }

    void "test flowable recovery circuitbreaker"() {
        when:
        Flowable<String> result = service.flowable();

        then:
        result.blockingFirst() == "recovered"
    }

    void "test maybe recovery circuitbreaker"() {
        when:
        Maybe<String> result = service.doSomethingMaybe();

        then:
        result.blockingGet() == "testMaybe"
    }

    void "test single recovery circuitbreaker"() {
        when:
        Single<String> result = service.doSomethingSingle();

        then:
        result.blockingGet() == "testSingle"
    }


    void "test single recovery circuitbreaker null"() {
        setup:
        Single<String> result = service.doSomethingSingleNull();

        when:
        result.blockingGet();

        then:
        thrown NoSuchElementException
    }


    void "test completable recovery circuitbreaker"() {
        when:
        CompletableFuture<String> result = service.completable();

        then:
        result.get() == "recovered"
    }

    @Singleton
    static class CircuitBreakerService extends TestDummyService {
        @CircuitBreaker(name = "default", fallbackMethod = 'syncRecovery')
        @Override
        String sync() {
            return syncError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'syncRecoveryParam')
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'completableRecovery')
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'completableRecoveryParam')
        @Override
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'flowableRecovery')
        @Override
        Flowable<String> flowable() {
            return flowableError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'doSomethingMaybeRecovery')
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'doSomethingCompletableRecovery')
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @CircuitBreaker(name = "default", fallbackMethod = 'doSomethingObservableRecovery')
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }

    }
}
