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
package io.github.resilience4j.micronaut.timelimiter

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.micronaut.annotation.TimeLimiter
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.timelimiter.enabled", value = "true")
class TimeLimiterRecoverySpec extends Specification {

    @Inject
    TimeLimiterService service;


    void "test sync recovery timeLimiter"() {
        when:
        String result = service.sync();

        then:
        result == "recovered"
    }

    void "test maybe recovery timeLimiter"() {
        when:
        Maybe<String> result = service.doSomethingMaybe();

        then:
        result.blockingGet() == "testMaybe"
    }

    void "test single recovery timeLimiter"() {
        when:
        Single<String> result = service.doSomethingSingle();

        then:
        result.blockingGet() == "testSingle"
    }

    void "test flowable recovery timeLimiter"() {
        when:
        Flowable<String> result = service.flowable();

        then:
        result.blockingFirst() == "recovered"
    }

    void "test single recovery timeLimiter null"() {
        setup:
        Single<String> result = service.doSomethingSingleNull();

        when:
        result.blockingGet();

        then:
        thrown NoSuchElementException
    }

    void "test completable recovery timeLimiter"() {
        when:
        CompletableFuture<String> result = service.completable();

        then:
        result.get() == "recovered"
    }

    @Singleton
    static class TimeLimiterService extends TestDummyService {
        @TimeLimiter(name = "backend-a", fallbackMethod = 'syncRecovery')
        @Override
        String sync() {
            return syncError()
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'syncRecoveryParam')
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'completableRecovery')
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'completableRecoveryParam')
        @Override
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'flowableRecovery')
        @Override
        Flowable<String> flowable() {
            return flowableError()
        }


        @TimeLimiter(name = "backend-a", fallbackMethod = 'doSomethingMaybeRecovery')
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'doSomethingCompletableRecovery')
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @TimeLimiter(name = "backend-a", fallbackMethod = 'doSomethingObservableRecovery')
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }
    }
}
