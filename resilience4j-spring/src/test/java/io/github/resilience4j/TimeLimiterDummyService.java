package io.github.resilience4j;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class TimeLimiterDummyService implements TestDummyService {

    @Override
    public String sync() {
        //no-op
        return null;
    }

    @Override
    public String syncSuccess() {
        //no-op
        return null;
    }

    @TimeLimiter(name = BACKEND, fallbackMethod = "completionStageRecovery")
    public CompletionStage<String> success() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        });
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
    @TimeLimiter(name = BACKEND, fallbackMethod = "rx3ObservableRecovery")
    public io.reactivex.rxjava3.core.Observable<String> rx3Observable() {
        return rx3ObservableError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "rx3SingleRecovery")
    public io.reactivex.rxjava3.core.Single<String> rx3Single() {
        return rx3SingleError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "rx3CompletableRecovery")
    public io.reactivex.rxjava3.core.Completable rx3Completable() {
        return rx3CompletableError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "rx3MaybeRecovery")
    public io.reactivex.rxjava3.core.Maybe<String> rx3Maybe() {
        return rx3MaybeError();
    }

    @Override
    @TimeLimiter(name = BACKEND, fallbackMethod = "rx3FlowableRecovery")
    public io.reactivex.rxjava3.core.Flowable<String> rx3Flowable() {
        return rx3FlowableError();
    }

    @Override
    @TimeLimiter(name = "#root.args[0]", fallbackMethod = "${missing.property:monoRecovery}")
    public Mono<String> spelMono(String backend) {
        return monoError(backend);
    }
}
