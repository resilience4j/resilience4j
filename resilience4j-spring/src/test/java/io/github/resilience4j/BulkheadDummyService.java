package io.github.resilience4j;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

@Component
public class BulkheadDummyService implements TestDummyService {

    @Override
    @Bulkhead(name = BACKEND, fallbackMethod = "recovery")
    public String sync() {
        return syncError();
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

    @Bulkhead(name = BACKEND, fallbackMethod = "futureFallbackRecovered" )
    public Future<String> asyncFutureSemaphoreSuccess(){
        return CompletableFuture.completedFuture("Hello World");
    }

    @Bulkhead(name = BACKEND, fallbackMethod = "futureFallbackRecovered" )
    public Future<String> asyncFutureSemaphoreFailure(){
        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(new FileNotFoundException("something went wrong!"));
        return f;
    }

    @Bulkhead(name = BACKEND, fallbackMethod = "futureFallbackFailure" )
    public Future<String> asyncFutureSemaphoreFallbackFailure(){
        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(new FileNotFoundException("something went wrong!"));
        return f;
    }

    @Bulkhead(name = BACKEND, type = Bulkhead.Type.THREADPOOL, fallbackMethod = "futureFallbackRecovered" )
    public Future<String> asyncFutureThreadpoolSuccess(){
        return CompletableFuture.completedFuture("Hello World");
    }

    @Bulkhead(name = BACKEND, type = Bulkhead.Type.THREADPOOL, fallbackMethod = "futureFallbackRecovered" )
    public Future<String> asyncFutureThreadpoolFailure(){
        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(new FileNotFoundException("something went wrong!"));
        return f;
    }

    @Bulkhead(name = BACKEND, type = Bulkhead.Type.THREADPOOL, fallbackMethod = "futureFallbackFailure" )
    public Future<String> asyncFutureThreadpoolFallbackFailure(){
        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(new FileNotFoundException("something went wrong!"));
        return f;
    }
}
