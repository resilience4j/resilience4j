package io.github.resilience4j;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class TimeLimiterDummyService implements TestDummyService {

    @Override
    public String sync() {
        //no-op
        return null;
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
    @TimeLimiter(name = BACKEND, fallbackMethod = "completionStageRecovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }

    @Override
    @TimeLimiter(name = "#root.args[0]", fallbackMethod = "${missing.property:monoRecovery}")
    public Mono<String> spelMono(String backend) {
        return monoError(backend);
    }
}
