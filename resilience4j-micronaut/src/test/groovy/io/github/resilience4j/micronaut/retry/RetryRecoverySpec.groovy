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
package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.micronaut.annotation.Retry
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
class RetryRecoverySpec extends Specification {

    @Inject
    RetryService service;


    void "test completable recovery retry"() {
        when:
        CompletableFuture<String> result = service.completable()

        then:
        result.get() == "recovered"
    }


    void "test sync recovery retry"() {
        when:
        String result = service.sync();

        then:
        result == "recovered"
    }

    void "test maybe recovery retry"() {
        when:
        Maybe<String> result = service.doSomethingMaybe();

        then:
        result.blockingGet() == "testMaybe"
    }

    void "test single recovery retry"() {
        when:
        Single<String> result = service.doSomethingSingle();

        then:
        result.blockingGet() == "testSingle"
    }

    void "test single recovery retry null"() {
        setup:
        Single<String> result = service.doSomethingSingleNull();

        when:
        result.blockingGet();

        then:
        thrown NoSuchElementException
    }

    void "test flowable recovery retry"() {
        when:
        Flowable<String> result = service.flowable()

        then:
        result.blockingFirst() == "recovered"
    }

    @Singleton
    static class RetryService extends TestDummyService {

        @Override
        @Retry(name = "backend-b", fallbackMethod = 'syncRecovery')
        String sync() {
            return syncError()
        }

        @Override
        @Retry(name = "backend-b", fallbackMethod = 'syncRecoveryParam')
        String syncWithParam(String param) {
            return syncError()
        }

        @Override
        @Retry(name = "backend-b", fallbackMethod = 'completableRecovery')
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @Override
        @Retry(name = "backend-b", fallbackMethod = 'completableRecoveryParam')
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @Override
        @Retry(name = "backend-b", fallbackMethod = 'flowableRecovery')
        Flowable<String> flowable() {
            return flowableError()
        }

        @Retry(name = "backend-b", fallbackMethod = 'doSomethingMaybeRecovery')
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @Retry(name = "backend-b", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @Retry(name = "backend-b", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @Retry(name = "backend-b", fallbackMethod = 'doSomethingCompletableRecovery')
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @Retry(name = "backend-b", fallbackMethod = 'doSomethingObservableRecovery')
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }
    }
}
