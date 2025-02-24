package io.github.resilience4j.spring6;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class RateLimiterDummyService implements TestDummyService {

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "recovery")
    public String syncSuccess() {
        return "ok";
    }

    @Override
    public CompletionStage<String> asyncThreadPool() {
        //no-op
        return null;
    }

    @Override
    public CompletionStage<String> asyncThreadPoolSuccess() {
        // no-op
        return null;
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "completionStageRecovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "rx3ObservableRecovery")
    public io.reactivex.rxjava3.core.Observable<String> rx3Observable() {
        return rx3ObservableError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "rx3SingleRecovery")
    public io.reactivex.rxjava3.core.Single<String> rx3Single() {
        return rx3SingleError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "rx3CompletableRecovery")
    public io.reactivex.rxjava3.core.Completable rx3Completable() {
        return rx3CompletableError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "rx3MaybeRecovery")
    public io.reactivex.rxjava3.core.Maybe<String> rx3Maybe() {
        return rx3MaybeError();
    }

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "rx3FlowableRecovery")
    public io.reactivex.rxjava3.core.Flowable<String> rx3Flowable() {
        return rx3FlowableError();
    }

    @Override
    @RateLimiter(name = "#root.args[0]", fallbackMethod = "#{'recovery'}")
    public String spelSync(String backend) {
        return syncError();
    }
}
