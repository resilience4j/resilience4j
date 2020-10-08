package io.github.resilience4j.micronaut.timelimiter

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.micronaut.annotation.TimeLimiter
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

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
