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
import java.util.concurrent.ExecutionException

@MicronautTest
@Property(name = "resilience4j.timelimiter.enabled", value = "true")
class TimeLimiterNonRecoverySpec  extends Specification {
    @Inject
    TimeLimiterNonRecoveryService service;


    void "test sync non recovery timelimiter"() {
        when:
        service.sync();

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"

    }

    void "test flowable non recovery timelimiter"() {
        setup:
        Flowable<String> result = service.flowable()

        when:
        result.blockingFirst()

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"
    }

    void "test completable non recovery timelimiter"() {
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
    static class TimeLimiterNonRecoveryService extends TestDummyService {
        @TimeLimiter(name = "backend-a")
        @Override
        String sync() {
            return syncError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        Flowable<String> flowable() {
            return flowableError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @TimeLimiter(name = "backend-a")
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @TimeLimiter(name = "backend-a")
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }
    }
}
