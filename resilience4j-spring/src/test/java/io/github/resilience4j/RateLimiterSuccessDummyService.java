package io.github.resilience4j;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

@Component
public class RateLimiterSuccessDummyService implements TestDummyService {

    @Override
    @RateLimiter(name = BACKEND, fallbackMethod = "recovery")
    public String sync() {
        return "success";
    }

    @Override
    public CompletionStage<String> asyncThreadPool() {
        return null;
    }

    @Override
    public CompletionStage<String> asyncThreadPoolSuccess() {
        return null;
    }

    @Override
    public CompletionStage<String> async() {
        return null;
    }

    @Override
    public Flux<String> flux() {
        return null;
    }

    @Override
    public Mono<String> mono(String parameter) {
        return null;
    }

    @Override
    public Observable<String> observable() {
        return null;
    }

    @Override
    public Single<String> single() {
        return null;
    }

    @Override
    public Completable completable() {
        return null;
    }

    @Override
    public Maybe<String> maybe() {
        return null;
    }

    @Override
    public Flowable<String> flowable() {
        return null;
    }

    @Override
    public io.reactivex.rxjava3.core.Observable<String> rx3Observable() {
        return null;
    }

    @Override
    public io.reactivex.rxjava3.core.Single<String> rx3Single() {
        return null;
    }

    @Override
    public io.reactivex.rxjava3.core.Completable rx3Completable() {
        return null;
    }

    @Override
    public io.reactivex.rxjava3.core.Maybe<String> rx3Maybe() {
        return null;
    }

    @Override
    public io.reactivex.rxjava3.core.Flowable<String> rx3Flowable() {
        return null;
    }

}
