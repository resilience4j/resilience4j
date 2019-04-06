package io.github.resilience4j;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class RateLimiterDummyService implements TestDummyService {
    @Override
    @RateLimiter(name = BACKEND, recovery = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "recovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @RateLimiter(name = BACKEND, recovery = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }
}
