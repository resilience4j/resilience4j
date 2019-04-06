package io.github.resilience4j;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class CircuitBreakerDummyService implements TestDummyService {
    @Override
    @CircuitBreaker(name = BACKEND, recovery = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "recovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, recovery = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }
}
