package io.github.resilience4j;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class BulkheadDummyService implements TestDummyService {
    @Override
    @Bulkhead(name = BACKEND, recovery = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "recovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @Bulkhead(name = BACKEND, recovery = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }
}
