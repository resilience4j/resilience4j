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
import java.util.concurrent.ExecutionException

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
class RateLimiterNonRecoverySpec extends Specification{
    @Inject
    RateLimiterNonRecoveryService service;

    void "test sync non recovery ratelimiter"() {
        when:
        service.sync();

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"

    }

    void "test maybe non recovery ratelimiter"() {
        setup:
        Maybe<String> result = service.doSomethingMaybe();

        when:
        result.blockingGet()

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "testMaybe"

    }

    void "test single non recovery ratelimiter"() {
        setup:
        Single<String> result = service.doSomethingSingle();

        when:
        result.blockingGet()

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "testSingle"
    }

    void "test single non recovery ratelimiter null"() {
        setup:
        Single<String> result = service.doSomethingSingleNull();

        when:
        result.blockingGet();

        then:
        thrown NoSuchElementException
    }

    void "test flowable non recovery ratelimiter"() {
        setup:
        Flowable<String> result = service.flowable()

        when:
        result.blockingFirst()

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"
    }

    void "test completable non recovery ratelimiter"() {
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
    static class RateLimiterNonRecoveryService extends TestDummyService {
        @RateLimiter(name = "default")
        @Override
        String sync() {
            return syncError()
        }

        @RateLimiter(name = "default")
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @RateLimiter(name = "default")
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @Override
        @RateLimiter(name = "default")
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @Override
        @RateLimiter(name = "default")
        Flowable<String> flowable() {
            return flowableError()
        }

        @RateLimiter(name = "default")
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @RateLimiter(name = "default")
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @RateLimiter(name = "default")
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @RateLimiter(name = "default")
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @RateLimiter(name = "default")
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }
    }
}
