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
import java.util.concurrent.ExecutionException

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
class RetryNonRecoverySpec extends Specification {

    @Inject
    RetryNonRecoveryService service;

    void "test sync non recovery retry"() {
        when:
        service.sync();

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"

    }

    void "test flowable non recovery retry"() {
        setup:
        Flowable<String> result = service.flowable()

        when:
        result.blockingFirst()

        then:
        RuntimeException ex = thrown()
        ex.getMessage() == "Test"
    }

    void "test completable non recovery retry"() {
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
    static class RetryNonRecoveryService extends TestDummyService {

        @Override
        @Retry(name = "backend-a")
        String sync() {
            return syncError()
        }

        @Override
        @Retry(name = "backend-a")
        String syncWithParam(String param) {
            return syncError()
        }

        @Override
        @Retry(name = "backend-a")
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @Override
        @Retry(name = "backend-a")
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @Override
        @Retry(name = "backend-a")
        Flowable<String> flowable() {
            return flowableError()
        }

        @Retry(name = "backend-a")
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @Retry(name = "backend-a")
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @Retry(name = "backend-a")
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @Retry(name = "backend-a")
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @Retry(name = "backend-a")
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }
    }
}
