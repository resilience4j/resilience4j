package io.github.resilience4j;

import io.github.resilience4j.retry.annotation.Retry;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class RetryDummyService implements TestDummyService {
    @Override
    @Retry(name = BACKEND, recovery = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @Retry(name = BACKEND, recovery = "recovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @Retry(name = BACKEND, recovery = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @Retry(name = BACKEND, recovery = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @Retry(name = BACKEND, recovery = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @Retry(name = BACKEND, recovery = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @Retry(name = BACKEND, recovery = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @Retry(name = BACKEND, recovery = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @Retry(name = BACKEND, recovery = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }
}
