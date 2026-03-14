package io.github.resilience4j.spring6;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.reactivex.*;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class TimeLimiterDummyService implements TestDummyService {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @TimeLimiter(name = BACKEND, fallbackMethod = "completionStageRecovery")
    public @interface ComposedTimeLimiter {
        @AliasFor(annotation = TimeLimiter.class, attribute = "name")
        String name() default BACKEND;

        @AliasFor(annotation = TimeLimiter.class, attribute = "fallbackMethod")
        String fallbackMethod() default "completionStageRecovery";
    }

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

    @Override
    @TimeLimiter(name = "#root.args[0]", configuration = BACKEND, fallbackMethod = "${missing.property:monoRecovery}")
    public Mono<String> spelMonoWithCfg(String backend) {
        return monoError(backend);
    }

    @ComposedTimeLimiter
    public CompletionStage<String> composedAsync() {
        return asyncError();
    }

    @ComposedTimeLimiter(name = "#root.args[0]", fallbackMethod = "#{'completionStageRecovery'}")
    public CompletionStage<String> composedSpelAsync(String backend) {
        return asyncError();
    }
}
