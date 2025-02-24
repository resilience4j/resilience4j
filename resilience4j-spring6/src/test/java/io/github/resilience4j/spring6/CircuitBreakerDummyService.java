package io.github.resilience4j.spring6;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class CircuitBreakerDummyService implements TestDummyService {

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "recovery")
    public String syncSuccess() {
        return "ok";
    }

    @Override
    public CompletionStage<String> asyncThreadPool() {
        // no-op
        return null;
    }

    @Override
    public CompletionStage<String> asyncThreadPoolSuccess() {
        // no-op
        return null;
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "completionStageRecovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "rx3ObservableRecovery")
    public io.reactivex.rxjava3.core.Observable<String> rx3Observable() {
        return rx3ObservableError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "rx3SingleRecovery")
    public io.reactivex.rxjava3.core.Single<String> rx3Single() {
        return rx3SingleError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "rx3CompletableRecovery")
    public io.reactivex.rxjava3.core.Completable rx3Completable() {
        return rx3CompletableError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "rx3MaybeRecovery")
    public io.reactivex.rxjava3.core.Maybe<String> rx3Maybe() {
        return rx3MaybeError();
    }

    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "rx3FlowableRecovery")
    public io.reactivex.rxjava3.core.Flowable<String> rx3Flowable() {
        return rx3FlowableError();
    }

    @Override
    @CircuitBreaker(name = "#root.args[0]", fallbackMethod = "#{'recovery'}")
    public String spelSync(String backend) {
        return syncError();
    }
}
