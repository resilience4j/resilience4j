package io.github.resilience4j.spring6;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class BulkheadDummyService implements TestDummyService {

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "recovery")
    public String sync() {
        return syncError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "recovery")
    public String syncSuccess() {
        return "ok";
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "completionStageRecovery")
    public CompletionStage<String> async() {
        return asyncError();
    }

    @Override
    @Bulkhead(name = BACKEND_B, type = Bulkhead.Type.THREADPOOL, fallbackMethod = "completionStageRecovery")
    public CompletionStage<String> asyncThreadPool() {
        return asyncError();
    }

    @Override
    @Bulkhead(name = BACKEND_B, type = Bulkhead.Type.THREADPOOL)
    public CompletionStage<String> asyncThreadPoolSuccess() {
        return CompletableFuture.completedFuture("finished");
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "fluxRecovery")
    public Flux<String> flux() {
        return fluxError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "monoRecovery")
    public Mono<String> mono(String parameter) {
        return monoError(parameter);
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "observableRecovery")
    public Observable<String> observable() {
        return observableError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "singleRecovery")
    public Single<String> single() {
        return singleError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "completableRecovery")
    public Completable completable() {
        return completableError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "maybeRecovery")
    public Maybe<String> maybe() {
        return maybeError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "flowableRecovery")
    public Flowable<String> flowable() {
        return flowableError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "rx3ObservableRecovery")
    public io.reactivex.rxjava3.core.Observable<String> rx3Observable() {
        return rx3ObservableError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "rx3SingleRecovery")
    public io.reactivex.rxjava3.core.Single<String> rx3Single() {
        return rx3SingleError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "rx3CompletableRecovery")
    public io.reactivex.rxjava3.core.Completable rx3Completable() {
        return rx3CompletableError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "rx3MaybeRecovery")
    public io.reactivex.rxjava3.core.Maybe<String> rx3Maybe() {
        return rx3MaybeError();
    }

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "rx3FlowableRecovery")
    public io.reactivex.rxjava3.core.Flowable<String> rx3Flowable() {
        return rx3FlowableError();
    }

    @Override
    @Bulkhead(name = "#root.args[0]", fallbackMethod = "#{'recovery'}")
    public String spelSync(String backend) {
        return syncError();
    }
}
