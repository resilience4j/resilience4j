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
import java.util.concurrent.ExecutionException

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
class CircuitBreakerNonRecoverySpec extends Specification{
    @Inject
    CircuitBreakerNonRecoveryService service;

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
    static class CircuitBreakerNonRecoveryService extends TestDummyService {
        @CircuitBreaker(name = "default")
        @Override
        String sync() {
            return syncError()
        }

        @CircuitBreaker(name = "default")
        @Override
        String syncWithParam(String param) {
            return syncError()
        }

        @CircuitBreaker(name = "default")
        @Override
        CompletableFuture<String> completable() {
            return completableFutureError()
        }

        @CircuitBreaker(name = "default")
        @Override
        CompletableFuture<String> completableWithParam(String param) {
            return completableFutureError()
        }

        @CircuitBreaker(name = "default")
        @Override
        Flowable<String> flowable() {
            return flowableError()
        }

        @CircuitBreaker(name = "default")
        @Override
        Maybe<String> doSomethingMaybe() {
            return doSomethingMaybeError()
        }

        @CircuitBreaker(name = "default")
        @Override
        Single<String> doSomethingSingle() {
            return doSomethingSingleError()
        }

        @CircuitBreaker(name = "default")
        @Override
        Single<String> doSomethingSingleNull() {
            return null
        }

        @CircuitBreaker(name = "default")
        @Override
        Completable doSomethingCompletable() {
            return doSomethingCompletableError()
        }

        @CircuitBreaker(name = "default")
        @Override
        Observable<String> doSomethingObservable() {
            return doSomethingObservableError()
        }

    }
}
