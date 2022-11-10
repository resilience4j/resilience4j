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
package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.micronaut.annotation.RateLimiter
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
class RateLimiterRecoverySpec extends Specification {
    @Inject
    RatelimiterService service;

    void "test sync recovery ratelimiter"() {
        when:
        String result = service.sync();

        then:
        result == "recovered"
    }

    void "test flowable recovery ratelimiter"() {
        when:
        Flowable<String> result = service.flowable();

        then:
        result.blockingFirst() == "recovered"
    }

    void "test maybe recovery ratelimiter"() {
        when:
        Maybe<String> result = service.doSomethingMaybe();

        then:
        result.blockingGet() == "testMaybe"
    }

    void "test single recovery ratelimiter null"() {
        setup:
        Single<String> result = service.doSomethingSingleNull();

        when:
        result.blockingGet();

        then:
        thrown NoSuchElementException
    }

    void "test single recovery ratelimiter"() {
        when:
        Single<String> result = service.doSomethingSingle();

        then:
        result.blockingGet() == "testSingle"
    }

    void "test completable recovery ratelimiter"() {
        when:
        CompletableFuture<String> result = service.completable();

        then:
        result.get() == "recovered"
    }

    @Singleton
    static class RatelimiterService extends TestDummyService {

        @RateLimiter(name = "default", fallbackMethod = 'syncRecovery')
        @Override
        String sync() {
            return syncError()
        }

        @RateLimiter(name = "default", fallbackMethod = 'syncRecoveryParam')
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @RateLimiter(name = "default", fallbackMethod = 'completableRecovery')
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @Override
        @RateLimiter(name = "default", fallbackMethod = 'completableRecoveryParam')
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @Override
        @RateLimiter(name = "default", fallbackMethod = 'flowableRecovery')
        Flowable<String> flowable() {
            return flowableError()
        }

        @RateLimiter(name = "default", fallbackMethod = 'doSomethingMaybeRecovery')
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @RateLimiter(name = "default", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @RateLimiter(name = "default", fallbackMethod = 'doSomethingSingleRecovery')
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @RateLimiter(name = "default", fallbackMethod = 'doSomethingCompletableRecovery')
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @RateLimiter(name = "default", fallbackMethod = 'doSomethingObservableRecovery')
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }
    }
}
