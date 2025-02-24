package io.github.resilience4j;

import io.github.resilience4j.retry.annotation.Retry;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RetryDummyService implements TestDummyService {

    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    @Override
    @Retry(name = BACKEND, fallbackMethod = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "recovery")
    public String syncSuccess() {
        int attempt = attemptCounter.getAndIncrement();
        if (attempt < 3) {
            throw new RuntimeException("Test exception");
        }
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
    @Retry(name = BACKEND, fallbackMethod = "completionStageRecovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "rx3ObservableRecovery")
    public io.reactivex.rxjava3.core.Observable<String> rx3Observable() {
        return rx3ObservableError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "rx3SingleRecovery")
    public io.reactivex.rxjava3.core.Single<String> rx3Single() {
        return rx3SingleError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "rx3CompletableRecovery")
    public io.reactivex.rxjava3.core.Completable rx3Completable() {
        return rx3CompletableError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "rx3MaybeRecovery")
    public io.reactivex.rxjava3.core.Maybe<String> rx3Maybe() {
        return rx3MaybeError();
    }

    @Override
    @Retry(name = BACKEND, fallbackMethod = "rx3FlowableRecovery")
    public io.reactivex.rxjava3.core.Flowable<String> rx3Flowable() {
        return rx3FlowableError();
    }

    @Override
    @Retry(name = "#root.args[0]", fallbackMethod = "#{'recovery'}")
    public String spelSync(String backend) {
        return syncError();
    }
}
